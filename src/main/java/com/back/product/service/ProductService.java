package com.back.product.service;

import com.back.product.document.ProductDocument;
import com.back.product.entity.Product;
import com.back.product.entity.ProductKeyword;
import com.back.product.repository.ProductDocumentRepository;
import com.back.product.repository.ProductRepository;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductDocumentRepository productDocumentRepository;
    private final EmbeddingModel embeddingModel;
    private final ElasticsearchTemplate elasticsearchTemplate;

    // 1) PostgreSQL에 상품 저장 → 2) 키워드 임베딩 → 3) Elasticsearch에 벡터 저장
    // 두 저장소는 하나의 트랜잭션으로 묶이지 않는다. 3)이 실패하면 "DB에는 있지만 검색되지 않는 상품"이 생긴다.
    public Product create(String name, List<String> keywords) {
        Product product = new Product();
        product.setName(name);
        keywords.forEach(product::addKeyword);
        float[] embedding = embed(keywords);
        product.setEmbedding(embedding); // pgvector 컬럼에도 저장 (13강에서 Elasticsearch를 대체)
        productRepository.save(product);

        saveDocument(product.getId(), embedding);
        return product;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findAll() {
        return StreamSupport.stream(productRepository.findAll().spliterator(), false).toList();
    }

    public Optional<ProductDocument> findDocumentById(Long id) {
        return productDocumentRepository.findById(id);
    }

    @Transactional
    public Product update(Long id, String name, List<String> keywords) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setName(name);
        product.getKeywords().clear(); // orphanRemoval = true : 컬렉션에서 빠진 키워드는 DELETE 된다
        keywords.forEach(product::addKeyword);
        float[] embedding = embed(keywords);
        product.setEmbedding(embedding);

        saveDocument(id, embedding);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
        productDocumentRepository.deleteById(id);
    }

    // 검색어를 임베딩해서, 벡터가 가장 가까운 상품 k개를 유사도 순으로 돌려준다
    public List<Product> knnSearch(String query, int k) {
        float[] queryVector = embeddingModel.embed(query);
        return findProductsByVector(queryVector, k);
    }

    // 여러 키워드를 각각 임베딩한 뒤 평균 벡터로 검색한다 (상품 벡터를 만드는 방식과 같다)
    public List<Product> knnSearch(List<String> keywords, int k) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        float[] queryVector = calculateAverage(embeddingModel.embed(keywords));
        return findProductsByVector(queryVector, k);
    }

    // 기준 상품의 키워드로 검색하고, 1위로 나오는 기준 상품 자신은 뺀다
    // (Elasticsearch 9.x는 저장한 벡터를 _source로 돌려주지 않으므로 키워드로 다시 임베딩한다)
    @Transactional(readOnly = true)
    public List<Product> findSimilarProducts(Long productId, int k) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        List<String> keywords = product.getKeywords().stream()
                .map(ProductKeyword::getKeyword)
                .toList();

        return knnSearch(keywords, k + 1).stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(k)
                .toList();
    }

    private List<Product> findProductsByVector(float[] queryVector, int k) {
        List<Long> ids = knnSearchByVector(queryVector, k, k * 2).stream()
                .map(ProductDocument::getId)
                .toList();

        // findAllById는 순서를 보장하지 않는다. KNN이 돌려준 id 순서(유사도 순)대로 다시 정렬한다.
        Map<Long, Product> productMap = StreamSupport
                .stream(productRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ProductDocument> knnSearchByVector(float[] queryVector, int k, int numCandidates) {
        KnnSearch knnSearch = KnnSearch.of(knn -> knn
                .field("embedding")
                .queryVector(toFloatList(queryVector)) // Elasticsearch 클라이언트는 List<Float>를 받는다
                .k(k)                                  // 최종으로 돌려받을 개수
                .numCandidates(numCandidates)          // 샤드마다 살펴볼 후보 수 (클수록 정확, 느림)
        );

        NativeQuery query = NativeQuery.builder()
                .withKnnSearches(knnSearch)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchTemplate.search(query, ProductDocument.class);
        searchHits.getSearchHits().forEach(hit ->
                System.out.println("  KNN hit id=" + hit.getContent().getId() + " score=" + hit.getScore()));
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    // 키워드가 없으면 벡터도 없다 (검색 대상에서 빠짐)
    private float[] embed(List<String> keywords) {
        return keywords.isEmpty() ? null : calculateAverage(embeddingModel.embed(keywords));
    }

    private void saveDocument(Long productId, float[] embedding) {
        ProductDocument doc = new ProductDocument();
        doc.setId(productId);
        doc.setEmbedding(embedding);
        productDocumentRepository.save(doc);
    }

    // pgvector로 검색 : 코사인 거리가 가까운 순서로 k개 (13강에서 knnSearch를 이 방식으로 바꾼다)
    public List<Product> vectorSearch(String query, int k) {
        float[] queryVector = embeddingModel.embed(query);
        SearchResults<Product> results = productRepository.searchByEmbeddingNear(
                Vector.of(queryVector),
                // cosine()의 Score는 "코사인 거리(embedding <=> 쿼리)의 상한"이다. 거리는 0(같음) ~ 2(반대).
                // 2.0이면 거르지 않고 가까운 순서로 k개를 받는다. 0.5처럼 주면 그보다 먼 상품은 빠진다.
                Score.of(2.0, ScoringFunction.cosine()),
                Limit.of(k));
        results.forEach(r -> System.out.println("  pgvector hit " + r.getContent().getName() + " distance=" + r.getScore().getValue()));
        return results.stream().map(SearchResult::getContent).toList();
    }

    // 키워드마다 만든 벡터를 차원별로 평균 내어 상품 하나를 대표하는 벡터로 만든다
    private float[] calculateAverage(List<float[]> vectors) {
        int dimension = vectors.get(0).length;
        float[] sum = new float[dimension];

        for (float[] vector : vectors) {
            for (int i = 0; i < dimension; i++) {
                sum[i] += vector[i];
            }
        }

        for (int i = 0; i < dimension; i++) {
            sum[i] /= vectors.size();
        }

        return sum;
    }
}

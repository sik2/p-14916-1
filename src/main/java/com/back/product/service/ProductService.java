package com.back.product.service;

import com.back.product.entity.Product;
import com.back.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final EmbeddingModel embeddingModel;

    // 상품 정보와 벡터를 같은 행에 저장한다. 하나의 트랜잭션이라 "DB에는 있지만 검색 안 되는 상품"이 생기지 않는다.
    @Transactional
    public Product create(String name, List<String> keywords) {
        Product product = new Product();
        product.setName(name);
        keywords.forEach(product::addKeyword);
        product.setEmbedding(embed(keywords));
        return productRepository.save(product);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findAll() {
        return StreamSupport.stream(productRepository.findAll().spliterator(), false).toList();
    }

    @Transactional
    public Product update(Long id, String name, List<String> keywords) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.setName(name);
        product.getKeywords().clear(); // orphanRemoval = true : 컬렉션에서 빠진 키워드는 DELETE 된다
        keywords.forEach(product::addKeyword);
        product.setEmbedding(embed(keywords));
        return product;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id); // 벡터도 같은 행에 있으므로 함께 지워진다
    }

    // 검색어를 임베딩해서, 벡터가 가장 가까운 상품 k개를 가까운 순서로 돌려준다
    public List<Product> knnSearch(String query, int k) {
        return searchByVector(embeddingModel.embed(query), k);
    }

    // 여러 키워드를 각각 임베딩한 뒤 평균 벡터로 검색한다 (상품 벡터를 만드는 방식과 같다)
    public List<Product> knnSearch(List<String> keywords, int k) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        return searchByVector(calculateAverage(embeddingModel.embed(keywords)), k);
    }

    // 기준 상품에 저장된 벡터로 바로 검색한다 (다시 임베딩하지 않으므로 임베딩 API 호출이 없다)
    @Transactional(readOnly = true)
    public List<Product> findSimilarProducts(Long productId, int k) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        if (product.getEmbedding() == null) {
            return List.of();
        }

        // 1위로 나오는 기준 상품 자신을 빼기 위해 하나 더 가져온다
        return searchByVector(product.getEmbedding(), k + 1).stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(k)
                .toList();
    }

    private List<Product> searchByVector(float[] queryVector, int k) {
        SearchResults<Product> results = productRepository.searchByEmbeddingNear(
                Vector.of(queryVector),
                // cosine()의 Score는 "코사인 거리(embedding <=> 쿼리)의 상한"이다. 거리는 0(같음) ~ 2(반대).
                // 2.0이면 거르지 않고 가까운 순서로 k개를 받는다. 0.5처럼 주면 그보다 먼 상품은 빠진다.
                Score.of(2.0, ScoringFunction.cosine()),
                Limit.of(k));
        return results.stream().map(SearchResult::getContent).toList();
    }

    // 키워드가 없으면 벡터도 없다 (검색 대상에서 빠짐)
    private float[] embed(List<String> keywords) {
        return keywords.isEmpty() ? null : calculateAverage(embeddingModel.embed(keywords));
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

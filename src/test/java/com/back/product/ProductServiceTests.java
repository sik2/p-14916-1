package com.back.product;

import com.back.TestcontainersConfiguration;
import com.back.product.document.ProductDocument;
import com.back.product.entity.Product;
import com.back.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTests {
    @Autowired
    private ProductService productService;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    // Elasticsearch 9.x는 벡터를 _source에서 빼고 저장한다. 그래서 조회하면 embedding이 null로 보인다.
    // 벡터가 색인되었는지는 exists 쿼리로 확인한다.
    private boolean hasEmbedding(Long id) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.ids(i -> i.values(String.valueOf(id))))
                        .filter(f -> f.exists(e -> e.field("embedding")))))
                .build();
        return elasticsearchOperations.count(query, ProductDocument.class) == 1;
    }

    @Test
    @DisplayName("create - PostgreSQL에 상품, Elasticsearch에 벡터가 같은 id로 저장된다")
    void t1() {
        Product product = productService.create("노트북", List.of("전자기기", "컴퓨터", "휴대용"));

        assertNotNull(product.getId());
        assertEquals(3, product.getKeywords().size());
        assertTrue(productService.findDocumentById(product.getId()).isPresent());
        assertTrue(hasEmbedding(product.getId()));
    }

    @Test
    @DisplayName("Elasticsearch 9.x - 저장한 문서를 조회하면 embedding은 _source에 없다")
    void t2() {
        Product product = productService.create("마우스", List.of("전자기기", "입력장치"));

        ProductDocument doc = productService.findDocumentById(product.getId()).orElseThrow();
        assertNull(doc.getEmbedding());   // _source에서 제외되어 돌아오지 않는다
        assertTrue(hasEmbedding(product.getId())); // 하지만 색인(검색용)에는 들어 있다
    }

    @Test
    @DisplayName("update - 이름과 키워드가 바뀌고 벡터도 다시 저장된다")
    void t3() {
        Product product = productService.create("마우스", List.of("전자기기", "입력장치"));

        productService.update(product.getId(), "게이밍 마우스", List.of("게임", "RGB"));

        Product updated = productService.findById(product.getId()).orElseThrow();
        assertEquals("게이밍 마우스", updated.getName());
        assertTrue(hasEmbedding(product.getId()));
    }

    @Test
    @DisplayName("delete - 두 저장소에서 모두 삭제된다")
    void t4() {
        Product product = productService.create("키보드", List.of("전자기기", "입력장치"));

        productService.delete(product.getId());

        assertTrue(productService.findById(product.getId()).isEmpty());
        assertTrue(productService.findDocumentById(product.getId()).isEmpty());
    }

    @Test
    @DisplayName("키워드가 없으면 임베딩을 만들지 않는다")
    void t5() {
        Product product = productService.create("이름만 있는 상품", List.of());

        assertTrue(productService.findDocumentById(product.getId()).isPresent());
        assertFalse(hasEmbedding(product.getId()));
    }
}

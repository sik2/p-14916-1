package com.back.product;

import com.back.TestcontainersConfiguration;
import com.back.product.document.ProductDocument;
import com.back.product.repository.ProductDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductDocumentTests {
    @Autowired
    private ProductDocumentRepository productDocumentRepository;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    @DisplayName("products 인덱스의 embedding 필드는 384차원 dense_vector다")
    @SuppressWarnings("unchecked")
    void t1() {
        Map<String, Object> mapping = elasticsearchOperations.indexOps(ProductDocument.class).getMapping();
        Map<String, Object> embedding = (Map<String, Object>) ((Map<String, Object>) mapping.get("properties")).get("embedding");
        System.out.println("embedding 매핑 : " + embedding);

        assertEquals("dense_vector", embedding.get("type"));
        assertEquals(384, embedding.get("dims"));
        assertEquals("cosine", embedding.get("similarity"));
    }

    @Test
    @DisplayName("ProductDocument 저장 후 id로 조회")
    void t2() {
        ProductDocument doc = new ProductDocument();
        doc.setId(9999L);
        doc.setEmbedding(new float[384]);
        doc.getEmbedding()[0] = 1.0f; // cosine 유사도는 0 벡터를 저장할 수 없다

        productDocumentRepository.save(doc);

        assertTrue(productDocumentRepository.findById(9999L).isPresent());
        productDocumentRepository.deleteById(9999L);
    }
}

package com.back.product;

import com.back.TestcontainersConfiguration;
import com.back.product.entity.Product;
import com.back.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTests {
    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("create - 상품과 384차원 벡터가 같은 행에 저장된다")
    void t1() {
        Product product = productService.create("노트북", List.of("전자기기", "컴퓨터", "휴대용"));

        Product found = productService.findById(product.getId()).orElseThrow();
        assertEquals(3, product.getKeywords().size());
        assertEquals(384, found.getEmbedding().length); // Elasticsearch와 달리 저장한 벡터를 다시 읽을 수 있다
    }

    @Test
    @DisplayName("update - 키워드가 바뀌면 벡터도 다시 계산된다")
    void t2() {
        Product product = productService.create("마우스", List.of("전자기기", "입력장치"));
        float[] before = productService.findById(product.getId()).orElseThrow().getEmbedding();

        productService.update(product.getId(), "게이밍 마우스", List.of("게임", "RGB"));

        Product updated = productService.findById(product.getId()).orElseThrow();
        assertEquals("게이밍 마우스", updated.getName());
        assertNotEquals(before[0], updated.getEmbedding()[0]);
    }

    @Test
    @DisplayName("delete - 삭제하면 벡터도 함께 사라진다")
    void t3() {
        Product product = productService.create("키보드", List.of("전자기기", "입력장치"));

        productService.delete(product.getId());

        assertTrue(productService.findById(product.getId()).isEmpty());
    }

    @Test
    @DisplayName("키워드가 없으면 임베딩을 만들지 않는다")
    void t4() {
        Product product = productService.create("이름만 있는 상품", List.of());

        assertNull(productService.findById(product.getId()).orElseThrow().getEmbedding());
    }
}

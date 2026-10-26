package com.back;

import com.back.product.entity.Product;
import com.back.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test") // 테스트는 Ollama 사용, OpenAI 자동 설정 제외
class BackApplicationTests {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("상품을 저장하면 키워드도 함께 저장되고, 키워드에 상품 id가 들어간다")
    void t1() {
        Product product = new Product();
        product.setName("테스트 상품");
        product.addKeyword("키워드1");
        product.addKeyword("키워드2");

        productRepository.save(product); // CascadeType.ALL : 키워드는 따로 저장하지 않는다

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_keyword WHERE product_id = ?", Integer.class, product.getId());
        assertEquals(2, count);
    }
}

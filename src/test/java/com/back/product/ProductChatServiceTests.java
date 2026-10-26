package com.back.product;

import com.back.TestcontainersConfiguration;
import com.back.product.service.ProductChatService;
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
class ProductChatServiceTests {
    @Autowired
    private ProductChatService productChatService;
    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("chatStream - 답변이 여러 토큰으로 나뉘어 온다")
    void t1() {
        productService.create("Espresso Coffee", List.of("coffee", "espresso", "beverage"));

        List<String> tokens = productChatService.chatStream("Recommend a hot drink", "stream-user").collectList().block();

        assertNotNull(tokens);
        assertTrue(tokens.size() > 1);
        System.out.println("토큰 수 : " + tokens.size() + ", 답변 : " + String.join("", tokens));
    }
}

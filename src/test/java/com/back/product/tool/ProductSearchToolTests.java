package com.back.product.tool;

import com.back.TestcontainersConfiguration;
import com.back.product.entity.Product;
import com.back.product.repository.ProductRepository;
import com.back.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductSearchToolTests {
    @Autowired
    private ProductSearchTool productSearchTool;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;

    private Product gamingLaptop;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        gamingLaptop = productService.create("Gaming Laptop", List.of("gaming", "laptop", "high-performance"));
        productService.create("Business Laptop", List.of("business", "laptop", "office"));
        productService.create("Gaming Mouse", List.of("gaming", "mouse", "RGB"));
    }

    private Map<String, ToolCallback> tools() {
        // .tools(productSearchTool)이 내부에서 하는 일 : @Tool 메서드를 ToolCallback으로 바꾼다
        return Arrays.stream(ToolCallbacks.from(productSearchTool))
                .collect(Collectors.toMap(t -> t.getToolDefinition().name(), t -> t));
    }

    @Test
    @DisplayName("@Tool 메서드 4개가 도구로 등록된다")
    void t1() {
        assertEquals(java.util.Set.of("searchProducts", "findSimilarProducts", "getProductById", "getAllProducts"),
                tools().keySet());
        System.out.println("searchProducts 입력 스키마 : " + tools().get("searchProducts").getToolDefinition().inputSchema());
    }

    @Test
    @DisplayName("AI가 k를 빼고 호출해도 기본값 5로 동작하고, 결과는 상품 JSON이다")
    void t2() {
        String result = tools().get("searchProducts").call("{\"keywords\":\"gaming\"}"); // AI가 보낼 인자(JSON)
        System.out.println("searchProducts 결과 : " + result);

        assertTrue(result.contains("Gaming Laptop"));
        assertTrue(result.contains("\"keywords\":[")); // 지연 로딩된 키워드까지 들어 있다
    }

    @Test
    @DisplayName("findSimilarProducts 도구 - 기준 상품은 결과에 없다")
    void t3() {
        String result = tools().get("findSimilarProducts")
                .call("{\"productId\":" + gamingLaptop.getId() + ",\"k\":2}");

        assertFalse(result.contains("\"Gaming Laptop\""));
        assertTrue(result.contains("Gaming Mouse"));
    }
}

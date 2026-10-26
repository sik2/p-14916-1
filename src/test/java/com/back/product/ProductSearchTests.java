package com.back.product;

import com.back.TestcontainersConfiguration;
import com.back.product.entity.Product;
import com.back.product.repository.ProductDocumentRepository;
import com.back.product.repository.ProductRepository;
import com.back.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ProductSearchTests {
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductDocumentRepository productDocumentRepository;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    // 테스트 클래스들이 같은 컨테이너를 공유하므로, 검색 결과가 섞이지 않게 비우고 시작한다
    @BeforeEach
    void setUp() {
        productDocumentRepository.deleteAll();
        productRepository.deleteAll();

        productService.create("Gaming Laptop", List.of("gaming", "laptop", "graphics-card"));
        productService.create("Business Laptop", List.of("office", "document", "lightweight"));
        productService.create("Gaming Mouse", List.of("gaming", "mouse", "RGB"));
        productService.create("Espresso Coffee", List.of("coffee", "espresso", "beverage"));

        elasticsearchOperations.indexOps(com.back.product.document.ProductDocument.class).refresh(); // 방금 넣은 문서를 바로 검색 가능하게
    }

    @Test
    @DisplayName("KNN 검색 - 'gaming' 과 가장 가까운 상품은 게이밍 상품이다")
    void t1() {
        List<Product> results = productService.knnSearch("gaming", 2);

        System.out.println("gaming 검색 결과 : " + results.stream().map(Product::getName).toList());
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getName().startsWith("Gaming")));
    }

    @Test
    @DisplayName("KNN 검색 - 키워드에 없는 단어(커피 음료)도 의미로 찾는다")
    void t2() {
        List<Product> results = productService.knnSearch("hot drink in the morning", 1);

        System.out.println("hot drink 검색 결과 : " + results.stream().map(Product::getName).toList());
        assertEquals("Espresso Coffee", results.get(0).getName());
    }

    @Test
    @DisplayName("KNN 검색 - k개만 돌려준다")
    void t3() {
        assertEquals(3, productService.knnSearch("computer", 3).size());
    }
}

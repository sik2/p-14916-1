package com.back.product.controller;

import com.back.TestcontainersConfiguration;
import com.back.product.entity.Product;
import com.back.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("POST /api/v1/products - 상품 생성, 응답 JSON에 키워드 목록이 들어 있다")
    void t1() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "LG Gram 17", "keywords": ["laptop", "lightweight", "windows"]}
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("LG Gram 17"))
                .andExpect(jsonPath("$.keywords", hasSize(3)))
                .andExpect(jsonPath("$.keywords[0].keyword").value("laptop"))
                .andExpect(jsonPath("$.keywords[0].product").doesNotExist()); // @JsonIgnore
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - 없는 id는 404")
    void t2() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - 이름과 키워드 수정")
    void t3() throws Exception {
        Product product = productService.create("Old Name", List.of("old"));

        mockMvc.perform(put("/api/v1/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "New Name", "keywords": ["new", "fresh"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.keywords[*].keyword", containsInAnyOrder("new", "fresh")));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - 삭제 후 조회하면 404")
    void t4() throws Exception {
        Product product = productService.create("To Delete", List.of("temp"));

        mockMvc.perform(delete("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/products/search?query=...&k=... - KNN 검색 API")
    void t5() throws Exception {
        productService.create("Gaming Keyboard", List.of("gaming", "keyboard", "mechanical"));

        mockMvc.perform(get("/api/v1/products/search").param("query", "gaming").param("k", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}

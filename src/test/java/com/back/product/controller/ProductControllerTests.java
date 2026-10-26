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
import org.springframework.test.web.servlet.MvcResult;

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

    @Test
    @DisplayName("GET /api/v1/products/{id}/similar - 유사 상품 조회, 자기 자신 제외")
    void t6() throws Exception {
        Product laptop = productService.create("MacBook Pro", List.of("laptop", "apple", "development"));
        productService.create("iPhone 15", List.of("smartphone", "apple", "communication"));
        productService.create("iPad Pro", List.of("tablet", "apple", "drawing"));

        mockMvc.perform(get("/api/v1/products/{id}/similar", laptop.getId()).param("k", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", not(hasItem("MacBook Pro"))));
    }

    @Test
    @DisplayName("POST /api/v1/products/search - 키워드 목록으로 검색")
    void t7() throws Exception {
        mockMvc.perform(post("/api/v1/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keywords": ["apple", "laptop"], "k": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("POST /api/v1/products/chat - 채팅으로 상품 추천 요청")
    void t8() throws Exception {
        productService.create("Gaming Laptop", List.of("gaming", "laptop", "high-performance"));

        mockMvc.perform(post("/api/v1/products/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "I need a laptop for gaming. Can you recommend something?"}
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("GET /api/v1/products/chat/stream - SSE 스트리밍, 토큰이 JSON으로 온다")
    void t9() throws Exception {
        productService.create("Gaming Laptop", List.of("gaming", "laptop", "high-performance"));

        MvcResult result = mockMvc.perform(get("/api/v1/products/chat/stream")
                        .param("message", "I need a laptop for gaming")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted()) // SseEmitter를 반환하면 비동기 요청이 시작된다
                .andReturn();

        result.getAsyncResult(120_000); // 스트림이 끝날 때까지 기다린다 (최대 120초)
        String body = result.getResponse().getContentAsString();
        System.out.println("SSE 응답 앞부분 : " + body.substring(0, Math.min(300, body.length())));

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("event:message"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("data:{\"content\":"));
    }
}

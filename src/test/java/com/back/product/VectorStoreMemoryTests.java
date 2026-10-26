package com.back.product;

import com.back.TestcontainersConfiguration;
import com.back.product.service.ProductChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class VectorStoreMemoryTests {
    @Autowired
    private ProductChatService productChatService;
    @Autowired
    private VectorStore vectorStore;

    @Test
    @DisplayName("대화 내용이 ChromaDB에 임베딩되어 저장되고, 의미로 다시 찾을 수 있다")
    void t1() {
        productChatService.chat("I am allergic to caffeine.", "vector-user-1");

        List<Document> found = vectorStore.similaritySearch(SearchRequest.builder()
                .query("coffee allergy") // 저장한 문장과 단어가 다르지만 의미가 비슷하다
                .topK(5)
                .filterExpression("conversationId == 'vector-user-1'") // 대화방 id로 거른다
                .build());

        found.forEach(d -> System.out.println("ChromaDB 문서 : " + d.getText() + " / " + d.getMetadata()));
        assertFalse(found.isEmpty());
        assertTrue(found.stream().anyMatch(d -> d.getText().contains("allergic to caffeine")));
    }

    @Test
    @DisplayName("다른 대화방의 기억은 검색되지 않는다")
    void t2() {
        productChatService.chat("My favorite color is green.", "vector-user-2");

        List<Document> other = vectorStore.similaritySearch(SearchRequest.builder()
                .query("favorite color")
                .topK(5)
                .filterExpression("conversationId == 'vector-user-3'")
                .build());

        assertTrue(other.isEmpty());
    }
}

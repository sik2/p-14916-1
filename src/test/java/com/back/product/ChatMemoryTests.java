package com.back.product;

import com.back.TestcontainersConfiguration;
import com.back.product.service.ProductChatService;
import com.back.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class ChatMemoryTests {
    @Autowired
    private ProductChatService productChatService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("대화 1번 = 메시지 2개(USER, ASSISTANT)가 PostgreSQL에 저장된다")
    void t1() {
        productService.create("노트북", List.of("laptop", "computer"));

        productChatService.chat("노트북 추천해줘", "memory-user-1");

        List<Message> messages = chatMemory.get("memory-user-1");
        assertEquals(List.of(MessageType.USER, MessageType.ASSISTANT),
                messages.stream().map(Message::getMessageType).toList());

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spring_ai_chat_memory WHERE conversation_id = ?", Integer.class, "memory-user-1");
        assertEquals(2, rows);
    }

    @Test
    @DisplayName("userId가 다르면 대화 이력이 섞이지 않는다")
    void t2() {
        productChatService.chat("스마트폰 추천해줘", "user-A");
        productChatService.chat("노트북 추천해줘", "user-B");

        assertEquals(2, chatMemory.get("user-A").size());
        assertEquals("스마트폰 추천해줘", chatMemory.get("user-A").get(0).getText());
        assertEquals("노트북 추천해줘", chatMemory.get("user-B").get(0).getText());
    }

    @Test
    @DisplayName("같은 userId로 이어서 질문하면 이전 대화가 함께 전달된다 (메시지가 쌓인다)")
    void t3() {
        productChatService.chat("내 이름은 철수야. 기억해줘.", "memory-user-3");
        String answer = productChatService.chat("내 이름이 뭐라고 했지?", "memory-user-3").message();

        System.out.println("두 번째 답변 : " + answer);
        assertEquals(4, chatMemory.get("memory-user-3").size());
    }

    @Test
    @DisplayName("maxMessages(10) : 6번 대화하면 가장 오래된 메시지부터 지워져 10개만 남는다")
    void t4() {
        for (int i = 1; i <= 6; i++) {
            productChatService.chat("Reply with one word: " + i, "memory-user-4");
        }

        List<Message> messages = chatMemory.get("memory-user-4");
        assertEquals(10, messages.size());
        assertEquals("Reply with one word: 2", messages.get(0).getText()); // 1번째 질문/답변은 밀려났다
    }
}

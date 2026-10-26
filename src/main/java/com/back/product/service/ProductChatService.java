package com.back.product.service;

import com.back.product.tool.ProductSearchTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductChatService {

    private final ChatClient chatClient;
    private final ProductSearchTool productSearchTool;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    private static final String SYSTEM_PROMPT = """
            당신은 친절한 상품 추천 어시스턴트입니다.
            사용자의 요구사항과 선호도에 맞는 상품을 찾는 것을 도와줍니다.

            사용자가 상품에 대해 질문하면, 사용 가능한 도구를 사용하여 상품을 검색하세요:
            - searchProducts: 키워드로 상품 검색 (키워드는 영어로)
            - findSimilarProducts: 특정 상품과 유사한 상품 찾기
            - getProductById: 특정 상품의 상세 정보 조회
            - getAllProducts: 모든 상품 목록 조회

            검색 결과에 있는 상품만 추천하세요. 결과에 없는 상품을 지어내지 마세요.
            상품을 찾지 못한 경우, 사용자에게 알리고 대안적인 검색어를 제안하세요.
            이전 대화 내용을 기억하고 맥락에 맞는 응답을 제공하세요.
            """;

    // conversationId : 대화방 id. 같은 id끼리 이전 대화를 공유한다 (최대 36자, SPRING_AI_CHAT_MEMORY 컬럼 크기)
    public ChatResponse chat(String userMessage, String conversationId) {
        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .tools(productSearchTool) // ProductSearchTool의 모든 @Tool 메서드를 AI가 쓸 수 있게 등록
                .advisors(memoryAdvisors())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return new ChatResponse(response);
    }

    // 토큰이 생성되는 대로 흘려보낸다. 도구 호출은 Spring AI가 boundedElastic 스레드에서 실행한다.
    public Flux<String> chatStream(String userMessage, String conversationId) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .tools(productSearchTool)
                .advisors(memoryAdvisors())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    // 단기 기억 : 최근 메시지 10개를 그대로 붙인다 (JDBC)
    // 장기 기억 : 지금 질문과 의미가 비슷한 과거 대화를 찾아 시스템 프롬프트에 붙인다 (ChromaDB)
    private List<Advisor> memoryAdvisors() {
        return List.of(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                VectorStoreChatMemoryAdvisor.builder(vectorStore).defaultTopK(3).build()
        );
    }

    public record ChatResponse(String message) {}
}

package com.back.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiConfig {
    // 저장소(pgvector 컬럼 vector(384))의 벡터 차원과 반드시 같아야 한다
    public static final int EMBEDDING_DIMENSIONS = 384;

    // default 프로필에는 OpenAI, Ollama ChatModel이 모두 존재한다.
    // 파라미터 이름(openAiChatModel)과 같은 이름의 빈이 주입된다.
    @Bean
    @Profile("default")
    public ChatClient openaiChatClient(ChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean
    @Profile("test")
    public ChatClient ollamaChatClient(ChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    // Spring AI 2.0.1의 Ollama 임베딩 설정(spring.ai.ollama.embedding.*)에는 dimensions가 없다.
    // 그래서 차원을 지정한 OllamaEmbeddingModel을 직접 등록한다. (자동 설정 빈은 만들어지지 않는다)
    @Bean
    @Profile("test")
    public OllamaEmbeddingModel ollamaEmbeddingModel(
            OllamaApi ollamaApi,
            @Value("${spring.ai.ollama.embedding.model}") String model) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .options(OllamaEmbeddingOptions.builder()
                        .model(model)
                        .dimensions(EMBEDDING_DIMENSIONS)
                        .build())
                .build();
    }
}

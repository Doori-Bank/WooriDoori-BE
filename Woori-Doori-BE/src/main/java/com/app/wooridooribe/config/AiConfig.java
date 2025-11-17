package com.app.wooridooribe.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    @Bean
    @Primary
    public ChatModel chatModel(@Qualifier("openAiChatModel") ChatModel openAiChatModel) {
        return openAiChatModel;
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(@Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }

    @Bean
    @ConditionalOnMissingBean
    public ChromaApi chromaApi(
            @Value("${spring.ai.vectorstore.chroma.client.base-url:http://localhost:8000}") String baseUrl) {
        return new ChromaApi(baseUrl);
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorStore customVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        // ChromaVectorStore 생성자: (EmbeddingModel, ChromaApi, collectionName,
        // initializeSchema)
        return new ChromaVectorStore(
                embeddingModel,
                chromaApi,
                "SpringAiCollection",
                true
        );
    }
}

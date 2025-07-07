package com.example.resumeMatcherService.config;

import com.google.cloud.vertexai.VertexAI;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexAiGeminiConfig {


    public VertexAiGeminiChatModel vertexAiGeminiChatModel() {
        VertexAiGeminiChatOptions chatOptions = VertexAiGeminiChatOptions.builder()
                .temperature(0.7)
                .model(VertexAiGeminiChatModel.ChatModel.GEMINI_2_5_PRO)
                .build();
        ToolCallingManager toolManager = DefaultToolCallingManager.builder()
                .build();

        return VertexAiGeminiChatModel.builder()
                .vertexAI(new VertexAI())
                .defaultOptions(chatOptions)

                .toolCallingManager(toolManager)
                .build();
    }
}

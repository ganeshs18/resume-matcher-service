package com.example.resumeMatcherService.service;

import com.example.resumeMatcherService.dto.ResumeEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class VertexAiService {

    @Autowired
    private final VertexAiGeminiChatModel geminiChatModel;
    @Autowired
    private ObjectMapper objectMapper;

    public Flux<ResumeEvent> streamRawEnhancedResume(
            Map<String, String> blocks, String jobTitle, String company, String rawJd) {

        return Flux.defer(() -> {
            // Defensive null check
            if (objectMapper == null) {
                return Flux.just(new ResumeEvent(
                        ResumeEvent.EventType.ERROR,
                        "ObjectMapper not initialized.",
                        null,
                        0
                ));
            }

            String userInput;
            try {
                userInput = objectMapper.writeValueAsString(blocks);
            } catch (Exception e) {
                return Flux.just(new ResumeEvent(
                        ResumeEvent.EventType.ERROR,
                        "Failed to serialize input blocks: " + e.getMessage(),
                        null,
                        0
                ));
            }

            String systemPrompt = String.format("""
                You are a resume enhancement assistant.
                Enhance the resume block-by-block using this job title: "%s" at "%s".
                Job Description:
                %s

                Respond in markdown format with JSON structure containing enhanced text per block.
            """, jobTitle, company, rawJd);

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userInput)
            ));

            System.out.println(prompt.toString());
            StringBuilder rawBuffer = new StringBuilder();

            return geminiChatModel.stream(prompt)
                    .map(response -> {
                        Message message = response.getResult().getOutput();
                        if (message instanceof AssistantMessage assistantMessage) {
                            return assistantMessage.getText();
                        }
                        return ""; // if not assistant message
                    })
                    .filter(chunk -> chunk != null && !chunk.trim().isEmpty())
                    .map(chunk -> {
                        rawBuffer.append(chunk);

                        return new ResumeEvent(
                                ResumeEvent.EventType.AI_RAW_STREAM,
                                chunk,
                                null,
                                0
                        );
                    })
                    .onErrorResume(e -> Flux.just(new ResumeEvent(
                            ResumeEvent.EventType.ERROR,
                            "Streaming error: " + e.getMessage(),
                            null,
                            0
                    )))
                    .concatWith(Mono.just(new ResumeEvent(
                            ResumeEvent.EventType.COMPLETE,
                            "Streaming complete.",
                            null,
                            100
                    )));
        });
    }






}

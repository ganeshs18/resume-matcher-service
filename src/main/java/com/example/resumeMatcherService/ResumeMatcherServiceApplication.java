package com.example.resumeMatcherService;

import org.springframework.ai.model.vertexai.autoconfigure.gemini.VertexAiGeminiChatAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication

public class ResumeMatcherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeMatcherServiceApplication.class, args);
	}

}

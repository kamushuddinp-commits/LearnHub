
package com.learnhub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/ask")
public class AiController {

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @PostMapping
    public Map<String, String> ask(@RequestBody Map<String, String> body) {

        String prompt = body.getOrDefault("prompt", "").trim();

        if (prompt.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter a question or topic."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured yet. Add GEMINI_API_KEY and restart LearnHub."
            );
        }

        try {
            String requestJson = mapper.writeValueAsString(
                    Map.of(
                            "contents", new Object[]{
                                    Map.of(
                                            "parts", new Object[]{
                                                    Map.of("text", prompt)
                                            }
                                    )
                            },
                            "generationConfig", Map.of(
                                    "temperature", 0.6,
                                    "maxOutputTokens", 1200
                            )
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                                    + apiKey
                    ))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "The AI service could not complete that request. Check your API key and try again."
                );
            }

            JsonNode root = mapper.readTree(response.body());

            String answer = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            if (answer.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "The AI service returned an empty answer. Please try again."
                );
            }

            return Map.of("answer", answer);

        } catch (ResponseStatusException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not reach the AI service. Please try again."
            );
        }
    }
}

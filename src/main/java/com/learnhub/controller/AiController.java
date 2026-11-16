package com.learnhub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/ai/ask")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    // Known reliable models for fallback in order of preference
    private static final List<String> KNOWN_MODELS = List.of(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    );

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}")
    private String configuredModel;

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient client;

    @FunctionalInterface
    public interface ResponseSender {
        HttpResponse<String> send(HttpRequest request) throws Exception;
    }

    private final ResponseSender responseSender;

    public AiController() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.responseSender = req -> this.client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public AiController(ResponseSender responseSender, String apiKey, String configuredModel) {
        this.client = null;
        this.responseSender = responseSender;
        this.apiKey = apiKey;
        this.configuredModel = configuredModel;
    }

    @PostMapping
    public Map<String, String> ask(@RequestBody Map<String, String> body) {
        String prompt = body != null ? body.getOrDefault("prompt", "").trim() : "";

        if (prompt.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter a question or topic."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("AI request rejected: GEMINI_API_KEY is not configured");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured yet. Add GEMINI_API_KEY and restart LearnHub."
            );
        }

        List<String> modelsToTry = getModelsToTry();
        String lastErrorMessage = null;
        int lastStatusCode = 0;

        for (String model : modelsToTry) {
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
                                        "temperature", 0.7,
                                        "maxOutputTokens", 2048
                                )
                        )
                );

                // Authenticate via x-goog-api-key header to prevent API key exposure in URLs and logs
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GEMINI_API_BASE + model + ":generateContent"))
                        .timeout(Duration.ofSeconds(45))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey.trim())
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<String> response = responseSender.send(request);

                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    String answer = parseSuccessfulResponse(response.body());
                    logger.info("AI response successfully generated using model: {}", model);
                    return Map.of("answer", answer);
                }

                lastStatusCode = statusCode;
                lastErrorMessage = extractSafeErrorMessage(response.body());
                logger.warn("Gemini API call to model '{}' failed with HTTP {}: {}", model, statusCode, lastErrorMessage);

                // Authentication or permission errors cannot be resolved by fallback
                if (statusCode == 401 || statusCode == 403) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid or unauthorized Gemini API key. Please check your GEMINI_API_KEY environment variable."
                    );
                }

                // If quota exhausted, don't keep hammering other models with the same project quota
                if (statusCode == 429) {
                    throw new ResponseStatusException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Gemini API rate limit or quota exceeded. Please wait a moment and try again."
                    );
                }

                // For 404 (model not found) or 503 (model capacity exhausted), loop will try next fallback model
                logger.info("Model '{}' was unavailable (HTTP {}). Checking fallback models...", model, statusCode);

            } catch (ResponseStatusException ex) {
                throw ex;
            } catch (HttpTimeoutException ex) {
                logger.error("Gemini API call to model '{}' timed out: {}", model, ex.getMessage());
                lastStatusCode = HttpStatus.GATEWAY_TIMEOUT.value();
                lastErrorMessage = "Connection to AI service timed out.";
            } catch (Exception ex) {
                logger.error("Error communicating with Gemini model '{}': {}", model, ex.getMessage());
                lastErrorMessage = ex.getMessage();
            }
        }

        // All models failed or were exhausted
        if (lastStatusCode == 503 || (lastErrorMessage != null && lastErrorMessage.toLowerCase().contains("capacity"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The AI service is currently at capacity. Please try again in a moment."
            );
        }

        String clientMessage = (lastErrorMessage != null && !lastErrorMessage.isBlank())
                ? "The AI service could not complete that request: " + lastErrorMessage
                : "The AI service could not complete that request. Check your API key and try again.";

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, clientMessage);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = Map.of(
                "error", ex.getStatusCode().toString(),
                "status", ex.getStatusCode().value(),
                "message", ex.getReason() != null ? ex.getReason() : "An error occurred"
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    private List<String> getModelsToTry() {
        Set<String> models = new LinkedHashSet<>();
        if (configuredModel != null && !configuredModel.isBlank()) {
            models.add(configuredModel.trim());
        }
        models.addAll(KNOWN_MODELS);
        return new ArrayList<>(models);
    }

    private String parseSuccessfulResponse(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                JsonNode blockReason = root.path("promptFeedback").path("blockReason");
                if (!blockReason.isMissingNode() && !blockReason.asText().isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "AI safety filters blocked this prompt: " + blockReason.asText()
                    );
                }
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "The AI service returned no response candidates."
                );
            }

            StringBuilder textBuilder = new StringBuilder();
            JsonNode parts = candidates.path(0).path("content").path("parts");

            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        textBuilder.append(part.path("text").asText());
                    }
                }
            }

            String answer = textBuilder.toString().trim();

            if (answer.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "The AI service returned an empty answer. Please try again."
                );
            }

            return answer;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Failed to parse Gemini API JSON response", ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to parse response from the AI service."
            );
        }
    }

    private String extractSafeErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Empty response from AI service";
        }
        try {
            JsonNode root = mapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            if (!message.isBlank()) {
                // Ensure no API keys or sensitive values are retained
                return message.replaceAll("key=[A-Za-z0-9_-]+", "key=REDACTED");
            }
        } catch (Exception ignored) {
            // Non-JSON response
        }
        return "HTTP error from AI service";
    }
}

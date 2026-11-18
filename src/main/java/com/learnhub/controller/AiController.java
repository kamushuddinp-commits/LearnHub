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
import java.util.Map;

@RestController
@RequestMapping("/api/ai/ask")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/interactions";

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.model:${GEMINI_MODEL:gemini-3.8-flash}}")
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
        this.responseSender = request ->
                this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public AiController(ResponseSender responseSender, String apiKey, String configuredModel) {
        this.client = null;
        this.responseSender = responseSender;
        this.apiKey = apiKey;
        this.configuredModel = configuredModel;
    }

    @PostMapping
    public Map<String, String> ask(@RequestBody Map<String, String> body) {

        String prompt = body != null
                ? body.getOrDefault("prompt", "").trim()
                : "";

        if (prompt.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter a question or topic."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Gemini API key is not configured");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured. Please check the GEMINI_API_KEY setting."
            );
        }

        String model = configuredModel == null || configuredModel.isBlank()
                ? "gemini-3.8-flash"
                : configuredModel.trim();

        try {
            String requestJson = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "input", prompt,
                    "store", false
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = responseSender.send(request);
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                String answer = parseSuccessfulResponse(response.body());
                logger.info("Gemini interaction completed using model: {}", model);
                return Map.of("answer", answer);
            }

            String error = extractSafeErrorMessage(response.body());
            logger.error("Gemini Interactions API returned HTTP {}: {}", status, error);

            if (status == 401 || status == 403) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Gemini API key is invalid or not authorized. Check your API key."
                );
            }

            if (status == 429) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Gemini API quota or rate limit reached. Please try again later."
                );
            }

            if (status == 503 || status == 504) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini is temporarily unavailable. Please try again shortly."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini API request failed: " + error
            );

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            logger.error("Gemini request timed out", ex);
            throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Gemini took too long to respond. Please try again."
            );
        } catch (Exception ex) {
            logger.error("Error communicating with Gemini", ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not connect to the Gemini AI service. Please try again."
            );
        }
    }

    private String parseSuccessfulResponse(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);

            JsonNode directText = root.path("output_text");
            if (directText.isTextual() && !directText.asText().isBlank()) {
                return directText.asText().trim();
            }

            StringBuilder answer = new StringBuilder();
            JsonNode steps = root.path("steps");

            if (steps.isArray()) {
                for (JsonNode step : steps) {
                    if ("model_output".equals(step.path("type").asText())) {
                        JsonNode content = step.path("content");
                        if (content.isArray()) {
                            for (JsonNode item : content) {
                                if ("text".equals(item.path("type").asText())) {
                                    answer.append(item.path("text").asText());
                                }
                            }
                        }
                    }
                }
            }

            String result = answer.toString().trim();

            if (result.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini returned an empty answer. Please try again."
                );
            }

            return result;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Could not parse Gemini response", ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not read the response from Gemini."
            );
        }
    }

    private String extractSafeErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Empty response from Gemini.";
        }

        try {
            JsonNode root = mapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");

            if (!message.isBlank()) {
                return message.replaceAll(
                        "key=[A-Za-z0-9_-]+",
                        "key=REDACTED"
                );
            }
        } catch (Exception ignored) {
            // Ignore non-JSON error responses.
        }

        return "HTTP error from Gemini.";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex) {

        Map<String, Object> body = Map.of(
                "error", ex.getStatusCode().toString(),
                "status", ex.getStatusCode().value(),
                "message", ex.getReason() != null
                        ? ex.getReason()
                        : "An error occurred"
        );

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}

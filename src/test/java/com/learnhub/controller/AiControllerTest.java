package com.learnhub.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AiControllerTest {

    private static class SimpleHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;
        private final HttpRequest request;

        public SimpleHttpResponse(int statusCode, String body, HttpRequest request) {
            this.statusCode = statusCode;
            this.body = body;
            this.request = request;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (k, v) -> true);
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request != null ? request.uri() : URI.create("https://example.com");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }
    }

    private static final String SUCCESS_RESPONSE_JSON = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Here is the comprehensive explanation with examples and a summary."
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
            """;

    @Test
    @DisplayName("Should throw 400 Bad Request when prompt is blank")
    void testAsk_blankPrompt_throwsBadRequest() {
        AiController controller = new AiController(req -> null, "fake-api-key", "gemini-2.5-flash");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.ask(Map.of("prompt", "   "))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Please enter a question or topic.", ex.getReason());
    }

    @Test
    @DisplayName("Should throw 503 Service Unavailable when API key is missing")
    void testAsk_missingApiKey_throwsServiceUnavailable() {
        AiController controller = new AiController(req -> null, "", "gemini-2.5-flash");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.ask(Map.of("prompt", "Explain Java"))
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertTrue(ex.getReason().contains("AI is not configured yet"));
    }

    @Test
    @DisplayName("Explain Topic feature succeeds and securely sends x-goog-api-key header")
    void testAsk_explainTopic_success() {
        String testApiKey = "test-secret-key-12345";
        List<HttpRequest> capturedRequests = new ArrayList<>();

        AiController controller = new AiController(req -> {
            capturedRequests.add(req);
            return new SimpleHttpResponse(200, SUCCESS_RESPONSE_JSON, req);
        }, testApiKey, "gemini-2.5-flash");

        String explainPrompt = "Teach me this topic from beginner level with a simple definition, step-by-step explanation, a real-world example, and a short recap: Polymorphism in Java";
        Map<String, String> result = controller.ask(Map.of("prompt", explainPrompt));

        assertNotNull(result);
        assertEquals("Here is the comprehensive explanation with examples and a summary.", result.get("answer"));

        assertEquals(1, capturedRequests.size());
        HttpRequest sentRequest = capturedRequests.get(0);

        // Security check: API key MUST be in header, NOT in URL
        assertEquals(testApiKey, sentRequest.headers().firstValue("x-goog-api-key").orElse(null));
        assertFalse(sentRequest.uri().toString().contains("key="), "URI must not contain API key parameter");
        assertEquals("POST", sentRequest.method());
        assertEquals("application/json", sentRequest.headers().firstValue("Content-Type").orElse(null));
    }

    @Test
    @DisplayName("Summarize Notes feature succeeds")
    void testAsk_summarizeNotes_success() {
        String summaryResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "# Summary\\n* Key Point 1\\n* Key Point 2"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        AiController controller = new AiController(
                req -> new SimpleHttpResponse(200, summaryResponse, req),
                "fake-api-key",
                "gemini-2.5-flash"
        );

        String summarizePrompt = "Summarize these study notes into clear headings and concise bullet points. Preserve important facts and definitions: Relational database normalization forms";
        Map<String, String> result = controller.ask(Map.of("prompt", summarizePrompt));

        assertNotNull(result);
        assertEquals("# Summary\n* Key Point 1\n* Key Point 2", result.get("answer"));
    }

    @Test
    @DisplayName("Generate Quiz feature succeeds")
    void testAsk_generateQuiz_success() {
        String quizResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "1. What is JVM?\\nA) Java Virtual Machine (Correct)\\nB) Java Variable Method"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        AiController controller = new AiController(
                req -> new SimpleHttpResponse(200, quizResponse, req),
                "fake-api-key",
                "gemini-2.5-flash"
        );

        String quizPrompt = "Create 5 multiple-choice questions from this topic/notes. Give 4 options per question, mark the correct answer, and explain why: Java Memory Model";
        Map<String, String> result = controller.ask(Map.of("prompt", quizPrompt));

        assertNotNull(result);
        assertTrue(result.get("answer").contains("What is JVM?"));
    }

    @Test
    @DisplayName("Flashcards feature succeeds")
    void testAsk_flashcards_success() {
        String flashcardsResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Front: What is ACID?\\nBack: Atomicity, Consistency, Isolation, Durability"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        AiController controller = new AiController(
                req -> new SimpleHttpResponse(200, flashcardsResponse, req),
                "fake-api-key",
                "gemini-2.5-flash"
        );

        String flashcardsPrompt = "Create 8 concise question-and-answer flashcards from this topic/notes. Format as Front: ... and Back: ...: SQL Transactions";
        Map<String, String> result = controller.ask(Map.of("prompt", flashcardsPrompt));

        assertNotNull(result);
        assertTrue(result.get("answer").contains("Front: What is ACID?"));
    }

    @Test
    @DisplayName("7-Day Study Plan feature succeeds")
    void testAsk_studyPlan_success() {
        String planResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Day 1: Introduction to Spring Boot\\nDay 2: Controllers and Routing"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        AiController controller = new AiController(
                req -> new SimpleHttpResponse(200, planResponse, req),
                "fake-api-key",
                "gemini-2.5-flash"
        );

        String planPrompt = "Create a realistic 7-day beginner study plan for this topic. Include daily goals, estimated time, practice, and a revision day: Spring Boot REST APIs";
        Map<String, String> result = controller.ask(Map.of("prompt", planPrompt));

        assertNotNull(result);
        assertTrue(result.get("answer").contains("Day 1: Introduction to Spring Boot"));
    }

    @Test
    @DisplayName("Should automatically fallback to next model when primary model returns 503 MODEL_CAPACITY_EXHAUSTED")
    void testAsk_modelCapacityExhausted_fallsBackToNextModel() {
        String capacityExhaustedJson = """
                {
                  "error": {
                    "code": 503,
                    "message": "MODEL_CAPACITY_EXHAUSTED: No capacity available for model gemini-3.8-flash",
                    "status": "UNAVAILABLE"
                  }
                }
                """;

        List<String> requestedUris = new ArrayList<>();

        AiController controller = new AiController(req -> {
            requestedUris.add(req.uri().toString());
            if (requestedUris.size() == 1) {
                return new SimpleHttpResponse(503, capacityExhaustedJson, req);
            }
            return new SimpleHttpResponse(200, SUCCESS_RESPONSE_JSON, req);
        }, "fake-api-key", "gemini-3.8-flash");

        Map<String, String> result = controller.ask(Map.of("prompt", "Explain encapsulation"));

        assertNotNull(result);
        assertEquals("Here is the comprehensive explanation with examples and a summary.", result.get("answer"));

        // Verify that 2 requests were made (1st to gemini-3.8-flash, 2nd to fallback model)
        assertEquals(2, requestedUris.size());
        assertTrue(requestedUris.get(0).contains("gemini-3.8-flash"));
        assertTrue(requestedUris.get(1).contains("gemini-2.5-flash"));
    }

    @Test
    @DisplayName("Should automatically fallback to next model when primary model returns 404 Not Found")
    void testAsk_modelNotFound_fallsBackToNextModel() {
        String notFoundJson = """
                {
                  "error": {
                    "code": 404,
                    "message": "models/gemini-3.8-flash is not found for API version v1beta",
                    "status": "NOT_FOUND"
                  }
                }
                """;

        List<String> requestedUris = new ArrayList<>();

        AiController controller = new AiController(req -> {
            requestedUris.add(req.uri().toString());
            if (requestedUris.size() == 1) {
                return new SimpleHttpResponse(404, notFoundJson, req);
            }
            return new SimpleHttpResponse(200, SUCCESS_RESPONSE_JSON, req);
        }, "fake-api-key", "gemini-3.8-flash");

        Map<String, String> result = controller.ask(Map.of("prompt", "Explain recursion"));

        assertNotNull(result);
        assertEquals("Here is the comprehensive explanation with examples and a summary.", result.get("answer"));
        assertEquals(2, requestedUris.size());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when API key is rejected with 401/403")
    void testAsk_invalidApiKey_throwsUnauthorized() {
        String invalidKeyJson = """
                {
                  "error": {
                    "code": 401,
                    "message": "API key not valid. Please pass a valid API key.",
                    "status": "UNAUTHENTICATED"
                  }
                }
                """;

        AiController controller = new AiController(
                req -> new SimpleHttpResponse(401, invalidKeyJson, req),
                "invalid-key",
                "gemini-2.5-flash"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.ask(Map.of("prompt", "Explain inheritance"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid or unauthorized Gemini API key"));
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when Gemini API rate limit is exceeded")
    void testAsk_rateLimit_throwsTooManyRequests() {
        String rateLimitJson = """
                {
                  "error": {
                    "code": 429,
                    "message": "Resource has been exhausted (e.g. check quota).",
                    "status": "RESOURCE_EXHAUSTED"
                  }
                }
                """;

        AiController controller = new AiController(
                req -> new SimpleHttpResponse(429, rateLimitJson, req),
                "fake-api-key",
                "gemini-2.5-flash"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.ask(Map.of("prompt", "Explain threads"))
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertTrue(ex.getReason().contains("rate limit or quota exceeded"));
    }

    @Test
    @DisplayName("ExceptionHandler returns structured JSON with message, error, and status")
    void testExceptionHandler_returnsStructuredResponse() {
        AiController controller = new AiController();
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service unavailable");

        ResponseEntity<Map<String, Object>> entity = controller.handleResponseStatusException(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals(502, entity.getBody().get("status"));
        assertEquals("Service unavailable", entity.getBody().get("message"));
    }
}

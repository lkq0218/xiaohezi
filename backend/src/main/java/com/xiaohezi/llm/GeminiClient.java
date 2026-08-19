package com.xiaohezi.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {
    private final GeminiProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;
    private final MeterRegistry meters;

    public GeminiClient(GeminiProperties properties, ObjectMapper mapper, MeterRegistry meters) {
        this.properties = properties;
        this.mapper = mapper;
        this.meters = meters;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds())).build();
    }

    public boolean isConfigured() { return properties.isConfigured(); }

    public Completion complete(String instructions, String input, int maxTokens) {
        return complete(instructions, input, maxTokens, null);
    }

    public Completion complete(String instructions, String input, int maxTokens,
                               Map<String, Object> jsonSchema) {
        if (!isConfigured()) throw new LlmUnavailableException("Gemini API Key 未配置");
        Timer.Sample sample = Timer.start(meters);
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("maxOutputTokens", maxTokens);
            config.put("thinkingConfig", Map.of("thinkingLevel", "minimal"));
            if (jsonSchema != null) {
                config.put("responseMimeType", "application/json");
                config.put("responseJsonSchema", jsonSchema);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", instructions))));
            body.put("contents", List.of(Map.of("role", "user",
                "parts", List.of(Map.of("text", input)))));
            body.put("generationConfig", config);

            String endpoint = trimSlash(properties.getBaseUrl()) + "/models/"
                + properties.getModel() + ":generateContent";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .header("x-goog-api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmUnavailableException("Gemini 调用失败，HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            StringBuilder text = new StringBuilder();
            for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
                if (part.has("text")) text.append(part.path("text").asText());
            }
            if (text.isEmpty()) throw new LlmUnavailableException("Gemini 返回内容为空");
            JsonNode usage = root.path("usageMetadata");
            Completion completion = new Completion(text.toString(),
                root.path("modelVersion").asText(properties.getModel()),
                usage.path("promptTokenCount").asInt(), usage.path("candidatesTokenCount").asInt());
            meters.counter("xiaohezi.llm.requests", "result", "success").increment();
            meters.counter("xiaohezi.llm.tokens", "type", "input").increment(completion.promptTokens());
            meters.counter("xiaohezi.llm.tokens", "type", "output").increment(completion.completionTokens());
            meters.counter("xiaohezi.llm.estimated.cost.usd").increment(
                completion.promptTokens() * properties.getInputUsdPerMillionTokens() / 1_000_000d
                    + completion.completionTokens() * properties.getOutputUsdPerMillionTokens() / 1_000_000d);
            sample.stop(meters.timer("xiaohezi.llm.latency", "result", "success"));
            return completion;
        } catch (LlmUnavailableException e) {
            recordFailure(sample);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(sample);
            throw new LlmUnavailableException("Gemini 调用被中断", e);
        } catch (Exception e) {
            recordFailure(sample);
            throw new LlmUnavailableException("Gemini 调用异常", e);
        }
    }

    private void recordFailure(Timer.Sample sample) {
        meters.counter("xiaohezi.llm.requests", "result", "failure").increment();
        sample.stop(meters.timer("xiaohezi.llm.latency", "result", "failure"));
    }

    private String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record Completion(String content, String model, int promptTokens, int completionTokens) {}
    public static class LlmUnavailableException extends RuntimeException {
        public LlmUnavailableException(String message) { super(message); }
        public LlmUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}

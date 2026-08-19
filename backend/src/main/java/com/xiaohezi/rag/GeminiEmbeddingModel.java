package com.xiaohezi.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohezi.llm.GeminiProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiEmbeddingModel implements EmbeddingModel {
    private final GeminiProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient client;

    public GeminiEmbeddingModel(GeminiProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds())).build();
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        return batch(texts, "RETRIEVAL_DOCUMENT");
    }

    @Override
    public float[] embedQuery(String text) {
        return batch(List.of(text), "RETRIEVAL_QUERY").getFirst();
    }

    @Override
    public boolean isConfigured() { return properties.isConfigured(); }

    private List<float[]> batch(List<String> texts, String taskType) {
        if (!isConfigured()) throw new IllegalStateException("Gemini Embedding 未配置");
        try {
            String modelName = "models/" + properties.getEmbeddingModel();
            List<Map<String, Object>> requests = texts.stream().map(text -> Map.<String, Object>of(
                "model", modelName,
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "taskType", taskType,
                "outputDimensionality", properties.getEmbeddingDimensions()
            )).toList();
            String base = properties.getBaseUrl().replaceAll("/+$", "");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/" + modelName + ":batchEmbedContents"))
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .header("x-goog-api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    mapper.writeValueAsString(Map.of("requests", requests)))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Gemini Embedding HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : root.path("embeddings")) {
                JsonNode values = item.path("values");
                float[] vector = new float[values.size()];
                for (int i = 0; i < values.size(); i++) vector[i] = (float) values.get(i).asDouble();
                normalize(vector);
                vectors.add(vector);
            }
            if (vectors.size() != texts.size()) throw new IllegalStateException("Embedding 数量不匹配");
            return vectors;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding 调用中断", e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Embedding 调用失败", e);
        }
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) sum += value * value;
        double norm = Math.sqrt(sum);
        if (norm > 0) for (int i = 0; i < vector.length; i++) vector[i] /= (float) norm;
    }
}

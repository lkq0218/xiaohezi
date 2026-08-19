package com.xiaohezi.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class KnowledgeAnswerCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public KnowledgeAnswerCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public Optional<CachedAnswer> get(String question) {
        try {
            String value = redis.opsForValue().get(key(question));
            return value == null ? Optional.empty() : Optional.of(mapper.readValue(value, CachedAnswer.class));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public void put(String question, CachedAnswer answer) {
        try {
            redis.opsForValue().set(key(question), mapper.writeValueAsString(answer), Duration.ofMinutes(30));
        } catch (Exception ignored) {
            // Redis is an optimization. Failure must not break the main path.
        }
    }

    private String key(String question) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(question.trim().getBytes(StandardCharsets.UTF_8));
            return "rag:answer:" + java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record CachedAnswer(String content, List<CachedCitation> citations) {}
    public record CachedCitation(String title, String chunkId) {}
}

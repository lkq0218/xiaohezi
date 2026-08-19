package com.xiaohezi.api;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@Service
public class ConcurrentWebService {
    private static final Set<String> ALLOWED_HOSTS = Set.of("localhost", "127.0.0.1");
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2)).build();
    private final Semaphore bulkhead = new Semaphore(20);

    public CompletableFuture<List<Map<String, Object>>> fetch(List<String> urls) {
        if (urls == null || urls.isEmpty() || urls.size() > 5) {
            throw new IllegalArgumentException("urls 数量必须为 1 到 5");
        }
        List<CompletableFuture<Map<String, Object>>> futures = urls.stream()
            .map(this::fetchOne).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
            .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .thenApply(unused -> futures.stream().map(CompletableFuture::join).toList());
    }

    private CompletableFuture<Map<String, Object>> fetchOne(String value) {
        URI uri = URI.create(value);
        if (!"http".equals(uri.getScheme()) || !ALLOWED_HOSTS.contains(uri.getHost())) {
            throw new IllegalArgumentException("教学接口只允许访问本机 HTTP 地址");
        }
        if (!bulkhead.tryAcquire()) {
            return CompletableFuture.completedFuture(Map.of("url", value, "status", 429));
        }
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenApply(response -> Map.<String, Object>of(
                "url", value, "status", response.statusCode()))
            .exceptionally(error -> Map.of("url", value, "status", 503,
                "error", error.getClass().getSimpleName()))
            .whenComplete((result, error) -> bulkhead.release());
    }
}

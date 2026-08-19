package com.xiaohezi.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientFaultTest {
    @Test
    void unavailableDependencyFailsClosedAndEmitsMetric() {
        GeminiProperties properties = new GeminiProperties();
        properties.setEnabled(true); properties.setApiKey("test-only");
        properties.setBaseUrl("http://127.0.0.1:1"); properties.setConnectTimeoutSeconds(1);
        properties.setReadTimeoutSeconds(1);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        GeminiClient client = new GeminiClient(properties, new ObjectMapper(), meters);

        assertThrows(GeminiClient.LlmUnavailableException.class,
            () -> client.complete("instruction", "input", 10));
        assertEquals(1, meters.counter("xiaohezi.llm.requests", "result", "failure").count());
    }
}

package com.xiaohezi.rag;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KnowledgeIndexEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexEventPublisher.class);
    private final ObjectProvider<RocketMQTemplate> template;
    private final boolean enabled;

    public KnowledgeIndexEventPublisher(ObjectProvider<RocketMQTemplate> template,
        @Value("${app.rocketmq.enabled:false}") boolean enabled) {
        this.template = template;
        this.enabled = enabled;
    }

    public void indexCompleted(int count) {
        if (!enabled) return;
        RocketMQTemplate rocket = template.getIfAvailable();
        if (rocket == null) return;
        try {
            rocket.convertAndSend("knowledge-index-events",
                Map.of("eventType", "INDEX_COMPLETED", "chunkCount", count,
                    "occurredAt", java.time.Instant.now().toString()));
        } catch (RuntimeException e) {
            log.warn("RocketMQ publish failed: {}", e.getMessage());
        }
    }
}

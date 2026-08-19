package com.xiaohezi.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohezi.llm.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgePersistenceService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgePersistenceService.class);
    private final KnowledgeChunkRepository repository;
    private final ObjectMapper mapper;
    private final GeminiProperties properties;
    private final KnowledgeIndexEventPublisher publisher;

    public KnowledgePersistenceService(KnowledgeChunkRepository repository, ObjectMapper mapper,
                                       GeminiProperties properties, KnowledgeIndexEventPublisher publisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.properties = properties;
        this.publisher = publisher;
    }

    @Transactional
    public void replace(List<KnowledgeChunk> chunks) {
        try {
            List<KnowledgeChunkEntity> entities = chunks.stream().map(chunk -> {
                try {
                    return new KnowledgeChunkEntity(chunk,
                        chunk.embedding() == null ? null : mapper.writeValueAsString(chunk.embedding()),
                        properties.getEmbeddingModel());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
            repository.saveAll(entities);
            publisher.indexCompleted(entities.size());
        } catch (RuntimeException e) {
            log.warn("Knowledge persistence unavailable; keeping in-memory index: {}", e.getMessage());
        }
    }
}

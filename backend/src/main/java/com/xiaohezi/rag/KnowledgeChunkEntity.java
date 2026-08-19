package com.xiaohezi.rag;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunkEntity {
    @Id
    @Column(length = 80)
    String id;
    @Column(nullable = false, length = 80)
    String documentId;
    @Column(nullable = false, length = 200)
    String title;
    @Column(nullable = false, length = 300)
    String sectionName;
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    String content;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String embeddingJson;
    @Column(nullable = false, length = 80)
    String embeddingModel;
    @Column(nullable = false)
    int chunkIndex;
    @Column(nullable = false)
    Instant updatedAt;

    protected KnowledgeChunkEntity() {}

    public KnowledgeChunkEntity(KnowledgeChunk chunk, String embeddingJson, String embeddingModel) {
        this.id = chunk.id();
        this.documentId = chunk.documentId();
        this.title = chunk.title();
        this.sectionName = chunk.section();
        this.content = chunk.content();
        this.embeddingJson = embeddingJson;
        this.embeddingModel = embeddingModel;
        this.chunkIndex = chunk.index();
        this.updatedAt = Instant.now();
    }
}

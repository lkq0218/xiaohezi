package com.xiaohezi.rag;

public record KnowledgeChunk(String id, String documentId, String title, String section,
                             String content, int index, float[] embedding) {
    public KnowledgeChunk withEmbedding(float[] value) {
        return new KnowledgeChunk(id, documentId, title, section, content, index, value);
    }
}

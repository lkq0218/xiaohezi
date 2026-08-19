package com.xiaohezi.rag;

import java.util.List;

public interface EmbeddingModel {
    List<float[]> embedDocuments(List<String> texts);
    float[] embedQuery(String text);
    boolean isConfigured();
}

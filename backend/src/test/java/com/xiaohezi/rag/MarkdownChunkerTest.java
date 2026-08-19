package com.xiaohezi.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownChunkerTest {
    @Test
    void preservesHeadingAndCreatesStableOverlappingChunks() {
        String text = "# 文档\n\n## 第一节\n" + "这是用于测试的句子。".repeat(60);
        MarkdownChunker chunker = new MarkdownChunker(120, 20);

        List<KnowledgeChunk> first = chunker.chunk("doc-1", "文档", text);
        List<KnowledgeChunk> second = chunker.chunk("doc-1", "文档", text);

        assertTrue(first.size() > 2);
        assertEquals("第一节", first.getFirst().section());
        assertEquals(first.stream().map(KnowledgeChunk::id).toList(),
            second.stream().map(KnowledgeChunk::id).toList());
        assertTrue(first.stream().allMatch(chunk -> chunk.content().length() <= 120));
    }

    @Test
    void doesNotCreateBlankChunks() {
        List<KnowledgeChunk> chunks = new MarkdownChunker(100, 10)
            .chunk("doc", "标题", "# 标题\n\n\n## 空章节\n\n## 内容\n有效内容");
        assertEquals(1, chunks.size());
        assertEquals("有效内容", chunks.getFirst().content());
    }
}

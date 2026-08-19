package com.xiaohezi.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class MarkdownChunker {
    private final int targetChars;
    private final int overlapChars;

    public MarkdownChunker(int targetChars, int overlapChars) {
        this.targetChars = targetChars;
        this.overlapChars = overlapChars;
    }

    public List<KnowledgeChunk> chunk(String documentId, String title, String markdown) {
        List<KnowledgeChunk> result = new ArrayList<>();
        String section = title;
        StringBuilder parent = new StringBuilder();
        int index = 0;
        for (String line : markdown.replace("\r", "").split("\n")) {
            if (line.startsWith("#")) {
                index = flush(result, documentId, title, section, parent.toString(), index);
                parent.setLength(0);
                section = line.replaceFirst("^#+\\s*", "").trim();
            } else if (line.isBlank()) {
                if (!parent.isEmpty()) parent.append('\n');
            } else {
                parent.append(line.trim()).append('\n');
                if (parent.length() >= targetChars * 2) {
                    index = flush(result, documentId, title, section, parent.toString(), index);
                    parent.setLength(0);
                }
            }
        }
        flush(result, documentId, title, section, parent.toString(), index);
        return result;
    }

    private int flush(List<KnowledgeChunk> out, String documentId, String title,
                      String section, String text, int index) {
        String clean = text.trim();
        if (clean.isEmpty()) return index;
        int start = 0;
        while (start < clean.length()) {
            int end = Math.min(clean.length(), start + targetChars);
            if (end < clean.length()) {
                int boundary = Math.max(clean.lastIndexOf('。', end), clean.lastIndexOf('\n', end));
                if (boundary > start + targetChars / 2) end = boundary + 1;
            }
            String content = clean.substring(start, end).trim();
            String id = documentId + "-" + hash(section + "|" + index + "|" + content);
            out.add(new KnowledgeChunk(id, documentId, title, section, content, index++, null));
            if (end == clean.length()) break;
            start = Math.max(start + 1, end - overlapChars);
        }
        return index;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.xiaohezi.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RagService {
    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private final EmbeddingModel embeddingModel;
    private final KnowledgePersistenceService persistenceService;
    private volatile List<KnowledgeChunk> chunks;

    public RagService(EmbeddingModel embeddingModel, KnowledgePersistenceService persistenceService) {
        this.embeddingModel = embeddingModel;
        this.persistenceService = persistenceService;
    }

    public SearchResult search(String query, int topK) {
        List<KnowledgeChunk> corpus = corpus();
        float[] queryVector = null;
        if (embeddingModel.isConfigured() && corpus.stream().anyMatch(c -> c.embedding() != null)) {
            try {
                queryVector = embeddingModel.embedQuery(query);
            } catch (RuntimeException e) {
                log.warn("Vector query degraded to lexical retrieval: {}", e.getMessage());
            }
        }

        float[] vector = queryVector;
        Map<String, Integer> vectorRanks = vector == null ? Map.of() : ranks(corpus, c ->
            c.embedding() == null ? 0 : dot(vector, c.embedding()));
        Set<String> queryTerms = bigrams(query);
        Map<String, Integer> lexicalRanks = ranks(corpus, c -> lexical(queryTerms, c));
        List<ScoredChunk> fused = corpus.stream().map(chunk -> {
            double score = rrf(vectorRanks.get(chunk.id())) + rrf(lexicalRanks.get(chunk.id()));
            return new ScoredChunk(chunk, score);
        }).sorted(Comparator.comparingDouble(ScoredChunk::score).reversed()).limit(topK).toList();
        return new SearchResult(fused, queryVector != null);
    }

    public boolean isVectorReady() {
        return corpus().stream().anyMatch(c -> c.embedding() != null);
    }

    private List<KnowledgeChunk> corpus() {
        if (chunks != null) return chunks;
        synchronized (this) {
            if (chunks != null) return chunks;
            List<KnowledgeChunk> loaded = loadResources();
            if (embeddingModel.isConfigured()) {
                try {
                    List<String> texts = loaded.stream()
                        .map(c -> "title: " + c.title() + " / " + c.section() + " | text: " + c.content())
                        .toList();
                    List<float[]> vectors = embeddingModel.embedDocuments(texts);
                    for (int i = 0; i < loaded.size(); i++) loaded.set(i, loaded.get(i).withEmbedding(vectors.get(i)));
                } catch (RuntimeException e) {
                    log.warn("Knowledge indexing degraded to lexical retrieval: {}", e.getMessage());
                }
            }
            persistenceService.replace(loaded);
            chunks = List.copyOf(loaded);
            return chunks;
        }
    }

    private List<KnowledgeChunk> loadResources() {
        try {
            List<KnowledgeChunk> loaded = new ArrayList<>();
            MarkdownChunker chunker = new MarkdownChunker(350, 60);
            Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:knowledge/*.md");
            for (Resource resource : resources) {
                String filename = Objects.requireNonNull(resource.getFilename());
                String documentId = filename.replaceFirst("\\.md$", "");
                String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String title = Arrays.stream(text.split("\n"))
                    .filter(line -> line.startsWith("# ")).findFirst()
                    .map(line -> line.substring(2).trim()).orElse(documentId);
                loaded.addAll(chunker.chunk(documentId, title, text));
            }
            return loaded;
        } catch (Exception e) {
            throw new IllegalStateException("知识文档加载失败", e);
        }
    }

    private Map<String, Integer> ranks(List<KnowledgeChunk> values,
                                       java.util.function.ToDoubleFunction<KnowledgeChunk> scorer) {
        List<KnowledgeChunk> sorted = values.stream()
            .sorted(Comparator.comparingDouble(scorer).reversed()).toList();
        Map<String, Integer> ranks = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) ranks.put(sorted.get(i).id(), i + 1);
        return ranks;
    }

    private double rrf(Integer rank) { return rank == null ? 0 : 1.0 / (60 + rank); }
    private double dot(float[] a, float[] b) {
        double result = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) result += a[i] * b[i];
        return result;
    }
    private double lexical(Set<String> query, KnowledgeChunk chunk) {
        Set<String> titleTerms = bigrams(chunk.title() + chunk.section());
        Set<String> contentTerms = bigrams(chunk.content());
        long titleMatches = query.stream().filter(titleTerms::contains).count();
        long contentMatches = query.stream().filter(contentTerms::contains).count();
        return query.isEmpty() ? 0 : (double) (titleMatches * 3 + contentMatches) / query.size();
    }
    private Set<String> bigrams(String value) {
        String clean = value == null ? "" : value.replaceAll("[\\p{P}\\p{Z}\\s]+", "").toLowerCase();
        Set<String> result = new HashSet<>();
        if (clean.length() == 1) result.add(clean);
        for (int i = 0; i + 1 < clean.length(); i++) result.add(clean.substring(i, i + 2));
        return result;
    }

    public record ScoredChunk(KnowledgeChunk chunk, double score) {}
    public record SearchResult(List<ScoredChunk> chunks, boolean vectorUsed) {
        public String context() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk c = chunks.get(i).chunk();
                result.append("[资料").append(i + 1).append("] ")
                    .append(c.title()).append(" / ").append(c.section()).append("\n")
                    .append(c.content()).append("\n");
            }
            return result.toString();
        }
    }
}

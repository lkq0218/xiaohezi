package com.xiaohezi.rag;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"gemini.enabled=false", "app.rocketmq.enabled=false"})
class RagOfflineEvaluationTest {
    @Autowired RagService rag;

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/rag-evaluation.csv", numLinesToSkip = 1)
    void expectedDocumentMustBeTopOne(String question, String expectedTitle) {
        RagService.SearchResult result = rag.search(question, 3);
        assertEquals(expectedTitle, result.chunks().getFirst().chunk().title(),
            "离线检索门禁失败；Top-1 文档不符合预期");
    }
}

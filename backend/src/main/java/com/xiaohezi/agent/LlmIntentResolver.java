package com.xiaohezi.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohezi.llm.GeminiProperties;
import com.xiaohezi.llm.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.xiaohezi.agent.AgentModels.Intent;

@Primary
@Component
public class LlmIntentResolver implements IntentResolver {
    private static final Logger log = LoggerFactory.getLogger(LlmIntentResolver.class);
    private static final String SYSTEM_PROMPT = """
        你是电力服务智能体的意图分类器。只输出 JSON，不要解释。
        可用意图：
        BILL_QUERY：查询账单金额、欠费或用电量；
        BILL_EXPLANATION：解释账单为什么高、环比同比、费用变化原因；
        BILL_PAYMENT：缴纳或支付电费；
        POWER_KNOWLEDGE：电价、阶梯、峰谷、安全用电等通用知识；
        OUTAGE_REPAIR：停电、故障、触电、冒烟、线路问题或报修；
        HUMAN_HANDOFF：转人工；
        PROGRESS_QUERY：查询缴费或工单进度；
        CHITCHAT：问候或闲聊；
        UNKNOWN：无法判断。
        高风险停电和触电优先 OUTAGE_REPAIR。“电费多少”与“为什么电费高”必须区分。
        输出格式：{"intent":"BILL_QUERY","confidence":0.95}
        confidence 必须是 0 到 1 的数字。
        """;

    private final GeminiClient client;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final IntentClassifier fallback;

    public LlmIntentResolver(GeminiClient client, GeminiProperties properties,
                             ObjectMapper objectMapper,
                             IntentClassifier fallback) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
    }

    @Override
    public IntentClassifier.Result classify(String input, Intent activeIntent) {
        IntentClassifier.Result ruleFirst = fallback.classify(input, activeIntent);
        if (ruleFirst.confidence >= 0.90) return ruleFirst;
        if (!client.isConfigured()) return fallback.classify(input, activeIntent);
        try {
            String userPrompt = "当前输入：" + safe(input)
                + "\n上一意图：" + (activeIntent == null ? "NONE" : activeIntent.name());
            GeminiClient.Completion completion = client.complete(
                SYSTEM_PROMPT, userPrompt, properties.getIntentMaxOutputTokens(), intentSchema());
            JsonNode json = objectMapper.readTree(stripCodeFence(completion.content()));
            Intent intent = Intent.valueOf(json.path("intent").asText("UNKNOWN"));
            double confidence = Math.max(0, Math.min(1, json.path("confidence").asDouble(0.5)));
            return new IntentClassifier.Result(intent, confidence, "LLM", completion.model(),
                completion.promptTokens(), completion.completionTokens());
        } catch (Exception e) {
            log.warn("LLM intent classification failed; using rule fallback: {}", safeMessage(e));
            IntentClassifier.Result rule = fallback.classify(input, activeIntent);
            return new IntentClassifier.Result(rule.intent, rule.confidence, "RULE_FALLBACK",
                null, 0, 0);
        }
    }

    private java.util.Map<String, Object> intentSchema() {
        return java.util.Map.of(
            "type", "object",
            "properties", java.util.Map.of(
                "intent", java.util.Map.of("type", "string", "enum",
                    java.util.Arrays.stream(Intent.values()).map(Enum::name).toList()),
                "confidence", java.util.Map.of("type", "number", "minimum", 0, "maximum", 1)
            ),
            "required", java.util.List.of("intent", "confidence"),
            "additionalProperties", false
        );
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null ? e.getClass().getSimpleName()
            : message.replaceAll("sk-[A-Za-z0-9_-]+", "[REDACTED]");
    }

    private String safe(String input) {
        if (input == null) return "";
        return input.length() > 1000 ? input.substring(0, 1000) : input;
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}


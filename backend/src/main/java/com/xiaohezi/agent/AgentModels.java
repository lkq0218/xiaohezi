package com.xiaohezi.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AgentModels {
    private AgentModels() {}

    public enum Intent {
        BILL_QUERY, BILL_EXPLANATION, BILL_PAYMENT, POWER_KNOWLEDGE, OUTAGE_REPAIR,
        HUMAN_HANDOFF, PROGRESS_QUERY, CHITCHAT, UNKNOWN
    }

    public static class MessageRequest {
        @NotBlank(message = "消息不能为空")
        @Size(max = 1000, message = "消息不得超过1000字")
        public String message;
        @Size(max = 64, message = "客户端请求ID不得超过64字符")
        public String clientRequestId;
    }

    public static class AgentReply {
        public String messageId;
        public String conversationId;
        public String content;
        public Intent intent;
        public double confidence;
        public Card card;
        public List<Citation> citations = new ArrayList<>();
        public List<Evidence> evidence = new ArrayList<>();
        public List<String> limitations = new ArrayList<>();
        public ModelTrace modelTrace;
        public DecisionTrace decisionTrace;

        public static AgentReply of(String conversationId, String content, Intent intent, double confidence) {
            AgentReply reply = new AgentReply();
            reply.messageId = java.util.UUID.randomUUID().toString();
            reply.conversationId = conversationId;
            reply.content = content;
            reply.intent = intent;
            reply.confidence = confidence;
            return reply;
        }
    }

    public static class ModelTrace {
        public String intentSource;
        public String intentModel;
        public String responseSource;
        public String responseModel;
        public int promptTokens;
        public int completionTokens;
        public boolean degraded;
    }

    public static class DecisionTrace {
        public String decisionId;
        public String scene;
        public String decision;
        public List<String> matchedRules = new ArrayList<>();
        public String ruleVersion;
        public String reason;
        public String decidedAt;
        public String executionResult;
    }

    public static class Card {
        public String type;
        public String title;
        public String actionToken;
        public Map<String, Object> fields = new LinkedHashMap<>();
        public List<String> options = new ArrayList<>();

        public static Card type(String type, String title) {
            Card c = new Card();
            c.type = type;
            c.title = title;
            return c;
        }
    }

    public static class Citation {
        public String title;
        public String chunkId;

        public Citation() {}

        public Citation(String title, String chunkId) {
            this.title = title;
            this.chunkId = chunkId;
        }
    }

    public static class Evidence {
        public String id;
        public String label;
        public String value;
        public String observedAt;

        public Evidence() {}

        public Evidence(String id, String label, String value, String observedAt) {
            this.id = id;
            this.label = label;
            this.value = value;
            this.observedAt = observedAt;
        }
    }

    public static class Conversation {
        public String id;
        public String userId;
        public Intent activeIntent;
        public String state = "IDLE";
        public Map<String, String> slots = new LinkedHashMap<>();
        public LocalDateTime updatedAt = LocalDateTime.now();
    }

    public static class Bill {
        public String id;
        public String accountId;
        public String accountMasked;
        public String period;
        public BigDecimal amount;
        public BigDecimal kwh;
        public int billingDays;
        public String status;
        public Map<String, BigDecimal> lineItems = new LinkedHashMap<>();

        public Bill(String id, String period, String amount, String kwh, int billingDays, String status) {
            this.id = id;
            this.accountId = "ACC-001";
            this.accountMasked = "0301****8821";
            this.period = period;
            this.amount = new BigDecimal(amount);
            this.kwh = new BigDecimal(kwh);
            this.billingDays = billingDays;
            this.status = status;
        }

        public Bill lineItem(String type, String amount) {
            lineItems.put(type, new BigDecimal(amount));
            return this;
        }
    }

    public static class ConfirmRequest {
        @Size(max = 64, message = "客户端请求ID不得超过64字符")
        public String clientRequestId;
    }
}

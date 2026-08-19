package com.xiaohezi.agent;

import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.xiaohezi.agent.AgentModels.Intent;

@Component
public class IntentClassifier implements IntentResolver {
    public Result classify(String input, Intent activeIntent) {
        String text = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (contains(text, "转人工", "人工客服", "找客服", "真人")) return new Result(Intent.HUMAN_HANDOFF, .99);
        if (contains(text, "触电", "电线掉", "电线落地", "冒烟", "起火")) return new Result(Intent.OUTAGE_REPAIR, .99);
        if (contains(text, "报修", "停电", "没电", "跳闸", "故障")) return new Result(Intent.OUTAGE_REPAIR, .94);
        if (contains(text, "交电费", "缴费", "付款", "支付电费")) return new Result(Intent.BILL_PAYMENT, .96);
        if ((contains(text, "为什么", "怎么这么高", "高这么多", "贵了", "变高", "增加", "多在哪")
                && contains(text, "电费", "账单"))
            || contains(text, "比上月", "比上个月", "跟上月比", "同比", "去年同期", "是不是阶梯导致")) {
            return new Result(Intent.BILL_EXPLANATION, .97);
        }
        if (contains(text, "查电费", "多少电费", "欠费", "账单", "用电量")
            || (text.contains("查") && text.contains("电费"))) return new Result(Intent.BILL_QUERY, .94);
        if (contains(text, "进度", "处理到哪", "工单状态", "缴费状态")) return new Result(Intent.PROGRESS_QUERY, .90);
        if (contains(text, "电价", "阶梯", "峰谷", "安全用电", "怎么算")) return new Result(Intent.POWER_KNOWLEDGE, .88);
        if (contains(text, "你好", "谢谢", "你是谁")) return new Result(Intent.CHITCHAT, .90);
        if (activeIntent != null && activeIntent != Intent.UNKNOWN) return new Result(activeIntent, .72);
        return new Result(Intent.UNKNOWN, .35);
    }

    private boolean contains(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    public static class Result {
        public final Intent intent;
        public final double confidence;
        public final String source;
        public final String model;
        public final int promptTokens;
        public final int completionTokens;

        public Result(Intent intent, double confidence) {
            this(intent, confidence, "RULE", null, 0, 0);
        }

        public Result(Intent intent, double confidence, String source, String model,
                      int promptTokens, int completionTokens) {
            this.intent = intent;
            this.confidence = confidence;
            this.source = source;
            this.model = model;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }
    }
}

package com.xiaohezi.agent;

import org.junit.jupiter.api.Test;

import static com.xiaohezi.agent.AgentModels.Intent.*;
import static org.assertj.core.api.Assertions.assertThat;

class IntentClassifierTest {
    private final IntentClassifier classifier = new IntentClassifier();

    @Test
    void recognizesCoreIntents() {
        assertThat(classifier.classify("帮我查一下这个月电费", null).intent).isEqualTo(BILL_QUERY);
        assertThat(classifier.classify("我要缴费", null).intent).isEqualTo(BILL_PAYMENT);
        assertThat(classifier.classify("家里突然停电了", null).intent).isEqualTo(OUTAGE_REPAIR);
        assertThat(classifier.classify("给我转人工客服", null).intent).isEqualTo(HUMAN_HANDOFF);
        assertThat(classifier.classify("阶梯电价怎么算", null).intent).isEqualTo(POWER_KNOWLEDGE);
        assertThat(classifier.classify("为什么这个月电费这么高", null).intent).isEqualTo(BILL_EXPLANATION);
        assertThat(classifier.classify("本月账单比上个月多在哪", null).intent).isEqualTo(BILL_EXPLANATION);
    }

    @Test
    void emergencyOutageHasHighConfidence() {
        IntentClassifier.Result result = classifier.classify("路边电线掉地上了", null);
        assertThat(result.intent).isEqualTo(OUTAGE_REPAIR);
        assertThat(result.confidence).isGreaterThan(.95);
    }
}

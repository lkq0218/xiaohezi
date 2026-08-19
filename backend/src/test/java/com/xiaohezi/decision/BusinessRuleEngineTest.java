package com.xiaohezi.decision;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRuleEngineTest {
    private final BusinessRuleEngine engine = new BusinessRuleEngine();

    @Test
    void rejectsDuplicatePaymentWithExplainableRule() {
        RuleDecision decision = engine.decide(DecisionContext.payment(
            "demo", "conv-1", new BigDecimal("128.60"), "PAID"));

        assertThat(decision.decision).isEqualTo(DecisionType.REJECT);
        assertThat(decision.ruleId).isEqualTo("PAYMENT_DUPLICATE_001");
        assertThat(decision.ruleVersion).isEqualTo("v1");
        assertThat(decision.reason).contains("already been paid");
    }

    @Test
    void requiresConfirmationForSimulatedPayment() {
        RuleDecision decision = engine.decide(DecisionContext.payment(
            "demo", "conv-1", new BigDecimal("128.60"), "UNPAID"));

        assertThat(decision.decision).isEqualTo(DecisionType.REQUIRE_CONFIRMATION);
        assertThat(decision.ruleId).isEqualTo("PAYMENT_SIMULATED_CONFIRM_001");
    }

    @Test
    void collectsAddressBeforeCreatingRepairOrder() {
        RuleDecision decision = engine.decide(DecisionContext.outageRepair(
            "demo", "conv-1", false, true));

        assertThat(decision.decision).isEqualTo(DecisionType.COLLECT_REQUIRED_INFO);
        assertThat(decision.ruleId).isEqualTo("OUTAGE_ADDRESS_REQUIRED_001");
    }

    @Test
    void distinguishesHighRiskRepairConfirmation() {
        RuleDecision decision = engine.decide(DecisionContext.outageRepair(
            "demo", "conv-1", true, true));

        assertThat(decision.decision).isEqualTo(DecisionType.REQUIRE_CONFIRMATION);
        assertThat(decision.ruleId).isEqualTo("OUTAGE_HIGH_RISK_CONFIRM_001");
    }
}

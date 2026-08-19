package com.xiaohezi.decision;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BusinessRuleEngine {
    private static final BigDecimal PAYMENT_CONFIRM_THRESHOLD = new BigDecimal("500.00");
    private static final String RULE_VERSION = "v1";

    public RuleDecision decide(DecisionContext context) {
        return switch (context.scene) {
            case PAYMENT -> decidePayment(context);
            case OUTAGE_REPAIR -> decideOutageRepair(context);
        };
    }

    private RuleDecision decidePayment(DecisionContext context) {
        if ("PAID".equals(context.billStatus)) {
            return decision(context, DecisionType.REJECT, "PAYMENT_DUPLICATE_001",
                "Reject duplicate payment", "The current bill has already been paid.");
        }
        if (context.amount != null && context.amount.compareTo(PAYMENT_CONFIRM_THRESHOLD) >= 0) {
            return decision(context, DecisionType.REQUIRE_CONFIRMATION, "PAYMENT_CONFIRM_001",
                "Require confirmation for large payment", "Payment amount is greater than or equal to 500.00.");
        }
        return decision(context, DecisionType.REQUIRE_CONFIRMATION, "PAYMENT_SIMULATED_CONFIRM_001",
            "Require confirmation for simulated payment", "Payment is a sensitive simulated business action.");
    }

    private RuleDecision decideOutageRepair(DecisionContext context) {
        if (!context.hasAddress) {
            return decision(context, DecisionType.COLLECT_REQUIRED_INFO, "OUTAGE_ADDRESS_REQUIRED_001",
                "Collect repair address", "Repair address is required before creating a work order.");
        }
        if (context.highRisk) {
            return decision(context, DecisionType.REQUIRE_CONFIRMATION, "OUTAGE_HIGH_RISK_CONFIRM_001",
                "Require confirmation for high-risk outage", "High-risk outage reports keep the user in control.");
        }
        return decision(context, DecisionType.REQUIRE_CONFIRMATION, "OUTAGE_CONFIRM_001",
            "Require confirmation for outage repair", "Creating a repair work order needs user confirmation.");
    }

    private RuleDecision decision(DecisionContext context, DecisionType type, String ruleId,
                                  String ruleName, String reason) {
        return new RuleDecision(context.scene, type, ruleId, ruleName, RULE_VERSION, reason);
    }
}

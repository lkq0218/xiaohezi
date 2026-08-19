package com.xiaohezi.decision;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RuleDecision {
    public final String decisionId;
    public final BusinessScene scene;
    public final DecisionType decision;
    public final String ruleId;
    public final String ruleName;
    public final String ruleVersion;
    public final String reason;
    public final LocalDateTime decidedAt;

    public RuleDecision(BusinessScene scene, DecisionType decision, String ruleId,
                        String ruleName, String ruleVersion, String reason) {
        this.decisionId = "D-" + UUID.randomUUID();
        this.scene = scene;
        this.decision = decision;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.ruleVersion = ruleVersion;
        this.reason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    public List<String> matchedRules() {
        return List.of(ruleId);
    }
}

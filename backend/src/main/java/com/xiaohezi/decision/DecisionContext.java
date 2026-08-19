package com.xiaohezi.decision;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class DecisionContext {
    public String userId;
    public String conversationId;
    public BusinessScene scene;
    public BigDecimal amount;
    public String billStatus;
    public boolean hasAddress;
    public boolean highRisk;
    public Map<String, Object> attributes = new LinkedHashMap<>();

    public static DecisionContext payment(String userId, String conversationId,
                                          BigDecimal amount, String billStatus) {
        DecisionContext context = new DecisionContext();
        context.userId = userId;
        context.conversationId = conversationId;
        context.scene = BusinessScene.PAYMENT;
        context.amount = amount;
        context.billStatus = billStatus;
        return context;
    }

    public static DecisionContext outageRepair(String userId, String conversationId,
                                               boolean hasAddress, boolean highRisk) {
        DecisionContext context = new DecisionContext();
        context.userId = userId;
        context.conversationId = conversationId;
        context.scene = BusinessScene.OUTAGE_REPAIR;
        context.hasAddress = hasAddress;
        context.highRisk = highRisk;
        return context;
    }
}

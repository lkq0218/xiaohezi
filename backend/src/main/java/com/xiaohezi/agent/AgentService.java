package com.xiaohezi.agent;

import com.xiaohezi.llm.GeminiProperties;
import com.xiaohezi.llm.GeminiClient;
import com.xiaohezi.decision.BusinessRuleEngine;
import com.xiaohezi.decision.DecisionContext;
import com.xiaohezi.decision.DecisionType;
import com.xiaohezi.decision.RuleDecision;
import com.xiaohezi.rag.RagService;
import com.xiaohezi.rag.KnowledgeAnswerCache;
import com.xiaohezi.state.BusinessStateStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.xiaohezi.agent.AgentModels.*;

@Service
public class AgentService {
    private final IntentResolver intentResolver;
    private final GeminiClient llmClient;
    private final GeminiProperties geminiProperties;
    private final RagService ragService;
    private final KnowledgeAnswerCache answerCache;
    private final BusinessStateStore stateStore;
    private final BusinessRuleEngine ruleEngine;
    private final java.util.Map<String, Bill> bills = new java.util.concurrent.ConcurrentHashMap<>();
    private final BillExplanationService billExplanationService;

    public AgentService(IntentResolver intentResolver, BillExplanationService billExplanationService,
                        GeminiClient llmClient, GeminiProperties geminiProperties,
                        RagService ragService, KnowledgeAnswerCache answerCache,
                        BusinessStateStore stateStore, BusinessRuleEngine ruleEngine) {
        this.intentResolver = intentResolver;
        this.billExplanationService = billExplanationService;
        this.llmClient = llmClient;
        this.geminiProperties = geminiProperties;
        this.ragService = ragService;
        this.answerCache = answerCache;
        this.stateStore = stateStore;
        this.ruleEngine = ruleEngine;
        bills.put("2026-07", new Bill("BILL-202607", "2026-07", "128.60", "186.0", 31, "UNPAID")
            .lineItem("电度电费", "112.60")
            .lineItem("附加费用", "12.00")
            .lineItem("调整项", "4.00"));
        bills.put("2026-06", new Bill("BILL-202606", "2026-06", "93.40", "132.0", 30, "PAID")
            .lineItem("电度电费", "82.40")
            .lineItem("附加费用", "11.00")
            .lineItem("调整项", "0.00"));
    }

    public Conversation createConversation(String userId) {
        Conversation c = new Conversation();
        c.id = UUID.randomUUID().toString();
        c.userId = userId;
        return stateStore.save(c);
    }

    @Transactional
    public AgentReply handle(String conversationId, String userId, MessageRequest request) {
        Conversation c = requireConversation(conversationId, userId);
        AgentReply existing = stateStore.reply(userId, request.clientRequestId).orElse(null);
        if (existing != null) return existing;
        IntentClassifier.Result result = intentResolver.classify(request.message, c.activeIntent);
        c.activeIntent = result.intent;
        c.updatedAt = LocalDateTime.now();
        AgentReply reply;
        switch (result.intent) {
            case BILL_QUERY: reply = billQuery(c, result); break;
            case BILL_EXPLANATION: reply = billExplanation(c, result); break;
            case BILL_PAYMENT: reply = payment(c, result); break;
            case POWER_KNOWLEDGE: reply = knowledge(c, request.message, result); break;
            case OUTAGE_REPAIR: reply = repair(c, request.message, result); break;
            case HUMAN_HANDOFF: reply = handoff(c, result); break;
            case PROGRESS_QUERY: reply = progress(c, result); break;
            case CHITCHAT: reply = reply(c, "你好，我是小赫兹。可以帮你查电费、模拟缴费、解答用电问题、停电报修或转人工。", result); break;
            default: reply = unknown(c, result);
        }
        stateStore.save(c);
        return stateStore.remember(userId, request.clientRequestId, reply);
    }

    private AgentReply billQuery(Conversation c, IntentClassifier.Result result) {
        Bill bill = currentBill();
        c.state = "SUCCEEDED";
        AgentReply r = reply(c, "查到了你当前绑定户号的 2026 年 7 月账单。", result);
        Card card = Card.type("bill", "本期电费");
        card.fields.put("户号", bill.accountMasked);
        card.fields.put("账期", bill.period);
        card.fields.put("金额", "¥" + bill.amount);
        card.fields.put("状态", "PAID".equals(bill.status) ? "已缴清" : "待缴费");
        r.card = card;
        return r;
    }

    private AgentReply billExplanation(Conversation c, IntentClassifier.Result result) {
        BillExplanationService.Explanation explanation =
            billExplanationService.explain(currentBill(), bills.get("2026-06"));
        c.state = "SUCCEEDED";
        c.slots.put("period", explanation.current.period);
        c.slots.put("comparisonPeriod", explanation.comparison.period);

        String direction = explanation.amountDelta.signum() >= 0 ? "增加" : "减少";
        AgentReply r = reply(c, String.format(
            "本月电费 %s 元，比上月%s %s 元（%s%%）。主要变化来自用电量相关电费；现有数据不能判断由哪一种具体电器造成。",
            money(explanation.current.amount), direction, money(explanation.amountDelta.abs()),
            explanation.amountDeltaRate.abs().toPlainString()), result);
        Card card = Card.type("bill_explanation", "本月电费为什么高");
        card.fields.put("本月账单", "¥" + money(explanation.current.amount));
        card.fields.put("上月账单", "¥" + money(explanation.comparison.amount));
        card.fields.put("金额变化", signedMoney(explanation.amountDelta)
            + "（" + signedPercent(explanation.amountDeltaRate) + "）");
        card.fields.put("本月用电量", explanation.current.kwh.toPlainString() + " kWh");
        card.fields.put("上月用电量", explanation.comparison.kwh.toPlainString() + " kWh");
        card.fields.put("日均用电量", explanation.currentDailyKwh + " vs "
            + explanation.comparisonDailyKwh + " kWh/天");
        card.fields.put("用量相关贡献", signedMoney(driverAmount(explanation, "CONSUMPTION")));
        card.fields.put("调整项贡献", signedMoney(driverAmount(explanation, "ADJUSTMENT")));
        card.fields.put("附加费用贡献", signedMoney(driverAmount(explanation, "SURCHARGE")));
        card.fields.put("计算差额", "¥" + money(explanation.unexplainedAmount));
        card.options.add("阶梯电价怎么算");
        card.options.add("转人工核查");
        r.card = card;
        r.evidence.add(new Evidence("bill:" + explanation.current.period, "本月账单",
            "¥" + money(explanation.current.amount) + " / " + explanation.current.kwh + " kWh",
            explanation.generatedAt));
        r.evidence.add(new Evidence("bill:" + explanation.comparison.period, "上月账单",
            "¥" + money(explanation.comparison.amount) + " / " + explanation.comparison.kwh + " kWh",
            explanation.generatedAt));
        r.limitations.add("当前演示数据只有月度账单和费用项，无法定位到具体电器。");
        r.limitations.add("当前未接入峰谷曲线和真实阶梯累计数据，不对这两项作确定性归因。");
        return r;
    }

    private AgentReply payment(Conversation c, IntentClassifier.Result result) {
        Bill bill = currentBill();
        RuleDecision decision = ruleEngine.decide(DecisionContext.payment(c.userId, c.id, bill.amount, bill.status));
        if (decision.decision == DecisionType.REJECT) {
            c.state = "SUCCEEDED";
            AgentReply r = reply(c, "本期账单已经缴清，无需重复缴费。", result);
            attachDecisionTrace(r, decision, "REJECTED");
            return r;
        }
        c.state = "READY_TO_CONFIRM";
        String token = UUID.randomUUID().toString();
        stateStore.createAction(token, c.id, c.userId, "PAYMENT", LocalDateTime.now().plusMinutes(5));
        AgentReply r = reply(c, "缴费前请核对账单信息。本系统只进行模拟支付，不会产生真实扣款。", result);
        Card card = Card.type("payment_confirm", "确认模拟缴费");
        card.actionToken = token;
        card.fields.put("户号", bill.accountMasked);
        card.fields.put("账期", bill.period);
        card.fields.put("应付金额", "¥" + bill.amount);
        card.fields.put("有效期", "5 分钟");
        card.fields.put("决策规则", decision.ruleId);
        card.fields.put("决策原因", decision.reason);
        r.card = card;
        attachDecisionTrace(r, decision, "WAITING_CONFIRMATION");
        return r;
    }

    private AgentReply repair(Conversation c, String message, IntentClassifier.Result result) {
        boolean danger = contains(message, "触电", "电线掉", "电线落地", "冒烟", "起火");
        RuleDecision decision = ruleEngine.decide(DecisionContext.outageRepair(
            c.userId, c.id, c.slots.containsKey("address"), danger));
        if (!c.slots.containsKey("address")) {
            c.state = "COLLECTING_SLOTS";
            if (contains(message, "天河", "小区", "路", "街", "号")) c.slots.put("address", message);
            else {
                AgentReply r = reply(c, danger
                    ? "请立即远离危险区域，不要触碰电线或自行处置。确认安全后，请告诉我报修地址。"
                    : "请告诉我停电或故障发生的详细地址。", result);
                r.card = Card.type("options", danger ? "安全优先" : "补充报修信息");
                r.card.options.add("转人工");
                r.card.options.add("取消报修");
                attachDecisionTrace(r, decision, "COLLECTING_REQUIRED_INFO");
                return r;
            }
        }
        decision = ruleEngine.decide(DecisionContext.outageRepair(c.userId, c.id, true, danger));
        c.state = "READY_TO_CONFIRM";
        String token = UUID.randomUUID().toString();
        stateStore.createAction(token, c.id, c.userId, "REPAIR", LocalDateTime.now().plusMinutes(5));
        AgentReply r = reply(c, danger
            ? "已标记为高风险报修。请远离现场并核对地址，确认后创建模拟工单。"
            : "信息已记录，请确认创建模拟报修工单。", result);
        Card card = Card.type("repair_confirm", "确认停电报修");
        card.actionToken = token;
        card.fields.put("地址/描述", c.slots.get("address"));
        card.fields.put("风险等级", danger ? "高" : "普通");
        card.fields.put("决策规则", decision.ruleId);
        card.fields.put("决策原因", decision.reason);
        r.card = card;
        attachDecisionTrace(r, decision, "WAITING_CONFIRMATION");
        return r;
    }

    private AgentReply knowledge(Conversation c, String message, IntentClassifier.Result result) {
        c.state = "SUCCEEDED";
        var cached = answerCache.get(message);
        if (cached.isPresent()) {
            AgentReply hit = reply(c, cached.get().content(), result);
            hit.modelTrace.responseSource = "REDIS_CACHE";
            cached.get().citations().forEach(citation ->
                hit.citations.add(new Citation(citation.title(), citation.chunkId())));
            return hit;
        }
        AgentReply r;
        if (llmClient.isConfigured()) {
            try {
                RagService.SearchResult retrieval = ragService.search(message, 4);
                GeminiClient.Completion completion = llmClient.complete("""
                    你是小赫兹电力服务助手。只能根据提供的资料，用简洁中文回答。
                    资料不足时明确说不知道，不得补造政策数值、来源或用户事实。
                    引用资料时用[资料1]格式。涉及地区政策时提醒以当地当前有效政策为准。
                    涉及触电、冒烟、线路落地时，优先建议远离危险区域并联系紧急或专业渠道。
                    """, "问题：" + message + "\n\n检索资料：\n" + retrieval.context(),
                    geminiProperties.getAnswerMaxOutputTokens());
                r = reply(c, completion.content(), result);
                r.modelTrace.responseSource = "LLM";
                r.modelTrace.responseModel = completion.model();
                r.modelTrace.promptTokens += completion.promptTokens();
                r.modelTrace.completionTokens += completion.completionTokens();
                for (RagService.ScoredChunk hit : retrieval.chunks()) {
                    r.citations.add(new Citation(hit.chunk().title(), hit.chunk().id()));
                }
                answerCache.put(message, new KnowledgeAnswerCache.CachedAnswer(r.content,
                    r.citations.stream().map(citation ->
                        new KnowledgeAnswerCache.CachedCitation(citation.title, citation.chunkId)).toList()));
                r.limitations.add(retrieval.vectorUsed()
                    ? "本次使用 Gemini Embedding + 关键词检索 + RRF 融合。"
                    : "向量服务不可用，本次降级为本地关键词检索。");
            } catch (RuntimeException e) {
                r = reply(c, fallbackKnowledge(message), result);
                r.modelTrace.responseSource = "TEMPLATE_FALLBACK";
                r.modelTrace.degraded = true;
                r.limitations.add("大模型暂不可用，本次使用本地安全模板回答。");
            }
        } else {
            r = reply(c, fallbackKnowledge(message), result);
            r.modelTrace.responseSource = "TEMPLATE";
            r.modelTrace.degraded = true;
            r.limitations.add("未配置 Gemini API Key，本次使用本地安全模板回答。");
        }
        return r;
    }

    private String fallbackKnowledge(String message) {
        return contains(message, "阶梯")
            ? "居民阶梯电价通常按累计用电量分档，具体档位和价格因地区与生效期而异，请以当地当前有效政策为准。"
            : "安全用电应避免湿手接触电器、私拉乱接和超负荷用电。发现设备冒烟、异味或线路落地时，应先远离危险区域并联系专业人员。";
    }

    private AgentReply handoff(Conversation c, IntentClassifier.Result result) {
        c.state = "HANDOFF";
        AgentReply r = reply(c, "已为你进入模拟人工客服队列。当前为演示环境，不会连接真实客服。", result);
        Card card = Card.type("handoff", "人工客服");
        card.fields.put("排队号", "A008");
        card.fields.put("预计等待", "约 2 分钟");
        r.card = card;
        return r;
    }

    private AgentReply progress(Conversation c, IntentClassifier.Result result) {
        AgentReply r = reply(c, "最近一笔模拟业务状态如下。", result);
        Card card = Card.type("receipt", "业务进度");
        stateStore.latestRecord(c.userId).ifPresentOrElse(record -> {
            card.fields.put("业务单号", record.id);
            card.fields.put("业务类型", record.type);
            card.fields.put("状态", record.status);
            card.fields.put("更新时间", record.updatedAt.withNano(0).toString());
        }, () -> card.fields.put("状态", "暂无已提交业务"));
        r.card = card;
        return r;
    }

    private AgentReply unknown(Conversation c, IntentClassifier.Result result) {
        c.state = "IDLE";
        AgentReply r = reply(c, "我还不确定你想办理什么，请选择下面的服务。", result);
        r.card = Card.type("options", "你想办理");
        r.card.options.add("查电费");
        r.card.options.add("交电费");
        r.card.options.add("停电报修");
        r.card.options.add("转人工");
        return r;
    }

    @Transactional
    public AgentReply confirm(String token, String userId, String requestId) {
        AgentReply existing = stateStore.reply(userId, requestId).orElse(null);
        if (existing != null) return existing;
        Bill bill = currentBill();
        BusinessStateStore.CompletedAction action = stateStore.completeAction(
            token, userId, bill.amount, null);
        Conversation c = requireConversation(action.conversationId(), userId);
        c.state = "SUCCEEDED";
        AgentReply r;
        if ("PAYMENT".equals(action.type())) {
            bill.status = "PAID";
            r = AgentReply.of(c.id, "模拟缴费成功，已生成回执。", Intent.BILL_PAYMENT, 1);
            Card card = Card.type("receipt", "模拟缴费回执");
            card.fields.put("业务单号", action.recordId());
            card.fields.put("金额", "¥" + bill.amount);
            card.fields.put("结果", "模拟支付成功");
            r.card = card;
        } else {
            r = AgentReply.of(c.id, "模拟报修工单已创建。", Intent.OUTAGE_REPAIR, 1);
            Card card = Card.type("receipt", "报修受理回执");
            card.fields.put("工单号", action.recordId());
            card.fields.put("状态", "待派单");
            r.card = card;
        }
        stateStore.save(c);
        return stateStore.remember(userId, requestId, r);
    }

    public Bill currentBill() { return bills.get("2026-07"); }

    private BigDecimal driverAmount(BillExplanationService.Explanation explanation, String type) {
        return explanation.drivers.stream()
            .filter(driver -> type.equals(driver.type))
            .map(driver -> driver.amount)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }

    private String money(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String signedMoney(BigDecimal value) {
        return (value.signum() > 0 ? "+¥" : value.signum() < 0 ? "-¥" : "¥")
            + money(value.abs());
    }

    private String signedPercent(BigDecimal value) {
        if (value == null) return "不可计算";
        return (value.signum() > 0 ? "+" : "") + value.toPlainString() + "%";
    }

    private AgentReply reply(Conversation c, String content, IntentClassifier.Result result) {
        AgentReply reply = AgentReply.of(c.id, content, result.intent, result.confidence);
        reply.modelTrace = new ModelTrace();
        reply.modelTrace.intentSource = result.source;
        reply.modelTrace.intentModel = result.model;
        reply.modelTrace.responseSource = "DETERMINISTIC";
        reply.modelTrace.promptTokens = result.promptTokens;
        reply.modelTrace.completionTokens = result.completionTokens;
        reply.modelTrace.degraded = "RULE_FALLBACK".equals(result.source);
        return reply;
    }

    private void attachDecisionTrace(AgentReply reply, RuleDecision decision, String executionResult) {
        DecisionTrace trace = new DecisionTrace();
        trace.decisionId = decision.decisionId;
        trace.scene = decision.scene.name();
        trace.decision = decision.decision.name();
        trace.matchedRules.addAll(decision.matchedRules());
        trace.ruleVersion = decision.ruleVersion;
        trace.reason = decision.reason;
        trace.decidedAt = decision.decidedAt.withNano(0).toString();
        trace.executionResult = executionResult;
        reply.decisionTrace = trace;
    }

    private Conversation requireConversation(String id, String userId) {
        return stateStore.conversation(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
    }

    private boolean contains(String text, String... values) {
        if (text == null) return false;
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

}


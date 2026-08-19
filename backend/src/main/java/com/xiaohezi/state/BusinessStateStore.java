package com.xiaohezi.state;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohezi.agent.AgentModels.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Optional;

@Service
public class BusinessStateStore {
    private final ConversationRepository conversations;
    private final PendingActionRepository actions;
    private final IdempotencyRepository idempotency;
    private final BusinessRecordRepository records;
    private final ObjectMapper mapper;

    public BusinessStateStore(ConversationRepository conversations, PendingActionRepository actions,
                              IdempotencyRepository idempotency, BusinessRecordRepository records,
                              ObjectMapper mapper) {
        this.conversations = conversations; this.actions = actions; this.idempotency = idempotency;
        this.records = records; this.mapper = mapper;
    }

    @Transactional
    public Conversation save(Conversation value) {
        ConversationEntity entity = conversations.findById(value.id).orElseGet(ConversationEntity::new);
        entity.id = value.id; entity.userId = value.userId;
        entity.activeIntent = value.activeIntent == null ? null : value.activeIntent.name();
        entity.state = value.state; entity.updatedAt = value.updatedAt;
        entity.slotsJson = write(value.slots);
        return toDomain(conversations.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> conversation(String id, String userId) {
        return conversations.findById(id).filter(value -> value.userId.equals(userId)).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public Optional<AgentReply> reply(String userId, String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        return idempotency.findByUserIdAndRequestKeyAndExpiresAtAfter(userId, key, LocalDateTime.now())
            .map(value -> read(value.responseJson, AgentReply.class));
    }

    @Transactional
    public AgentReply remember(String userId, String key, AgentReply reply) {
        if (key == null || key.isBlank()) return reply;
        IdempotencyEntity entity = new IdempotencyEntity();
        entity.userId = userId; entity.requestKey = key; entity.responseJson = write(reply);
        entity.createdAt = LocalDateTime.now(); entity.expiresAt = entity.createdAt.plusHours(24);
        try { idempotency.saveAndFlush(entity); }
        catch (DataIntegrityViolationException duplicate) {
            return this.reply(userId, key).orElseThrow(() -> duplicate);
        }
        return reply;
    }

    @Transactional
    public void createAction(String token, String conversationId, String userId, String type,
                             LocalDateTime expiresAt) {
        PendingActionEntity entity = new PendingActionEntity();
        entity.token = token; entity.conversationId = conversationId; entity.userId = userId;
        entity.type = type; entity.expiresAt = expiresAt;
        actions.save(entity);
    }

    @Transactional
    public CompletedAction completeAction(String token, String userId, BigDecimal paymentAmount,
                                          String repairDescription) {
        PendingActionEntity entity = actions.findById(token)
            .filter(value -> value.userId.equals(userId) && !value.consumed
                && value.expiresAt.isAfter(LocalDateTime.now()))
            .orElseThrow(() -> new IllegalArgumentException("确认令牌无效或已过期"));
        entity.consumed = true;
        actions.saveAndFlush(entity);
        BusinessRecordEntity record = new BusinessRecordEntity();
        record.id = ("PAYMENT".equals(entity.type) ? "PAY-" : "REP-")
            + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        record.userId = userId; record.conversationId = entity.conversationId; record.type = entity.type;
        record.status = "PAYMENT".equals(entity.type) ? "SUCCEEDED" : "PENDING_ASSIGNMENT";
        record.amount = "PAYMENT".equals(entity.type) ? paymentAmount : null;
        record.description = "PAYMENT".equals(entity.type) ? null
            : conversations.findById(entity.conversationId)
                .map(value -> read(value.slotsJson,
                    new TypeReference<LinkedHashMap<String, String>>() {}).get("address"))
                .orElse(repairDescription);
        record.createdAt = LocalDateTime.now(); record.updatedAt = record.createdAt;
        records.save(record);
        return new CompletedAction(entity.conversationId, entity.type, record.id);
    }

    @Transactional(readOnly = true)
    public Optional<BusinessRecordEntity> latestRecord(String userId) {
        return records.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    @Scheduled(cron = "${app.state.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredState() {
        LocalDateTime now = LocalDateTime.now();
        idempotency.deleteByExpiresAtBefore(now);
        actions.deleteByExpiresAtBeforeOrConsumedTrue(now);
    }

    @Scheduled(cron = "${app.state.cleanup-cron:0 17 * * * *}")
    @Transactional
    public void purgeExpiredIdempotency() {
        idempotency.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private Conversation toDomain(ConversationEntity entity) {
        Conversation result = new Conversation(); result.id = entity.id; result.userId = entity.userId;
        result.activeIntent = entity.activeIntent == null ? null : Intent.valueOf(entity.activeIntent);
        result.state = entity.state; result.updatedAt = entity.updatedAt;
        result.slots = read(entity.slotsJson, new TypeReference<LinkedHashMap<String, String>>() {});
        return result;
    }
    private String write(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("业务状态序列化失败", e); }
    }
    private <T> T read(String value, Class<T> type) {
        try { return mapper.readValue(value, type); }
        catch (Exception e) { throw new IllegalStateException("业务状态反序列化失败", e); }
    }
    private <T> T read(String value, TypeReference<T> type) {
        try { return mapper.readValue(value, type); }
        catch (Exception e) { throw new IllegalStateException("业务状态反序列化失败", e); }
    }
    public record CompletedAction(String conversationId, String type, String recordId) {}
}

package com.xiaohezi.state;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

interface ConversationRepository extends JpaRepository<ConversationEntity, String> {}
interface PendingActionRepository extends JpaRepository<PendingActionEntity, String> {
    long deleteByExpiresAtBeforeOrConsumedTrue(LocalDateTime now);
}
interface BusinessRecordRepository extends JpaRepository<BusinessRecordEntity, String> {
    Optional<BusinessRecordEntity> findFirstByUserIdOrderByCreatedAtDesc(String userId);
}
interface IdempotencyRepository extends JpaRepository<IdempotencyEntity, Long> {
    Optional<IdempotencyEntity> findByUserIdAndRequestKeyAndExpiresAtAfter(
        String userId, String requestKey, LocalDateTime now);
    long deleteByExpiresAtBefore(LocalDateTime now);
}

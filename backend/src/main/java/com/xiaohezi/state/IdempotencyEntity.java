package com.xiaohezi.state;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_record", uniqueConstraints =
    @UniqueConstraint(name = "uk_idempotency_user_key", columnNames = {"userId", "requestKey"}),
    indexes = @Index(name = "idx_idempotency_expiry", columnList = "expiresAt"))
public class IdempotencyEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 120) public String userId;
    @Column(nullable = false, length = 64) public String requestKey;
    @Column(nullable = false, columnDefinition = "TEXT") public String responseJson;
    @Column(nullable = false) public LocalDateTime createdAt;
    @Column(nullable = false) public LocalDateTime expiresAt;
}

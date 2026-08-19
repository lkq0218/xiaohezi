package com.xiaohezi.state;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_action", indexes = {
    @Index(name = "idx_action_user", columnList = "userId"),
    @Index(name = "idx_action_expiry", columnList = "expiresAt")
})
public class PendingActionEntity {
    @Id @Column(length = 64) public String token;
    @Column(nullable = false, length = 64) public String conversationId;
    @Column(nullable = false, length = 120) public String userId;
    @Column(nullable = false, length = 32) public String type;
    @Column(nullable = false) public LocalDateTime expiresAt;
    @Column(nullable = false) public boolean consumed;
    @Version public long version;
}

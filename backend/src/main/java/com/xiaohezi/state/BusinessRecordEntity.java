package com.xiaohezi.state;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_record", indexes = @Index(name = "idx_business_user_time", columnList = "userId,createdAt"))
public class BusinessRecordEntity {
    @Id @Column(length = 64) public String id;
    @Column(nullable = false, length = 120) public String userId;
    @Column(nullable = false, length = 64) public String conversationId;
    @Column(nullable = false, length = 32) public String type;
    @Column(nullable = false, length = 32) public String status;
    @Column(precision = 12, scale = 2) public BigDecimal amount;
    @Column(length = 500) public String description;
    @Column(nullable = false) public LocalDateTime createdAt;
    @Column(nullable = false) public LocalDateTime updatedAt;
    @Version public long version;
}

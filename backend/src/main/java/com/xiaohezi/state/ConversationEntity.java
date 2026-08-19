package com.xiaohezi.state;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation", indexes = @Index(name = "idx_conversation_user", columnList = "userId,updatedAt"))
public class ConversationEntity {
    @Id public String id;
    @Column(nullable = false, length = 120) public String userId;
    @Column(length = 40) public String activeIntent;
    @Column(nullable = false, length = 40) public String state;
    @Column(nullable = false, columnDefinition = "TEXT") public String slotsJson;
    @Column(nullable = false) public LocalDateTime updatedAt;
    @Version public long version;
}

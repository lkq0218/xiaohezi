package com.xiaohezi.state;

import com.xiaohezi.agent.AgentModels.AgentReply;
import com.xiaohezi.agent.AgentModels.Conversation;
import com.xiaohezi.agent.AgentModels.Intent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"gemini.enabled=false", "app.rocketmq.enabled=false"})
class BusinessStateStoreIntegrationTest {
    @Autowired BusinessStateStore store;

    @Test
    void conversationAndIdempotentReplyCanBeReloadedFromDatabase() {
        Conversation conversation = new Conversation();
        conversation.id = UUID.randomUUID().toString(); conversation.userId = "restart-user";
        conversation.activeIntent = Intent.BILL_QUERY; conversation.slots.put("period", "2026-07");
        store.save(conversation);
        Conversation loaded = store.conversation(conversation.id, "restart-user").orElseThrow();
        assertEquals("2026-07", loaded.slots.get("period"));
        assertTrue(store.conversation(conversation.id, "another-user").isEmpty());
        AgentReply reply = AgentReply.of(conversation.id, "stable", Intent.BILL_QUERY, 1);
        store.remember("restart-user", "same-request", reply);
        assertEquals(reply.messageId,
            store.reply("restart-user", "same-request").orElseThrow().messageId);
    }
}

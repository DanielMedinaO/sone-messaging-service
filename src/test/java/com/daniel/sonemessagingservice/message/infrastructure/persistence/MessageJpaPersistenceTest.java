package com.daniel.sonemessagingservice.message.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageType;
import com.daniel.sonemessagingservice.message.domain.repository.MessagePage;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;

/** Runs against the local PostgreSQL configured for the application; each test rolls back. */
@SpringBootTest
@Transactional
class MessageJpaPersistenceTest {

    @Autowired ConversationRepository conversationRepository;
    @Autowired MessageRepository messageRepository;

    private Conversation newConversation() {
        return conversationRepository.save(Conversation.create(UUID.randomUUID(), UUID.randomUUID()));
    }

    private Message at(Conversation c, String content, String instant) {
        return messageRepository.save(Message.reconstitute(UUID.randomUUID(), c.getId(), c.getCustomerId(),
                MessageType.TEXT, content, Instant.parse(instant)));
    }

    @Test
    void persistsAndFindsByConversation() {
        Conversation c = newConversation();
        Message saved = messageRepository.save(Message.createText(c.getId(), c.getCustomerId(), "Hola"));

        MessagePage page = messageRepository.findByConversationId(c.getId(), 0, 50);

        assertEquals(1, page.totalElements());
        assertEquals(saved.getId(), page.content().get(0).getId());
        assertEquals("Hola", page.content().get(0).getContent());
        assertEquals(MessageType.TEXT, page.content().get(0).getType());
        assertTrue(messageRepository.findByConversationId(newConversation().getId(), 0, 50).content().isEmpty());
    }

    @Test
    void foreignKeyToConversationEnforced() {
        assertThrows(DataIntegrityViolationException.class, () -> messageRepository.save(
                Message.createText(UUID.randomUUID(), UUID.randomUUID(), "orphan")));
    }

    @Test
    void countsByUtcRange_halfOpen() {
        Conversation c = newConversation();
        at(c, "yesterday", "2030-05-09T23:59:59Z");
        at(c, "start", "2030-05-10T00:00:00Z");
        at(c, "end", "2030-05-10T23:59:59Z");
        at(c, "tomorrow", "2030-05-11T00:00:00Z");

        assertEquals(2, messageRepository.countByConversationIdCreatedBetween(c.getId(),
                Instant.parse("2030-05-10T00:00:00Z"), Instant.parse("2030-05-11T00:00:00Z")));
    }

    @Test
    void paginatesNewestFirst() {
        Conversation c = newConversation();
        at(c, "m1", "2030-05-10T10:00:00Z");
        at(c, "m3", "2030-05-10T12:00:00Z");
        at(c, "m2", "2030-05-10T11:00:00Z");

        MessagePage first = messageRepository.findByConversationId(c.getId(), 0, 2);
        MessagePage second = messageRepository.findByConversationId(c.getId(), 1, 2);

        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals("m3", first.content().get(0).getContent());
        assertEquals("m2", first.content().get(1).getContent());
        assertEquals("m1", second.content().get(0).getContent());
    }
}

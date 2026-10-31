package com.daniel.sonemessagingservice.message.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageAttachment;
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

    private MessageAttachment attachment(String name) {
        return MessageAttachment.create(UUID.randomUUID().toString(), name, "image/png", 2048,
                "mock://sone-messaging/" + name, Instant.parse("2030-05-10T10:00:00Z"));
    }

    private Message image(Conversation c, Instant at, String... names) {
        List<MessageAttachment> list = java.util.Arrays.stream(names).map(this::attachment).toList();
        return messageRepository.save(Message.createImage(c.getId(), c.getCustomerId(), list, at));
    }

    @Test
    void persistsImageMessageWithAttachmentsInOrder() {
        Conversation c = newConversation();
        Message saved = image(c, Instant.parse("2030-05-10T10:00:00Z"), "a.png", "b.png", "c.png");
        assertEquals(3, saved.getAttachments().size());

        Message loaded = messageRepository.findByConversationId(c.getId(), 0, 10).content().get(0);
        assertEquals(MessageType.IMAGE, loaded.getType());
        assertNull(loaded.getContent());
        assertEquals(List.of("a.png", "b.png", "c.png"),
                loaded.getAttachments().stream().map(MessageAttachment::getOriginalFileName).toList());
        MessageAttachment first = loaded.getAttachments().get(0);
        assertEquals("image/png", first.getMimeType());
        assertEquals(2048, first.getSizeBytes());
        assertEquals("mock://sone-messaging/a.png", first.getUrl());
        assertEquals(saved.getAttachments().get(0).getId(), first.getId());
    }

    @Test
    void historyMixesTextAndImagesAcrossPagesWithoutLosingAttachments() {
        Conversation c = newConversation();
        at(c, "t1", "2030-05-10T10:00:00Z");
        image(c, Instant.parse("2030-05-10T11:00:00Z"), "a.png", "b.png");
        at(c, "t2", "2030-05-10T12:00:00Z");
        image(c, Instant.parse("2030-05-10T13:00:00Z"), "c.png");

        MessagePage first = messageRepository.findByConversationId(c.getId(), 0, 2);
        MessagePage second = messageRepository.findByConversationId(c.getId(), 1, 2);

        assertEquals(MessageType.IMAGE, first.content().get(0).getType());
        assertEquals(1, first.content().get(0).getAttachments().size());
        assertEquals("t2", first.content().get(1).getContent());
        assertTrue(first.content().get(1).getAttachments().isEmpty());
        assertEquals(2, second.content().get(0).getAttachments().size());
        assertEquals("t1", second.content().get(1).getContent());
    }

    @Test
    void attachmentRequiresExistingMessage() {
        Conversation c = newConversation();
        Message m = Message.createImage(UUID.randomUUID(), c.getCustomerId(), List.of(attachment("x.png")),
                Instant.now());
        assertThrows(DataIntegrityViolationException.class, () -> messageRepository.save(m));
    }
}

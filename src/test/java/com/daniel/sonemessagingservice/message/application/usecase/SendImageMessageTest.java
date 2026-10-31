package com.daniel.sonemessagingservice.message.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;
import com.daniel.sonemessagingservice.message.application.exception.ConversationNotFoundException;
import com.daniel.sonemessagingservice.message.application.exception.DailyMessageLimitExceededException;
import com.daniel.sonemessagingservice.message.application.exception.FileStorageUnavailableException;
import com.daniel.sonemessagingservice.message.application.exception.ImageTooLargeException;
import com.daniel.sonemessagingservice.message.application.exception.InvalidImageUploadException;
import com.daniel.sonemessagingservice.message.application.exception.NotConversationParticipantException;
import com.daniel.sonemessagingservice.message.application.exception.UnsupportedImageTypeException;
import com.daniel.sonemessagingservice.message.application.port.FileStoragePort;
import com.daniel.sonemessagingservice.message.application.port.ImageUpload;
import com.daniel.sonemessagingservice.message.application.port.StoredFile;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageType;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;
import com.daniel.sonemessagingservice.shared.security.model.AppType;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class SendImageMessageTest {

    private static final Instant NOW = Instant.parse("2030-05-10T15:30:00Z");
    private static final Instant DAY_START = Instant.parse("2030-05-10T00:00:00Z");
    private static final Instant NEXT_DAY = Instant.parse("2030-05-11T00:00:00Z");

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock FileStoragePort storage;
    @Mock ApplicationEventPublisher eventPublisher;

    SendImageMessage useCase;
    final UUID customerId = UUID.randomUUID();
    final UUID partnerId = UUID.randomUUID();
    Conversation conversation;

    @BeforeEach
    void setUp() {
        useCase = new SendImageMessage(new ConversationAccess(conversationRepository), conversationRepository,
                messageRepository, storage, Clock.fixed(NOW, ZoneOffset.UTC), eventPublisher);
        conversation = Conversation.create(customerId, partnerId);
    }

    private void found() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
    }

    private void stubOk(long count) {
        found();
        when(messageRepository.countByConversationIdCreatedBetween(conversation.getId(), DAY_START, NEXT_DAY))
                .thenReturn(count);
        when(storage.store(any())).thenAnswer(i -> {
            ImageUpload u = i.getArgument(0);
            String key = UUID.randomUUID().toString();
            return new StoredFile(key, "mock://sone-messaging/" + key, u.originalFileName(), u.mimeType(),
                    u.sizeBytes());
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void stubLimitOnly(long count) {
        found();
        when(messageRepository.countByConversationIdCreatedBetween(any(), any(), any())).thenReturn(count);
    }

    private static ImageUpload img(String name, String type, long size) {
        return new ImageUpload(name, type, size, new byte[] {1});
    }

    private static List<ImageUpload> images(int n) {
        List<ImageUpload> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(img("p" + i + ".jpg", "image/jpeg", 1000));
        }
        return list;
    }

    private AuthenticatedUser customer(UUID id) { return new AuthenticatedUser(id, AppType.CUSTOMER, "ACTIVE"); }

    private AuthenticatedUser partner(UUID id) { return new AuthenticatedUser(id, AppType.PARTNER, "ACTIVE"); }

    @Test
    void customerParticipantCanSend() {
        stubOk(0);
        Message m = useCase.execute(conversation.getId(), customer(customerId), images(1));
        assertEquals(MessageType.IMAGE, m.getType());
        assertEquals(customerId, m.getSenderId());
        assertNull(m.getContent());
        assertEquals(NOW, m.getCreatedAt());
    }

    @Test
    void partnerParticipantCanSend() {
        stubOk(0);
        assertEquals(partnerId, useCase.execute(conversation.getId(), partner(partnerId), images(2)).getSenderId());
    }

    @Test
    void externalUserForbidden() {
        found();
        assertThrows(NotConversationParticipantException.class,
                () -> useCase.execute(conversation.getId(), customer(UUID.randomUUID()), images(1)));
        verify(storage, never()).store(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void conversationNotFound() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ConversationNotFoundException.class, () -> useCase.execute(id, customer(customerId), images(1)));
    }

    @Test
    void invalidFormatRejected() {
        stubLimitOnly(0);
        for (String type : new String[] {"image/gif", "image/svg+xml", "application/pdf", "video/mp4", null}) {
            assertThrows(UnsupportedImageTypeException.class, () -> useCase.execute(conversation.getId(),
                    customer(customerId), List.of(img("a.jpg", type, 10))));
        }
        verify(storage, never()).store(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void allowedFormatsAccepted() {
        stubOk(0);
        List<ImageUpload> list = List.of(img("a", "image/jpeg", 10), img("b", "image/png", 10),
                img("c", "image/webp", 10));
        assertEquals(3, useCase.execute(conversation.getId(), customer(customerId), list).getAttachments().size());
    }

    @Test
    void fileOverFiveMbRejectedButExactlyFiveMbAllowed() {
        stubLimitOnly(0);
        assertThrows(ImageTooLargeException.class, () -> useCase.execute(conversation.getId(), customer(customerId),
                List.of(img("a.jpg", "image/jpeg", 5L * 1024 * 1024 + 1))));
        verify(storage, never()).store(any());
    }

    @Test
    void exactlyFiveMbAllowed() {
        stubOk(0);
        assertDoesNotThrow(() -> useCase.execute(conversation.getId(), customer(customerId),
                List.of(img("a.jpg", "image/jpeg", 5L * 1024 * 1024))));
    }

    @Test
    void moreThanThreeRejected() {
        stubLimitOnly(0);
        assertThrows(InvalidImageUploadException.class,
                () -> useCase.execute(conversation.getId(), customer(customerId), images(4)));
        verify(storage, never()).store(any());
    }

    @Test
    void zeroImagesRejected() {
        stubLimitOnly(0);
        assertThrows(InvalidImageUploadException.class,
                () -> useCase.execute(conversation.getId(), customer(customerId), List.of()));
    }

    @Test
    void allows199() {
        stubOk(199);
        assertDoesNotThrow(() -> useCase.execute(conversation.getId(), customer(customerId), images(1)));
    }

    @Test
    void rejects200() {
        stubLimitOnly(200);
        assertThrows(DailyMessageLimitExceededException.class,
                () -> useCase.execute(conversation.getId(), customer(customerId), images(1)));
        verify(storage, never()).store(any());
        verify(messageRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void threeImagesCountAsOneMessage() {
        stubOk(199);
        Message m = useCase.execute(conversation.getId(), customer(customerId), images(3));
        assertEquals(3, m.getAttachments().size());
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void callsStoragePerImageAndBuildsAttachmentsFromStoredFile() {
        stubOk(0);
        Message m = useCase.execute(conversation.getId(), customer(customerId),
                List.of(img("C:\\fakepath\\one.jpg", "image/jpeg", 111), img("two.png", "image/png", 222)));

        ArgumentCaptor<ImageUpload> c = ArgumentCaptor.forClass(ImageUpload.class);
        verify(storage, times(2)).store(c.capture());
        assertEquals("one.jpg", c.getAllValues().get(0).originalFileName());
        assertEquals(222, c.getAllValues().get(1).sizeBytes());
        assertEquals("one.jpg", m.getAttachments().get(0).getOriginalFileName());
        assertEquals("image/png", m.getAttachments().get(1).getMimeType());
        assertTrue(m.getAttachments().get(0).getUrl().startsWith("mock://sone-messaging/"));
    }

    @Test
    void persistsMessageAndUpdatesConversation() {
        stubOk(0);
        Message m = useCase.execute(conversation.getId(), customer(customerId), images(2));
        verify(messageRepository).save(m);
        verify(conversationRepository).save(conversation);
        assertEquals(m.getCreatedAt(), conversation.getLastMessageAt());
    }

    @Test
    void publishesMessageSentEvent() {
        stubOk(0);
        Message m = useCase.execute(conversation.getId(), partner(partnerId), images(1));
        ArgumentCaptor<MessageSentEvent> c = ArgumentCaptor.forClass(MessageSentEvent.class);
        verify(eventPublisher).publishEvent(c.capture());
        assertSame(m, c.getValue().message());
        assertEquals(customerId, c.getValue().customerId());
        assertEquals(partnerId, c.getValue().partnerId());
    }

    @Test
    void storageFailureDoesNotPersistNorPublish() {
        stubLimitOnly(0);
        when(storage.store(any())).thenThrow(new IllegalStateException("down"));
        assertThrows(FileStorageUnavailableException.class,
                () -> useCase.execute(conversation.getId(), customer(customerId), images(2)));
        verify(messageRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}

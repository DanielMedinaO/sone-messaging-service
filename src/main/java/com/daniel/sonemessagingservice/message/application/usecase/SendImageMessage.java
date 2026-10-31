package com.daniel.sonemessagingservice.message.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import com.daniel.sonemessagingservice.message.application.event.MessageSentEvent;
import com.daniel.sonemessagingservice.message.application.exception.FileStorageUnavailableException;
import com.daniel.sonemessagingservice.message.application.exception.ImageTooLargeException;
import com.daniel.sonemessagingservice.message.application.exception.InvalidImageUploadException;
import com.daniel.sonemessagingservice.message.application.exception.UnsupportedImageTypeException;
import com.daniel.sonemessagingservice.message.application.port.FileStoragePort;
import com.daniel.sonemessagingservice.message.application.port.ImageUpload;
import com.daniel.sonemessagingservice.message.application.port.StoredFile;
import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageAttachment;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;
import com.daniel.sonemessagingservice.shared.security.model.AuthenticatedUser;

@Service
public class SendImageMessage {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ConversationAccess conversationAccess;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final FileStoragePort fileStorage;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public SendImageMessage(ConversationAccess conversationAccess,
                            ConversationRepository conversationRepository,
                            MessageRepository messageRepository,
                            FileStoragePort fileStorage,
                            Clock clock,
                            ApplicationEventPublisher eventPublisher) {
        this.conversationAccess = conversationAccess;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.fileStorage = fileStorage;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Message execute(UUID conversationId, AuthenticatedUser user, List<ImageUpload> images) {
        Conversation conversation = conversationAccess.requireParticipant(conversationId, user);

        Instant now = clock.instant();
        DailyMessageLimit.ensureNotReached(messageRepository, conversationId, now);
        validate(images);

        List<MessageAttachment> attachments = new ArrayList<>();
        for (ImageUpload image : images) {
            StoredFile stored = store(image);
            attachments.add(MessageAttachment.create(stored.storageKey(), stored.originalFileName(),
                    stored.mimeType(), stored.sizeBytes(), stored.url(), now));
        }

        Message message = messageRepository.save(Message.createImage(conversationId, user.id(), attachments, now));
        conversation.registerMessage(message.getCreatedAt());
        conversationRepository.save(conversation);
        eventPublisher.publishEvent(
                new MessageSentEvent(message, conversation.getCustomerId(), conversation.getPartnerId()));
        return message;
    }

    private void validate(List<ImageUpload> images) {
        if (images == null || images.isEmpty()) {
            throw new InvalidImageUploadException("At least one image is required");
        }
        if (images.size() > Message.MAX_ATTACHMENTS) {
            throw new InvalidImageUploadException("At most " + Message.MAX_ATTACHMENTS + " images are allowed");
        }
        for (ImageUpload image : images) {
            if (image == null || image.content() == null || image.sizeBytes() <= 0) {
                throw new InvalidImageUploadException("Images must not be empty");
            }
            if (image.mimeType() == null || !ALLOWED_MIME_TYPES.contains(image.mimeType().toLowerCase())) {
                throw new UnsupportedImageTypeException(
                        "Unsupported image type; allowed: image/jpeg, image/png, image/webp");
            }
            if (image.sizeBytes() > MAX_IMAGE_BYTES) {
                throw new ImageTooLargeException("Each image must not exceed 5 MB");
            }
        }
    }

    private StoredFile store(ImageUpload image) {
        ImageUpload normalized = new ImageUpload(sanitizeFileName(image.originalFileName()),
                image.mimeType().toLowerCase(), image.sizeBytes(), image.content());
        try {
            return fileStorage.store(normalized);
        } catch (FileStorageUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FileStorageUnavailableException("File storage is unavailable", e);
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "image";
        }
        String base = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1).strip();
        if (base.isEmpty()) {
            return "image";
        }
        return base.length() > MAX_FILE_NAME_LENGTH ? base.substring(0, MAX_FILE_NAME_LENGTH) : base;
    }
}

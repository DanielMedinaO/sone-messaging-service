package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.model.MessageAttachment;

@Component
public class MessageJpaMapper {

    public MessageJpaEntity toEntity(Message message) {
        return new MessageJpaEntity(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getType(), message.getContent(), message.getCreatedAt());
    }

    /** Attachment order in the message is stored as {@code position}. */
    public List<MessageAttachmentJpaEntity> toAttachmentEntities(Message message) {
        List<MessageAttachmentJpaEntity> result = new ArrayList<>();
        List<MessageAttachment> attachments = message.getAttachments();
        for (int i = 0; i < attachments.size(); i++) {
            MessageAttachment a = attachments.get(i);
            result.add(new MessageAttachmentJpaEntity(a.getId(), message.getId(), (short) i, a.getStorageKey(),
                    a.getOriginalFileName(), a.getMimeType(), a.getSizeBytes(), a.getUrl(), a.getCreatedAt()));
        }
        return result;
    }

    public Message toDomain(MessageJpaEntity entity, List<MessageAttachmentJpaEntity> attachments) {
        return Message.reconstitute(entity.getId(), entity.getConversationId(), entity.getSenderId(),
                entity.getType(), entity.getContent(), attachments.stream().map(this::toDomain).toList(),
                entity.getCreatedAt());
    }

    private MessageAttachment toDomain(MessageAttachmentJpaEntity e) {
        return MessageAttachment.reconstitute(e.getId(), e.getStorageKey(), e.getOriginalFileName(),
                e.getMimeType(), e.getSizeBytes(), e.getUrl(), e.getCreatedAt());
    }
}

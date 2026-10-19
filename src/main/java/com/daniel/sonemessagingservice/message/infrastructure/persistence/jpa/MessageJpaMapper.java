package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import org.springframework.stereotype.Component;

import com.daniel.sonemessagingservice.message.domain.model.Message;

@Component
public class MessageJpaMapper {

    public MessageJpaEntity toEntity(Message message) {
        return new MessageJpaEntity(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getType(), message.getContent(), message.getCreatedAt());
    }

    public Message toDomain(MessageJpaEntity entity) {
        return Message.reconstitute(entity.getId(), entity.getConversationId(), entity.getSenderId(),
                entity.getType(), entity.getContent(), entity.getCreatedAt());
    }
}

package com.daniel.sonemessagingservice.conversation.infrastructure.persistence.jpa;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationJpaMapper {

    public ConversationJpaEntity toEntity(Conversation conversation) {
        return new ConversationJpaEntity(
                conversation.getId(),
                conversation.getCustomerId(),
                conversation.getPartnerId(),
                conversation.getStatus(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getLastMessageAt());
    }

    public Conversation toDomain(ConversationJpaEntity entity) {
        return Conversation.reconstitute(
                entity.getId(),
                entity.getCustomerId(),
                entity.getPartnerId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastMessageAt());
    }
}

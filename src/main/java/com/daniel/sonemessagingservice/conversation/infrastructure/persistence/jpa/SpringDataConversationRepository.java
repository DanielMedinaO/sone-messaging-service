package com.daniel.sonemessagingservice.conversation.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataConversationRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    Optional<ConversationJpaEntity> findByCustomerIdAndPartnerId(UUID customerId, UUID partnerId);
}

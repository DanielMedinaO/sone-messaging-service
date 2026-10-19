package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMessageRepository extends JpaRepository<MessageJpaEntity, UUID> {

    Page<MessageJpaEntity> findByConversationId(UUID conversationId, Pageable pageable);

    long countByConversationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID conversationId, Instant from, Instant to);
}

package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMessageAttachmentRepository extends JpaRepository<MessageAttachmentJpaEntity, UUID> {

    List<MessageAttachmentJpaEntity> findByMessageIdInOrderByPositionAsc(Collection<UUID> messageIds);
}

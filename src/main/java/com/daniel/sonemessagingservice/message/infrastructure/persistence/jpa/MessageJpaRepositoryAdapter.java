package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.daniel.sonemessagingservice.message.domain.model.Message;
import com.daniel.sonemessagingservice.message.domain.repository.MessagePage;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;

@Repository
public class MessageJpaRepositoryAdapter implements MessageRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final SpringDataMessageRepository springDataRepository;
    private final SpringDataMessageAttachmentRepository attachmentRepository;
    private final MessageJpaMapper mapper;

    public MessageJpaRepositoryAdapter(SpringDataMessageRepository springDataRepository,
                                       SpringDataMessageAttachmentRepository attachmentRepository,
                                       MessageJpaMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.attachmentRepository = attachmentRepository;
        this.mapper = mapper;
    }

    @Override
    public Message save(Message message) {
        MessageJpaEntity saved = springDataRepository.saveAndFlush(mapper.toEntity(message));
        List<MessageAttachmentJpaEntity> attachments = mapper.toAttachmentEntities(message);
        if (!attachments.isEmpty()) {
            attachments = attachmentRepository.saveAllAndFlush(attachments);
        }
        return mapper.toDomain(saved, attachments);
    }

    @Override
    public MessagePage findByConversationId(UUID conversationId, int page, int size) {
        Page<MessageJpaEntity> result = springDataRepository.findByConversationId(
                conversationId, PageRequest.of(page, size, NEWEST_FIRST));
        Map<UUID, List<MessageAttachmentJpaEntity>> attachmentsByMessage = loadAttachments(result.getContent());
        List<Message> messages = result.getContent().stream()
                .map(e -> mapper.toDomain(e, attachmentsByMessage.getOrDefault(e.getId(), List.of())))
                .toList();
        return new MessagePage(messages,
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public long countByConversationIdCreatedBetween(UUID conversationId, Instant from, Instant to) {
        return springDataRepository.countByConversationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                conversationId, from, to);
    }

    /** Single query for the whole page, grouped by message, to avoid N+1. */
    private Map<UUID, List<MessageAttachmentJpaEntity>> loadAttachments(List<MessageJpaEntity> messages) {
        if (messages.isEmpty()) {
            return new HashMap<>();
        }
        List<UUID> ids = messages.stream().map(MessageJpaEntity::getId).toList();
        return attachmentRepository.findByMessageIdInOrderByPositionAsc(ids).stream()
                .collect(Collectors.groupingBy(MessageAttachmentJpaEntity::getMessageId));
    }
}

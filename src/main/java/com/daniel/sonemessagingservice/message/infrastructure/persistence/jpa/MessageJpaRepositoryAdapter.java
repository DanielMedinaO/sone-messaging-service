package com.daniel.sonemessagingservice.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

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
    private final MessageJpaMapper mapper;

    public MessageJpaRepositoryAdapter(SpringDataMessageRepository springDataRepository,
                                       MessageJpaMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Message save(Message message) {
        return mapper.toDomain(springDataRepository.saveAndFlush(mapper.toEntity(message)));
    }

    @Override
    public MessagePage findByConversationId(UUID conversationId, int page, int size) {
        Page<MessageJpaEntity> result = springDataRepository.findByConversationId(
                conversationId, PageRequest.of(page, size, NEWEST_FIRST));
        return new MessagePage(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public long countByConversationIdCreatedBetween(UUID conversationId, Instant from, Instant to) {
        return springDataRepository.countByConversationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                conversationId, from, to);
    }
}

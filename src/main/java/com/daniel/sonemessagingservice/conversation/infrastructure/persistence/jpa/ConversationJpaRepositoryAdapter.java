package com.daniel.sonemessagingservice.conversation.infrastructure.persistence.jpa;

import com.daniel.sonemessagingservice.conversation.domain.model.Conversation;
import com.daniel.sonemessagingservice.conversation.domain.repository.ConversationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationJpaRepositoryAdapter implements ConversationRepository {

    private final SpringDataConversationRepository springDataRepository;
    private final ConversationJpaMapper mapper;

    public ConversationJpaRepositoryAdapter(SpringDataConversationRepository springDataRepository,
                                            ConversationJpaMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity saved = springDataRepository.saveAndFlush(mapper.toEntity(conversation));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Conversation> findByCustomerIdAndPartnerId(UUID customerId, UUID partnerId) {
        return springDataRepository.findByCustomerIdAndPartnerId(customerId, partnerId)
                .map(mapper::toDomain);
    }
}

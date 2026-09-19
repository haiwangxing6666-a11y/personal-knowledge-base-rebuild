package com.ithwx.personalknowledgebase.qa.infrastructure;

import com.ithwx.personalknowledgebase.qa.domain.Conversation;
import com.ithwx.personalknowledgebase.qa.domain.ConversationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class JpaConversationRepositoryAdapter implements ConversationRepository {

    private final SpringDataConversationRepository jpaRepository;

    public JpaConversationRepositoryAdapter(
            SpringDataConversationRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Conversation save(Conversation conversation) {
        ConversationEntity entity = conversation.id() == null
                ? new ConversationEntity()
                : jpaRepository.findById(conversation.id())
                .orElseThrow(() -> new NoSuchElementException(
                        "会话不存在：" + conversation.id()));
        entity.setCreatedAt(conversation.createdAt());
        entity.appendMessages(conversation.messages());
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private Conversation toDomain(ConversationEntity entity) {
        return new Conversation(
                entity.getId(),
                entity.getCreatedAt(),
                entity.getMessages().stream()
                        .map(ChatMessageEntity::toDomain)
                        .toList()
        );
    }
}

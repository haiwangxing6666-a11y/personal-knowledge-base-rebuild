package com.ithwx.personalknowledgebase.qa.infrastructure;

import com.ithwx.personalknowledgebase.qa.domain.ChatMessage;
import com.ithwx.personalknowledgebase.qa.domain.MessageRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_message")
class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean refused;

    @ElementCollection
    @CollectionTable(
            name = "chat_message_source",
            joinColumns = @JoinColumn(name = "message_id")
    )
    @OrderColumn(name = "source_order")
    private List<MessageSourceValue> sources = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    static ChatMessageEntity from(
            ConversationEntity conversation,
            ChatMessage message
    ) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setConversation(conversation);
        entity.setRole(message.role());
        entity.setContent(message.content());
        entity.setRefused(message.refused());
        entity.setSources(new ArrayList<>(message.sources().stream()
                .map(MessageSourceValue::from)
                .toList()));
        entity.setCreatedAt(message.createdAt());
        return entity;
    }

    ChatMessage toDomain() {
        return new ChatMessage(
                role,
                content,
                refused,
                sources.stream().map(MessageSourceValue::toDomain).toList(),
                createdAt
        );
    }
}

package com.ithwx.personalknowledgebase.qa.infrastructure;

import com.ithwx.personalknowledgebase.qa.domain.Conversation;
import com.ithwx.personalknowledgebase.qa.domain.AnswerSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaConversationRepositoryAdapterTest {

    @Mock
    private SpringDataConversationRepository jpaRepository;

    @Test
    void shouldSaveAndRestoreConversationMessages() {
        Conversation conversation = Conversation.start();
        conversation.addUserMessage("什么是 RAG？");
        conversation.addAssistantMessage(
                "RAG 是检索增强生成。",
                false,
                List.of(new AnswerSource(
                        1L, "RAG 笔记", "note", null, List.of(0, 1)))
        );
        when(jpaRepository.saveAndFlush(any(ConversationEntity.class)))
                .thenAnswer(invocation -> {
                    ConversationEntity entity = invocation.getArgument(0);
                    entity.setId(8L);
                    return entity;
                });

        Conversation saved = new JpaConversationRepositoryAdapter(jpaRepository)
                .save(conversation);

        assertEquals(8L, saved.id());
        assertEquals(2, saved.messages().size());
        assertEquals("RAG 是检索增强生成。", saved.messages().get(1).content());
        assertEquals(List.of(0, 1),
                saved.messages().get(1).sources().get(0).chunkIndexes());
    }
}

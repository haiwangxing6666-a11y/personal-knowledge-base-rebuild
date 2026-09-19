package com.ithwx.personalknowledgebase.qa.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataConversationRepository
        extends JpaRepository<ConversationEntity, Long> {
}

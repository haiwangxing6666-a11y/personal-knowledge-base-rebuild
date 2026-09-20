package com.ithwx.personalknowledgebase.qa.domain;

import java.util.Optional;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(Long id);
}

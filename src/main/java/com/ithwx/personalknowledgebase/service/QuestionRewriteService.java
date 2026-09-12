package com.ithwx.personalknowledgebase.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QuestionRewriteService {

    private final ChatModel chatModel;

    public QuestionRewriteService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public Optional<String> rewrite(String question) {
        String prompt = """
                把下面的问题改写为独立、清晰、适合知识库检索的问题。
                不要回答，只输出改写后的问题。

                用户问题：%s
                """.formatted(question);

        try {
            String rewritten = chatModel.call(prompt);
            return rewritten == null || rewritten.isBlank()
                    ? Optional.empty()
                    : Optional.of(rewritten.strip());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}

package com.ithwx.personalknowledgebase.qa.infrastructure;

import com.ithwx.personalknowledgebase.qa.domain.Evidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiliconFlowAnswerGeneratorTest {

    @Mock
    private ChatModel chatModel;

    @Test
    void shouldGenerateAnswerFromEvidence() {
        SiliconFlowAnswerGenerator generator = new SiliconFlowAnswerGenerator(chatModel);
        when(chatModel.call(contains("支持 TXT")))
                .thenReturn(" 支持 TXT。[证据 1] ");

        String answer = generator.generate(
                "支持什么格式？",
                List.of(),
                List.of(new Evidence(
                        1L, "资料", "note", null, 0, "支持 TXT。", 0.9))
        );

        assertEquals("支持 TXT。[证据 1]", answer);
    }
}

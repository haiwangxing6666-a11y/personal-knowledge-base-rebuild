package com.ithwx.personalknowledgebase.practice.infrastructure;

import com.ithwx.personalknowledgebase.practice.domain.GeneratedQuestion;
import com.ithwx.personalknowledgebase.practice.domain.PracticeSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiliconFlowQuestionGeneratorTest {

    @Mock
    private ChatModel chatModel;

    @Test
    void shouldParseQuestionAndReferenceAnswer() {
        when(chatModel.call(anyString())).thenReturn("```json\n{\"question\":\"什么是事务的原子性？\",\"referenceAnswer\":\"事务中的操作要么全部成功，要么全部失败。\"}\n```");
        SiliconFlowQuestionGenerator generator = new SiliconFlowQuestionGenerator(chatModel);

        GeneratedQuestion generated = generator.generate(
                "数据库事务",
                List.of(new PracticeSource(1L, "数据库笔记", "FILE", null, 0, "原子性要求事务不可分割。"))
        );

        assertThat(generated.question()).isEqualTo("什么是事务的原子性？");
        assertThat(generated.referenceAnswer()).contains("全部成功");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue()).contains("数据库事务", "原子性要求事务不可分割", "只能依据");
    }
}

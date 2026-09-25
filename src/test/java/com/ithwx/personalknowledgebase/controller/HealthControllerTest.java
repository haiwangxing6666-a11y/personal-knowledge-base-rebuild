package com.ithwx.personalknowledgebase.controller;

import com.ithwx.personalknowledgebase.controller.dto.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void shouldReturnApplicationHealth() {
        HealthController controller =
                new HealthController("personal-knowledge-base");

        HealthResponse response = controller.health();

        assertEquals("UP", response.status());
        assertEquals("personal-knowledge-base", response.application());
    }
}

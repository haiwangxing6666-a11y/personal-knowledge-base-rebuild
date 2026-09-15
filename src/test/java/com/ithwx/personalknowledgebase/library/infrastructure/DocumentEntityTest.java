package com.ithwx.personalknowledgebase.library.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentEntityTest {

    @Test
    void shouldInitializePersistenceDefaults() {
        DocumentEntity entity = new DocumentEntity();

        entity.initializeDefaults();

        assertNotNull(entity.getUploadTime());
        assertEquals("PENDING", entity.getStatus());
        assertEquals(0, entity.getChunkCount());
    }
}

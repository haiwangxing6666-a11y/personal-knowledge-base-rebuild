package com.ithwx.personalknowledgebase.library.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldSaveAndReadFile() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(temporaryDirectory.toString());
        byte[] content = "正文".getBytes();

        String storedPath = storage.save("笔记.TXT", content);

        assertArrayEquals(content, storage.read(storedPath));
    }

    @Test
    void shouldRejectPathOutsideStorageDirectory() {
        LocalFileStorage storage = new LocalFileStorage(temporaryDirectory.toString());

        assertThrows(IllegalArgumentException.class, () -> storage.read("../secret.txt"));
    }
}

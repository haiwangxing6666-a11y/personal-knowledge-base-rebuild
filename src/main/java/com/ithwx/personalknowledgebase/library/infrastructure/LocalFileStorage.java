package com.ithwx.personalknowledgebase.library.infrastructure;

import com.ithwx.personalknowledgebase.library.application.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path storageDirectory;

    public LocalFileStorage(
            @Value("${app.storage.directory:data/documents}") String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String save(String filename, byte[] content) throws IOException {
        Files.createDirectories(storageDirectory);
        String storedName = UUID.randomUUID() + "." + extensionOf(filename);
        Files.write(storageDirectory.resolve(storedName), content);
        return storedName;
    }

    @Override
    public byte[] read(String storedPath) throws IOException {
        Path path = storageDirectory.resolve(storedPath).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        return Files.readAllBytes(path);
    }

    private String extensionOf(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("无法识别文件格式");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

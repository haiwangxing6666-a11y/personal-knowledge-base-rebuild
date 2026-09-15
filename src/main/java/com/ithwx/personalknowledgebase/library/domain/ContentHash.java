package com.ithwx.personalknowledgebase.library.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record ContentHash(String value) {

    public static ContentHash of(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new ContentHash(HexFormat.of().formatHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8))
            ));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }
}

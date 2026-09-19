package com.ithwx.personalknowledgebase.library.application;

import java.io.IOException;

public interface FileStorage {

    String save(String filename, byte[] content) throws IOException;

    byte[] read(String storedPath) throws IOException;
}

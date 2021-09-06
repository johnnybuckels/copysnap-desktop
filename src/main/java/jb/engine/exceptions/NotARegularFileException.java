package jb.engine.exceptions;

import java.nio.file.Path;

public class NotARegularFileException extends RuntimeException {
    public NotARegularFileException(String message) {
        super(message);
    }

    public NotARegularFileException(Path path) {
        super("Given path is not a regular file: " + path.toString());
    }
}

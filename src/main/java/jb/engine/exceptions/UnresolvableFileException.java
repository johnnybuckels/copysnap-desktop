package jb.engine.exceptions;

import java.nio.file.Path;

public class UnresolvableFileException extends RuntimeException {
    public UnresolvableFileException(String message) {
        super(message);
    }

    public UnresolvableFileException(Path path) {
        super("Could not handle file: " + path.toString());
    }

    public UnresolvableFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

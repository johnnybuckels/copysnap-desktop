package jb.engine.exceptions;

import java.nio.file.Path;

public class NotADirectoryException extends ContextException {
    public NotADirectoryException(String message) {
        super(message);
    }

    public NotADirectoryException(Path path) {
        super("Given path is not a directory: " + path.toString());
    }
}

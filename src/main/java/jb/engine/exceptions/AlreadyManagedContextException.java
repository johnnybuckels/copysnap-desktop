package jb.engine.exceptions;

import java.nio.file.Path;

/**
 * This exception occurs when a context is initialised on a directory that is already managed by CopySnap.
 */
public class AlreadyManagedContextException extends ContextException {
    public AlreadyManagedContextException(String message) {
        super(message);
    }
    public AlreadyManagedContextException(Path path) {
        super("Given path contains a directory that is already managed by CopySnap: " + path.toString());
    }
}

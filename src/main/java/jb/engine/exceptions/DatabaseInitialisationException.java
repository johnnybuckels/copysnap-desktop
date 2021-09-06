package jb.engine.exceptions;

import java.sql.SQLException;

/**
 * Thrown when the creation of the sqlite database fails.
 */
public class DatabaseInitialisationException extends SQLException {

    public DatabaseInitialisationException() {
    }

    public DatabaseInitialisationException(String message) {
        super(message);
    }

    public DatabaseInitialisationException(String message, Throwable cause) {
        super(message, cause);
    }
}

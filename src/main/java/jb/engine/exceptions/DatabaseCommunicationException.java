package jb.engine.exceptions;

import java.sql.SQLException;

/**
 * Thrown when communicating to the sqlite database fails.
 */
public class DatabaseCommunicationException extends SQLException {

    public DatabaseCommunicationException() {
    }

    public DatabaseCommunicationException(String message) {
        super(message);
    }

    public DatabaseCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

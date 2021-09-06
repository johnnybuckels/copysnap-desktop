package jb.engine.core;

import java.nio.file.Path;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;

/**
 * Simple container class with information about a Context object.
 */
public class ContextInfoContainer {
    private final String id;
    private final Path homePath;
    private final String name;
    private final Instant createdTime;
    private final int iterations;

    public ContextInfoContainer(String id, Path homePath, String name, Instant createdTime, int iterations) {
        this.id = id;
        this.homePath = homePath;
        this.name = name;
        this.createdTime = createdTime;
        this.iterations = iterations;
    }

    public String getId() {
        return id;
    }

    public Path getHomePath() {
        return homePath;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public int getIterations() {
        return iterations;
    }

    @Override
    public String toString() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(createdTime)) + "; " + iterations + "; " + homePath;
    }
}

package jb.engine.core;

import jb.engine.core.data.CopyType;
import jb.engine.core.data.DataField;
import jb.engine.core.data.SQLiteConstraint;
import jb.engine.core.data.SQLiteType;
import jb.engine.services.HashService;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;

/**
 * Container for Information about one specific snapshot that has been created by CopySnap.
 */
public class SnapshotInfo implements Comparable<SnapshotInfo> {

    /**
     * Database column name of the field {@link #associatedContextId}
     */
    public static final String CONTEXT_IDENTIFYING_SNAPSHOT_COLUMN_NAME = "associated_context_id";

    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.NOT_NULL}, constructorArgumentPositionIndex = 0)
    private final String associatedContextId;
    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.NOT_NULL}, constructorArgumentPositionIndex = 1)
    private String name;
    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.NOT_NULL, SQLiteConstraint.UNIQUE}, constructorArgumentPositionIndex = 2)
    private final Path runTargetDirectory;
    @DataField(sqliteType = SQLiteType.BLOB, sqliteConstraints = {SQLiteConstraint.NOT_NULL}, constructorArgumentPositionIndex = 3)
    private final HashMap<String, byte[]> redirectedChecksumMap;
    @DataField(sqliteType = SQLiteType.INTEGER, constructorArgumentPositionIndex = 4)
    private final CopyType copyType;
    @DataField(sqliteType = SQLiteType.INTEGER, sqliteConstraints = {SQLiteConstraint.NOT_NULL}, constructorArgumentPositionIndex = 5)
    private final Instant createdTime;
    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.PRIMARY_KEY}, constructorArgumentPositionIndex = 6)
    private final String id;

    protected SnapshotInfo(String associatedContextId, String name, Path runTargetDirectory, HashMap<Path, ByteBuffer> redirectedChecksumMap, CopyType copyType, Instant createdTime, String id) {
        this.associatedContextId = associatedContextId;
        this.name = name;
        this.runTargetDirectory = runTargetDirectory;
        this.redirectedChecksumMap = HashService.toSerializableChecksumMap(redirectedChecksumMap);
        this.copyType = copyType;
        this.createdTime = createdTime;
        this.id = id;
    }

    /**
     * @return the key-value inverted checksum map of this snapshot where every {@code byte[]} was wrapped in a {@link ByteBuffer} and
     * String values were mapped to {@link Path} objects.
     */
    public HashMap<ByteBuffer, Path> getInverseChecksumMap() {
        return HashService.invertHashMap(HashService.toCopySnapInternalChecksumMap(redirectedChecksumMap));
    }

    /**
     * Returns the serializable version of this snapshot's checksum map.
     */
    public HashMap<String, byte[]> getRedirectedChecksumMap() {
        return redirectedChecksumMap;
    }

    // Getter

    public String getAssociatedContextId() {
        return associatedContextId;
    }

    public String getName() {
        return name;
    }

    public Path getRunTargetDirectory() {
        return runTargetDirectory;
    }

    public CopyType getCopyType() {
        return copyType;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Overridden

    @Override
    public int compareTo(SnapshotInfo o) {
        return createdTime.compareTo(o.createdTime);
    }
}

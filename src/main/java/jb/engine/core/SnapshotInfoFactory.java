package jb.engine.core;

import jb.engine.core.data.CopyType;
import jb.engine.core.data.DatabaseManager;
import jb.engine.exceptions.ObjectCreatorException;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;

public class SnapshotInfoFactory implements ObjectCreator<SnapshotInfo> {

    public static SnapshotInfo createNew(String associatedContextId, String name, Path runTargetDirectory, HashMap<Path, ByteBuffer> redirectedChecksumMap, CopyType copyType) {
        return new SnapshotInfo(associatedContextId, name, runTargetDirectory, redirectedChecksumMap, copyType, Instant.now(), DatabaseManager.getNewIdValue());
    }

    @Override
    public SnapshotInfo createFromArgs(Object[] args) throws ObjectCreatorException {
        Class<?>[] requiredTypes = {String.class, String.class, Path.class, HashMap.class, CopyType.class, Instant.class, String.class};
        if(args.length != requiredTypes.length) {
            throw new ObjectCreatorException("Could not create SnapshotInfo instance: got " + args.length + "arguments but expected " + requiredTypes.length);
        }
        // check types
        for(int i = 0; i < requiredTypes.length; i++) {
            Object arg = args[i];
            if(arg == null) {
                throw new ObjectCreatorException("Could not create SnapshotInfo instance: the given required argument at position " + i + " was null");
            } else if(!requiredTypes[i].isAssignableFrom(args[i].getClass())) {
                throw new ObjectCreatorException("Could not create SnapshotInfo instance: the argument at position " + i + " of type " + arg.getClass() + " can not be assigned to the required type " + requiredTypes[i]);
            }
        }
        try {
            return new SnapshotInfo(
                    (String) args[0],
                    (String) args[1],
                    (Path) args[2],
                    (HashMap<Path, ByteBuffer>) args[3],
                    (CopyType) args[4],
                    (Instant) args[5],
                    (String) args[6]
            );
        } catch (ClassCastException e) {
            throw new ObjectCreatorException("Could not create SnapshotInfo instance: Error while calling constructor: " + e, e);
        }
    }

}

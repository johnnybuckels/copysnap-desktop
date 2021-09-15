package jb.engine.core;

import jb.engine.core.data.DataField;
import jb.engine.core.data.SQLiteConstraint;
import jb.engine.core.data.SQLiteType;
import jb.engine.exceptions.ObjectCreatorException;

/**
 * Class representing the latest state CopySnap was used with
 */
public class LatestState {

    @DataField(columnName = "state_id", sqliteType = SQLiteType.INTEGER, sqliteConstraints = {SQLiteConstraint.PRIMARY_KEY})
    public static final Integer STATE_ID = 1;

    @DataField(sqliteType = SQLiteType.TEXT, constructorArgumentPositionIndex = 0)
    private final String lastUsedContextId;

    public LatestState(String lastUsedContextId) {
        this.lastUsedContextId = lastUsedContextId;
    }

    public static LatestState createFromArgs(Object[] args) throws ObjectCreatorException {
        if(args.length != 1) {
            throw new ObjectCreatorException("Can not create latest state: expected argument list of size 1 but received " + args.length + " arguments");
        }
        return new LatestState((String) args[0]);
    }

    public String getLastUsedContextId() {
        return lastUsedContextId;
    }
}

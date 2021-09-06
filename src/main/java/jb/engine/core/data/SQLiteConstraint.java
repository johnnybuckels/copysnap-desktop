package jb.engine.core.data;

public enum SQLiteConstraint {
    NOT_NULL("NOT NULL"),
    UNIQUE("UNIQUE"),
    PRIMARY_KEY("PRIMARY KEY")
    ;

    private final String constraint;

    SQLiteConstraint(String constraint) {
        this.constraint = constraint;
    }

    public String getConstraint() {
        return constraint;
    }
}

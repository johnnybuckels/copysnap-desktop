package jb.engine.core.data;

import java.sql.Types;

public enum SQLiteType {
    TEXT("TEXT", Types.VARCHAR),
    BLOB("BLOB", Types.BLOB),
    INTEGER("INTEGER", Types.INTEGER)
    ;
    private final String type;
    private final int typeJavaSql;

    SQLiteType(String sqliteType, int typeJavaSql) {
        this.type = sqliteType;
        this.typeJavaSql = typeJavaSql;
    }

    public String getType() {
        return type;
    }

    public int getJavaSqlType() {
        return typeJavaSql;
    }
}

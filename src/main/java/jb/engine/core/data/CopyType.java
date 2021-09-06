package jb.engine.core.data;

public enum CopyType {
    PLAIN_COPY("Plain Copy"),
    SNAPSHOT("Snapshot"),
    RESTORED("Restored")
    ;

    private final String name;

    CopyType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

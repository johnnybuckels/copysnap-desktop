package jb.gui.filetree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileTreeItem extends DefaultMutableTreeNode{

    private final static int MAX_FILE_COUNT = 1000;

    private final Path location;
    private final boolean isDirectory;
    private final boolean isUnchanged;

    private final boolean isDummy;

    public FileTreeItem(Path location) {
        this(location, false);
    }

    private FileTreeItem(Path location, boolean isDummy) {
        if(location == null) {
            throw new IllegalArgumentException("FileTreeItem location can not be null");
        }
        this.isDummy = isDummy;
        this.location = location;
        this.isDirectory = Files.isDirectory(location);
        setAllowsChildren(isDirectory);
        this.isUnchanged = Files.isSymbolicLink(location);
        if(isDirectory) {
            extendSelfWithOwnChildren();
        }
    }

    /**
     * Constructs a tree path from the given path consisting of respective {@link FileTreeItem}-Objects.
     * @param path path to create TreePath from
     * @param truncateFirstN number of indexes to truncate starting from index 0.
     *                  This is useful when creating a TreePath for some displayed JTree with a root item that is contained further down iin the given path.
     */
    public static TreePath getTreePathOfFileTreeItem(Path path, int truncateFirstN) {
        FileTreeItem[] fileTreeItems = new FileTreeItem[Integer.max(0, path.getNameCount()-truncateFirstN)];
        Path currentPath = path;
        while(currentPath != null && currentPath.getNameCount() > truncateFirstN) {
            fileTreeItems[currentPath.getNameCount()-truncateFirstN-1] = new FileTreeItem(currentPath);
            currentPath = currentPath.getParent();
        }
        return new TreePath(fileTreeItems);
    }
    /**
     * Constructs a tree path from the given path consisting of respective {@link FileTreeItem}-Objects.
     */
    public static TreePath getTreePathOfFileTreeItem(Path path) {
        return getTreePathOfFileTreeItem(path, 0);
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public Path getIOPath() {
        return location;
    }

    public boolean isUnchanged() {
        return isUnchanged;
    }

    public boolean isDummy() {
        return isDummy;
    }

    // ----- helper methods
    /**
     * Given a FileTreeItem, this method lists all first level child-paths on this file system and ads them to the given node
     * as children.
     * <p>This method does nothing, if this FileTreeItem is not a directory or already possesses any children.</p>
     */
    private void extendSelfWithOwnChildren() {
        if(isDummy || !isDirectory || getChildCount() > 0) {
            // this FileTreeItem is not a directory or it was already extended since it contains children.
            return;
        }
        List<Path> childPaths;
        try {
            childPaths = Files.list(location).limit(MAX_FILE_COUNT).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Could not extends this file tree item with its child paths: %s", this.location), e);
        }
        childPaths.stream().sorted((t, o) -> comparePaths(t, o, false)).forEach(path -> this.add(new FileTreeItem(path)));
        if(childPaths.size() >= MAX_FILE_COUNT) {
            this.add(new FileTreeItem(this.location, true));
        }
    }

    @Override
    public String toString() {
        if(isDummy) {
            return "...";
        } else {
            return location.getFileName().toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTreeItem that = (FileTreeItem) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    public int comparePaths(Path t, Path o, boolean dirFirst) {
        int a = dirFirst ? -1 : 1;
        if(Files.isDirectory(t) == Files.isDirectory(o)) {
            return t.toString().compareTo(o.toString());
        } else if (Files.isDirectory(t)) {
            return a;
        } else {
            return -a;
        }
    }
}

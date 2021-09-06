package jb.gui.components;

import jb.gui.filetree.FileTreeItem;
import jb.gui.utils.TreeUtils;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class CopySnapTreeContainer {

    private final PathSelectionBar monitoredPathBar;

    private final JTree tree;
    private final FileTreeItem rootItem;

    private BiConsumer<Path, Path> selectionReceiver;
    private boolean enableReceiverConnection = true;

    public CopySnapTreeContainer(Path rootTreePath, PathSelectionBar monitoredPathBar) {
        this.monitoredPathBar = monitoredPathBar;
        this.monitoredPathBar.addPathConsumer(this::selectNodeInThisTree);
        this.rootItem = new FileTreeItem(rootTreePath);
        this.tree = new JTree(rootItem);
        this.tree.setExpandsSelectedPaths(true);
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.setExpandsSelectedPaths(true);
        // add custom objects to tree
        this.tree.setExpandsSelectedPaths(true);
        this.tree.setCellRenderer(new TreeUtils.CopySnapFileTreeRenderer());
        this.tree.addTreeSelectionListener(
                // set path to registered path bar
                e -> this.monitoredPathBar.validateAndSetTextFieldValue(((FileTreeItem) e.getPath().getLastPathComponent()).getIOPath())
        );
        this.tree.addMouseListener(new TreeUtils.CopySnapFileTreeMouseListener(tree, this::selectNode));
        this.tree.addKeyListener(new TreeUtils.CopySnapFileTreeKeyboardListener(tree, this::selectNode));
    }

    /**
     * Register a method that is called whenever a node from this tree is selected.
     * <p>
     *     The given BiConsumer will be called with the file system location of the selected {@link FileTreeItem} and this containers root path.
     * </p>
     * @param selectionReceiver the method consuming the selected Nodes file system location and this containers root path.
     */
    public void setSelectionReceiver(BiConsumer<Path, Path> selectionReceiver) {
       this.selectionReceiver = selectionReceiver;
    }

    public void setEnableReceiverConnection(boolean enableReceiverConnection) {
        this.enableReceiverConnection = enableReceiverConnection;
    }

    public boolean isEnableReceiverConnection() {
        return enableReceiverConnection;
    }

    public PathSelectionBar getMonitoredPathBar() {
        return monitoredPathBar;
    }

    public JTree getTree() {
        return tree;
    }

    public FileTreeItem getRootItem() {
        return rootItem;
    }

    /**
     * Given a Path, this method tries to select the corresponding Node in this containers tree by replacing the paths root by this
     * containers root path and then calling {@link #selectNodeInThisTree(Path)}.
     * <p>
     *     Consider this containers root path a/b.<br>
     *     The given pathToSelect is x/y/c/d and the given rootPathToRedirect is x/y.<br>
     *     This method will try to select the node at a/b/c/d.
     * </p>
     * @param pathToSelect the original path
     * @param rootPathToRedirect the original root path that should be replaced by this containers root path.
     */
    public void selectNodeRelativeToThisTree(Path pathToSelect, Path rootPathToRedirect) {
        if(!pathToSelect.startsWith(rootPathToRedirect)) {
            throw new IllegalArgumentException(String.format("Given path %s does not start with given root path %s", pathToSelect, rootPathToRedirect));
        }
        Path relativePathToSelect = rootPathToRedirect.relativize(pathToSelect);
        Path pathToSelectInThisTree = rootItem.getIOPath().resolve(relativePathToSelect);
        selectNodeInThisTree(pathToSelectInThisTree);
    }

    /**
     * Tries to select the TreePath corresponding to the given IO-Path object.
     * This only works if the given path starts with this containers root path.
     * @param path the path to select in this tree container.
     */
    public void selectNodeInThisTree(Path path) {
        if(!path.startsWith(rootItem.getIOPath())) {
            return;
        }
        // TreePath object of path
        int lengthToTruncate = rootItem.getIOPath().getNameCount() - 1;  // length to cut without cutting the rootItem
        TreePath pathToSelect = FileTreeItem.getTreePathOfFileTreeItem(path, lengthToTruncate);
        // truncate TreePath to match this containers root node
        selectNode(pathToSelect);
    }

    private void selectNode(TreePath path) {
        TreeUtils.collapseOrExpandNode(path, this.tree);
        tree.setSelectionPath(path);
        tree.setLeadSelectionPath(path);
        monitoredPathBar.validateAndSetTextFieldValue(((FileTreeItem) path.getLastPathComponent()).getIOPath());
        if(enableReceiverConnection && selectionReceiver != null) {
            selectionReceiver.accept(((FileTreeItem) path.getLastPathComponent()).getIOPath(), this.rootItem.getIOPath());
        }
    }

}

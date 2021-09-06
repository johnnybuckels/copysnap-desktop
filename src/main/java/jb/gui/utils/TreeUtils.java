package jb.gui.utils;

import jb.gui.filetree.FileTreeItem;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Class providing static classes and methods for JTrees.
 */
public class TreeUtils {

    /**
     * Class for rendering the actual file tree.
     */
    public static class CopySnapFileTreeRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof FileTreeItem) {
                FileTreeItem item = (FileTreeItem) value;
                // set icon
                if(item.isDummy()) {
                    setIcon(null);
                } else if (item.isDirectory()) {
                    setIcon(UIManager.getIcon("Tree.openIcon"));
                } else {
                    setIcon(UIManager.getIcon("Tree.leafIcon"));
                }
                // set color
                if(item.isUnchanged()) {
                    setForeground(Color.LIGHT_GRAY);
                }
            }
            return this;
        }

    }

    public static class CopySnapFileTreeMouseListener implements MouseListener {

        private final JTree monitoredTree;
        private final Consumer<TreePath> treePathConsumer;


        public CopySnapFileTreeMouseListener(JTree monitoredTree, Consumer<TreePath> treePathConsumer) {
            if(monitoredTree.getSelectionModel().getSelectionMode() != TreeSelectionModel.SINGLE_TREE_SELECTION) {
                throw new IllegalArgumentException("Tree model needs to have set its selection mode to TreeSelectionModel.SINGLE_TREE_SELECTION");
            }
            this.monitoredTree = monitoredTree;
            this.treePathConsumer = treePathConsumer;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            TreePath clickedPath = monitoredTree.getPathForLocation(e.getX(), e.getY());
            if(clickedPath == null) {
                return;
            }
            if(e.getClickCount() == 1 && e.getButton() == 1) {
                treePathConsumer.accept(clickedPath);
            } else if(e.getClickCount() == 1 && e.getButton() == 2) {
                treePathConsumer.accept(clickedPath);
                if(Desktop.isDesktopSupported()) {
                    Path pathToLaunch = ((FileTreeItem)clickedPath.getLastPathComponent()).getIOPath().normalize();
                    if(Files.isRegularFile(pathToLaunch)) {
                        pathToLaunch = pathToLaunch.getParent();
                    }
                    try {
                        Desktop.getDesktop().open(pathToLaunch.toFile());
                    } catch (IOException ioException) {
                        throw new UncheckedIOException("Could not show file in browser: " + ioException, ioException);
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}
    }

    public static class CopySnapFileTreeKeyboardListener implements KeyListener {

        private final JTree monitoredTree;
        private final Consumer<TreePath> treePathConsumer;


        public CopySnapFileTreeKeyboardListener(JTree monitoredTree, Consumer<TreePath> treePathConsumer) {
            if(monitoredTree.getSelectionModel().getSelectionMode() != TreeSelectionModel.SINGLE_TREE_SELECTION) {
                throw new IllegalArgumentException("Tree model needs to have set its selection mode to TreeSelectionModel.SINGLE_TREE_SELECTION");
            }
            this.treePathConsumer = treePathConsumer;
            this.monitoredTree = monitoredTree;
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getSource() == monitoredTree && e.getKeyCode() == KeyEvent.VK_ENTER) {
                TreePath currentSelectedNode = monitoredTree.getSelectionPath();
                if(currentSelectedNode != null) {
                    treePathConsumer.accept(currentSelectedNode);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {}

    }

    /**
     * Collapses or expands the given Tree along the given path.
     */
    public static void collapseOrExpandNode(TreePath pathToNodeToExpand, JTree containingTree) {
        if (pathToNodeToExpand == null || pathToNodeToExpand.getPathCount() < 2) {
            return;
        }
        if (containingTree.isExpanded(pathToNodeToExpand)) {
            containingTree.collapsePath(pathToNodeToExpand);
        } else {
            // expand all subpaths in tree
            Object[] pathElements = pathToNodeToExpand.getPath();
            for(int i = 0; i < pathToNodeToExpand.getPathCount(); i++) {
                TreePath currentPath = new TreePath(Arrays.copyOfRange(pathElements, 0, i+1));
                containingTree.expandPath(currentPath);
                containingTree.revalidate();
            }
        }
    }

}

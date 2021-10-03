package jb.gui.components;

import jb.engine.core.Context;
import jb.engine.core.SnapshotInfo;
import jb.engine.services.CopyService;
import jb.gui.constants.CopySnapGeometry;
import jb.gui.exceptions.CopySnapException;
import jb.gui.exceptions.InvalidPathSelectionException;
import jb.gui.exceptions.RefreshDisplayException;
import jb.gui.filetree.FileTreeItem;
import jb.gui.utils.LayoutUtils;
import jb.gui.utils.MessageUtils;
import jb.gui.worker.BackgroundWorker;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CopySnapDisplay extends JPanel {

    private final PathSelectionBar toBar;
    private final PathSelectionBar fromBar;
    private final JButton buttonRestore;
    private final JScrollPane fromScrollPane = new JScrollPane();
    private final JScrollPane toScrollPane = new JScrollPane();

    private Context currentContext = null;
    private SnapshotInfo currentSnapshotInfo = null;
    private CopySnapTreeContainer fromTreeContainer = null;
    private CopySnapTreeContainer toTreeContainer = null;

    public CopySnapDisplay() {
        super(new GridBagLayout());
        this.fromBar = new PathSelectionBar("From", 0.05);
        this.toBar = new PathSelectionBar("To",0.05);
        this.buttonRestore = new CopySnapButton("Restore");
        this.buttonRestore.addActionListener(action -> this.restore());
        buttonRestore.setPreferredSize(new Dimension(60, CopySnapGeometry.BUTTON_HEIGHT));
        arrangeContents();
    }

    private void arrangeContents() {
        // ----- from and to bar and buttons
        JPanel topPanel = new JPanel(new GridBagLayout());
        // from bar
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.9;
        c.insets = new Insets(0, 0, 10, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(fromBar, c);

        // to bar
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.9;
        c.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(toBar, c);

        // button restore
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.05;
        c.gridheight = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 10, 10, 10);
        topPanel.add(buttonRestore, c);

        // ----- tree views
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        // scroll pane from
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, 0, 5);
        fromScrollPane.getViewport().setBackground(Color.WHITE);
        fromScrollPane.setBackground(Color.WHITE);
        LayoutUtils.makeScrollbarsThin(fromScrollPane);
        JPanel fromPane = new PanelWithScrollPanel(fromScrollPane);
        bottomPanel.add(fromPane, c);

        // scroll pane to
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.5;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 5, 0, 0);
        toScrollPane.getViewport().setBackground(Color.WHITE);
        toScrollPane.setBackground(Color.WHITE);
        LayoutUtils.makeScrollbarsThin(toScrollPane);
        JPanel toPane = new PanelWithScrollPanel(toScrollPane);
        bottomPanel.add(toPane, c);

        // ---------- insert panels
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(20, 10, 0, 20);
        this.add(topPanel, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 10, 20, 20);
        this.add(bottomPanel, c);
        updateScrollPaneBorders();
    }

    /**
     * Method for receiving a newly loaded context or snapshot info in order to update this displays filetrees accordingly.
     */
    public void retrieveContextAndSnapshotInfo(Context newContext, SnapshotInfo newSnapshotInfo) {
        this.currentSnapshotInfo = newSnapshotInfo;
        this.currentContext = newContext;
        refreshDirectoryTreeDisplays();
    }

    /**
     * Issues a rebuild of the tree displays using the currently loaded context and snapshotinfo.
     */
    private void refreshDirectoryTreeDisplays() {
        if(currentContext == null || currentSnapshotInfo ==  null) {
            return;
        }
        try {
            // get from root inside the run's target directory
            Path actualFromDir = currentSnapshotInfo.getRunTargetDirectory().resolve(currentContext.getSourcePath().getFileName());
            fromBar.setFixRootPath(actualFromDir);
            toBar.setFixRootPath(currentContext.getSourcePath());
            displayDirectoryTrees(actualFromDir, currentContext.getSourcePath());
        } catch (InvalidPathSelectionException | IOException e) {
            throw new RefreshDisplayException("Could not refresh the directory tree display: " + e, e);
        }
    }

    /**
     * Initialize two file trees within this display using the given from and to path as the basic paths
     * of the respective trees.
     */
    private void displayDirectoryTrees(Path fromPath, Path toPath) throws InvalidPathSelectionException, IOException {
        // show from tree
        fromTreeContainer = new CopySnapTreeContainer(fromPath, fromBar);
        fromScrollPane.setViewportView(fromTreeContainer.getTree());
        // show to tree
        toTreeContainer = new CopySnapTreeContainer(toPath, toBar);
        toScrollPane.setViewportView(toTreeContainer.getTree());
        // add connections
        fromTreeContainer.setSelectionReceiver(toTreeContainer::selectNodeRelativeToThisTree);
        updateScrollPaneBorders();
    }

    public void toggleTreeLink() {
        if(fromTreeContainer == null || toTreeContainer == null) {
            return;
        }
        fromTreeContainer.setEnableReceiverConnection(!fromTreeContainer.isEnableReceiverConnection());
        updateScrollPaneBorders();
    }

    /**
     * Updates the Border of the Scrollpane inside the tree containers, indicating if the selection-link is active or not.
     */
    private void updateScrollPaneBorders() {
        String fromTitle;
        String toTitle;
        if(currentContext == null || currentSnapshotInfo == null) {
            fromTitle = "No active context and snapshot";
            toTitle = fromTitle;
        } else {
            fromTitle = "CopySnap Data: " + currentSnapshotInfo.getName();
            toTitle = "Source";
        }
        Font f = new Font(Font.MONOSPACED, Font.PLAIN, 10);
        TitledBorder bFrom = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 1), fromTitle, TitledBorder.LEFT, TitledBorder.TOP, f);
        TitledBorder bTo = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 1), toTitle, TitledBorder.LEFT, TitledBorder.TOP, f);
        if(fromTreeContainer != null && !fromTreeContainer.isEnableReceiverConnection()) {
            bFrom.setTitleColor(Color.BLACK);
            bTo.setTitleColor(Color.LIGHT_GRAY);
        }
        fromScrollPane.setBorder(bFrom);
        toScrollPane.setBorder(bTo);
    }

    /**
     * Rebuilds both tree views and updates the tree with potentially newly added or deleted files and
     * selects the tree paths that were selected before the this method ran.
     */
    public void refreshTreeView() {
        if(currentContext == null) {
            return;
        }
        TreePath fromPathOld = fromTreeContainer.getTree().getSelectionPath();
        TreePath toPathOld = toTreeContainer.getTree().getSelectionPath();
        refreshDirectoryTreeDisplays();
        if(fromPathOld != null) {
            Path oldPath = ((FileTreeItem)fromPathOld.getLastPathComponent()).getIOPath();
            fromBar.validateAndSetTextFieldValue(oldPath);
            fromTreeContainer.selectNodeInThisTree(oldPath);
            fromTreeContainer.getTree().expandPath(fromPathOld);
            fromTreeContainer.getTree().revalidate();
        }
        if(toPathOld != null) {
            Path oldPath = ((FileTreeItem)toPathOld.getLastPathComponent()).getIOPath();
            toBar.validateAndSetTextFieldValue(oldPath);
            toTreeContainer.selectNodeInThisTree(oldPath);
            toTreeContainer.getTree().expandPath(toPathOld);
            toTreeContainer.getTree().revalidate();
        }
    }

    /**
     * Starts the restoring process for the currently selected from and to path.
     */
    public void restore() {
        if(currentContext == null) {
            // only use this when there is a context active
            return;
        }
        // check if file names are identical
        Path fromPath;
        Path toPath;
        try {
            fromPath = fromBar.getPath(true);
            toPath= toBar.getPath(true);
        } catch (InvalidPathSelectionException e) {
            throw new CopySnapException("Can not restore from or to invalid path: " + e, e);
        }
        if(!toPath.startsWith(currentContext.getSourcePath())) {
            // for safety reasons only allow to write into source path
            throw new CopySnapException("Currently it is only allowed to write/restore into this context's source path: " + currentContext.getSourcePath());
        }
        int userChoice = getRestoreUserChoice(fromPath, toPath);  // -1: cancel, 0: insert, 1: override
        Path actualTargetPath;
        if(userChoice == 0) {
            // execute insert copy
            if(Files.isRegularFile(toPath)) {
                // copy service is configured to write INTO a directory. for a single file we need to give its parent dir.
                actualTargetPath = toPath.getParent();
            } else if (Files.isDirectory(toPath)) {
                actualTargetPath = toPath;
            } else {
                throw new CopySnapException("Given to-path is invalid: " + toPath);
            }
            BackgroundWorker.builderForJob(() -> CopyService.createCopyService(actualTargetPath == null ? Path.of("/") : actualTargetPath, fromPath).plainCopy())
                    .withJobName("Restoring")
                    .withDoneRunnable(this::refreshTreeView)
                    .build()
                    .executeAndShow();
        } else if(userChoice == 1) {
            // execute override copy
            // copy service is configured to write INTO a directory. in order to override the field at toPath we need to give its parent to the copy service
            actualTargetPath = toPath.getParent();
            BackgroundWorker.builderForJob(() -> CopyService.createCopyService(actualTargetPath == null ? Path.of("/") : actualTargetPath, fromPath).plainCopyOverride())
                    .withJobName("Restoring")
                    .withDoneRunnable(this::refreshTreeView)
                    .build()
                    .executeAndShow();
        }
        // if here: probably cancel
    }

    /**
     * Prompts the user for a choice:
     * <p>-1: cancel, 0: insert, 1: override</p>
     */
    private int getRestoreUserChoice(Path fromPath, Path toPath) {
        int userChoice;
        if (fromPath.getFileName().equals(toPath.getFileName())) {
            if (Files.isDirectory(toPath)) {
                // process would override the target directory or could be inserted into the target directory
                int optionPaneResult = JOptionPane.showOptionDialog(null,
                        MessageUtils.getMessageContentScrollPane(
                                String.format("You are about to restore contents at\n  %s\nwith contents from\n  %s\n\nYou are able to either INSERT into or OVERRIDE the target directory.", toPath, fromPath),
                                true
                        ),
                        "Confirm override or insert",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object [] {"Insert", "Override", "Cancel"},
                        "Insert"
                );
                userChoice = optionPaneResult > 1 ? -1 : optionPaneResult; // exactly the user choice unless "cancel" was pressed
            } else {
                // process would override the target file
                int optionPaneResult = JOptionPane.showConfirmDialog(null,
                        MessageUtils.getMessageContentScrollPane(
                                String.format("You are about to OVERRIDE contents at\n  %s\nwith\n  %s\n\nDo you want to continue?", toPath, fromPath),
                                true
                        ),
                        "Confirm insert",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
                );
                userChoice = optionPaneResult == 0 ? 1 : -1;  // if YES then OVERRIDE
            }
        } else {
            // process would insert into target directory
            int optionPaneResult = JOptionPane.showConfirmDialog(null,
                    MessageUtils.getMessageContentScrollPane(
                            String.format("You are about to INSERT contents into\n  %s\nfrom\n  %s\n\nDo you want to continue?", toPath, fromPath),
                            true
                    ),
                    "Confirm insert",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
            );
            userChoice = optionPaneResult == 0 ? 0 : -1;  // if YES then INSERT
        }
        return userChoice;
    }
}

package jb.gui.components;

import jb.engine.core.Context;
import jb.engine.core.SnapshotInfo;
import jb.engine.reporting.CopyProgress;
import jb.engine.utils.GeneralUtils;
import jb.gui.constants.CopySnapFonts;
import jb.gui.constants.CopySnapGeometry;
import jb.gui.exceptions.CopySnapException;
import jb.gui.utils.LayoutUtils;
import jb.gui.utils.MessageUtils;
import jb.gui.worker.BackgroundWorker;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class CopySnapSidebar extends JPanel {

    private static final String ANALYZED_FILE_COUNT_STRING_TEMPLATE = "Analyzed files: %s (%s regular files, %s directories)";

    private final JPanel snapshotInfoPanel;
    private final BiConsumer<Context, SnapshotInfo> contextSnapshotInfoBiConsumer;
    protected final JButton buttonCopy;
    protected final JButton buttonSnapshot;

    private final List<SnapshotInfoGUIItem> snapshotInfoList = new ArrayList<>();
    private Context context = null;
    private SnapshotInfo currentSelectedSnapshot = null;

    /**
     * Creates a sidebar object showing a list of snapshot information of a currently loaded context.
     * Each of these items may be selected by the user.
     * <p>This container also manages its contents visuals.</p>
     * @param contextSnapshotInfoBiConsumer a method that is called, when a specific SnapshotInfo is selected.
     */
    public CopySnapSidebar(BiConsumer<Context, SnapshotInfo> contextSnapshotInfoBiConsumer) {
        super(new GridBagLayout());
        this.contextSnapshotInfoBiConsumer = contextSnapshotInfoBiConsumer;
        this.buttonCopy = new CopySnapButton("Create plain copy");
        buttonCopy.setPreferredSize(new Dimension(60, CopySnapGeometry.BUTTON_HEIGHT));
        this.buttonCopy.addActionListener(a -> plainCopy());
        this.buttonSnapshot = new CopySnapButton("Create new snapshot");
        this.buttonSnapshot.addActionListener(a -> snapshot());
        buttonSnapshot.setPreferredSize(new Dimension(60, CopySnapGeometry.BUTTON_HEIGHT));
        this.snapshotInfoPanel = new JPanel(new GridBagLayout());
        this.snapshotInfoPanel.setBackground(Color.WHITE);
        this.setPreferredSize(new Dimension(400, this.getPreferredSize().height));
        arrangeContents();
    }

    private void arrangeContents() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(20, 20, 20, 0);
        this.add(Box.createRigidArea(new Dimension()), c);
        c = new GridBagConstraints();
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.45;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(20, 20, 5, 0);
        this.add(buttonCopy, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.45;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(20, 20, 5, 0);
        this.add(buttonSnapshot, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 1.0;
        c.weighty = 0.7;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(5, 20, 20, 0);
        JScrollPane scrollPane = new JScrollPane(snapshotInfoPanel);
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);
        LayoutUtils.makeScrollbarsInvisible(scrollPane);
        this.add(scrollPane, c);
    }

    public void setContext(Context context) {
        this.context = context;
        refreshListDisplay();
    }

    private void refreshListDisplay() {
        if(context == null) {
            return;
        }
        snapshotInfoPanel.removeAll();
        snapshotInfoList.clear();
        context.getSnapshotInfoList().sort(Collections.reverseOrder());
        context.getSnapshotInfoList().forEach(si -> snapshotInfoList.add(new SnapshotInfoGUIItem(si, this::snapshotInfoButtonPressed)));
        int row = 0;
        for(SnapshotInfoGUIItem si : snapshotInfoList) {
            GridBagConstraints c  = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = row;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_START;
            snapshotInfoPanel.add(si, c);
            row++;
        }
        GridBagConstraints c  = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        snapshotInfoPanel.add(Box.createRigidArea(new Dimension()), c);
        snapshotInfoPanel.revalidate();
        if(snapshotInfoList.size() > 0) {
            snapshotInfoList.get(0).clickButton();
        }
    }

    public void snapshotInfoButtonPressed(SnapshotInfoGUIItem snapshotInfoGUIItem) {
        snapshotInfoList.forEach(item -> item.updateButton(false));
        snapshotInfoGUIItem.updateButton(true);
        currentSelectedSnapshot = snapshotInfoGUIItem.getSnapshotInfo();
        contextSnapshotInfoBiConsumer.accept(context, snapshotInfoGUIItem.getSnapshotInfo());
    }

    /**
     * Perform a plain copy on the current context and refresh this sidebar.
     */
    public void plainCopy() {
        if(context == null) {
            return;
        }
        SnapshotNameDialog dialog = new SnapshotNameDialog();
        if(dialog.showDialog("Plain Copy") == JOptionPane.OK_OPTION) {
            String runName = getValidatedTextFieldContent(dialog.nameField);
            BackgroundWorker.builderForJob(copyProgress -> context.plainCopyAndSave(runName, copyProgress), CopyProgress.class)
                    .withJobName("Creating plain copy")
                    .withDoneRunnable(this::refreshListDisplay)
                    .withStringMessage(ANALYZED_FILE_COUNT_STRING_TEMPLATE, List.of(CopyProgress::getTotalFileCount, CopyProgress::getTrueFileCount, CopyProgress::getDirectoryCount))
                    .showIntermediateResults(true)
                    .build()
                    .showAndExecute();
        }
    }

    /**
     * Perform a snapshot against the newest Copy-Entry and refresh this sidebar.
     */
    public void snapshot() {
        if(context == null) {
            return;
        }
        if(context.getSnapshotInfoList().isEmpty()) {
            JOptionPane.showConfirmDialog(null,
                    "Can not perform snapshot when there is no earlier iteration to compare to",
                    "Can not perform Snapshot",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
                    );
            return;
        }
        SnapshotNameDialog dialog = new SnapshotNameDialog();
        if(dialog.showDialog("New Snapshot") == JOptionPane.OK_OPTION) {
            String runName = getValidatedTextFieldContent(dialog.nameField);
            BackgroundWorker.builderForJob(copyProgress -> context.snapshotAndSave(runName, copyProgress), CopyProgress.class)
                    .withJobName("Creating Snapshot")
                    .withDoneRunnable(this::refreshListDisplay)
                    .withStringMessage(ANALYZED_FILE_COUNT_STRING_TEMPLATE, List.of(CopyProgress::getTotalFileCount, CopyProgress::getTrueFileCount, CopyProgress::getDirectoryCount))
                    .withProgressFunction(copyProgress -> copyProgress.getPercentage().intValue())
                    .showIntermediateResults(true)
                    .build()
                    .showAndExecute();
        }
    }

    public void deleteSelectedSnapshot() {
        if(context == null || currentSelectedSnapshot == null) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(null,
                MessageUtils.getMessageContentScrollPane(
                        String.format("You are about to delete snapshot \n  %s\n including all attached files on this filesystem.\n\n Do you want to continue?", currentSelectedSnapshot.getName()),
                        true
                ),
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        if(result != JOptionPane.YES_OPTION) {
            return;
        }
        BackgroundWorker.builderForJob(() -> context.deleteSnapshotAndSave(currentSelectedSnapshot))
                .withJobName("Deleting snapshot")
                .withDoneRunnable(this::refreshListDisplay)
                .build()
                .showAndExecute();
    }

    /**
     * Returns teh text field string if it is not blank or null or throws a runtime exception.
     */
    private String getValidatedTextFieldContent(JTextField textField) {
       if(textField.getText() == null || textField.getText().isBlank()) {
           throw new CopySnapException("Invalid text field content: " + textField.getText());
       }
       return textField.getText();
    }

    /**
     * Simple class for showing a dialog to retrieve a user input String.
     */
    private static class SnapshotNameDialog extends JPanel {

        private static final int MAX_STRING_LENGTH = 80;

        private final JTextField nameField;

        private SnapshotNameDialog() {
            this.setLayout(new GridBagLayout());
            this.nameField = new JTextField(GeneralUtils.getNowAsString());
            this.nameField.select(0, nameField.getText().length());
            // TODO: Fix this
            // this.nameField.addAncestorListener(new GeneralUtils.TextFieldAncestorListener());
            arrangeContents();
        }

        private void arrangeContents() {
            GridBagConstraints c = new GridBagConstraints();
            // label
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 10, 5, 10);
            JLabel label = new JLabel("Enter a name for this run:");
            label.setFont(CopySnapFonts.LABEL_TEXT_FONT);
            label.setPreferredSize(new Dimension(0, CopySnapGeometry.BUTTON_HEIGHT));
            this.add(label, c);
            // text field
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 1.0;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(5, 10, 5, 10);
            nameField.setPreferredSize(new Dimension(400, CopySnapGeometry.BUTTON_HEIGHT));
            this.add(nameField, c);
        }

        public int showDialog(String actionName) {
            return JOptionPane.showConfirmDialog(
                    this,
                    this,
                    actionName,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
        }

        public String getTextFieldContent() {
            if(nameField.getText() == null) {
                return "";
            }
            return nameField.getText().substring(Integer.min(nameField.getText().length(), MAX_STRING_LENGTH));
        }

    }

}

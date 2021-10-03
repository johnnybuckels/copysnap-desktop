package jb.gui.components;

import jb.engine.core.SnapshotInfo;
import jb.gui.components.contextmenus.ContextMenu;
import jb.gui.components.listeners.ContextMenuListener;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class SnapshotInfoGUIItem extends JPanel {

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final SnapshotInfo snapshotInfo;
    private final JToggleButton infoButton = new JToggleButton();
    private final Runnable refreshRunnable;

    /**
     * Creates a Display-Container for a SnapshotInfo Object.
     * It provides functionality to transmit this Display-Items actual SnapshotInfo.
     * @param snapshotInfo the info to store in this display item.
     * @param snapshotInfoGUIItemConsumer a method that is called with this container when this containers button is pressed.
     * @param refreshRunnable a runnable that is run, when this gui item has been updated somehow
     */
    public SnapshotInfoGUIItem(SnapshotInfo snapshotInfo, Consumer<SnapshotInfoGUIItem> snapshotInfoGUIItemConsumer, Runnable refreshRunnable) {
        super(new GridBagLayout());
        this.snapshotInfo = snapshotInfo;
        this.refreshRunnable = refreshRunnable;

        infoButton.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED, null, Color.DARK_GRAY));
        infoButton.setText(snapshotInfo.getName());
        infoButton.addActionListener(action -> snapshotInfoGUIItemConsumer.accept(this));
        arrangeContents();
        this.setMaximumSize(new Dimension(this.getPreferredSize().width, 60));
        this.addMouseListener(new ContextMenuListener(createNewContextMenu()));
    }

    private ContextMenu<SnapshotInfoGUIItem> createNewContextMenu() {
        return ContextMenu.of(this)
                .addAction("Rename...", SnapshotInfoGUIItem::startRenameDialogAndRename);
    }

    private void arrangeContents() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        this.add(infoButton, c);

        JLabel labelTime = new JLabel();
        labelTime.setText(DATE_TIME_FORMATTER.format(snapshotInfo.getCreatedTime()));
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 2, 2, 4);
        this.add(labelTime, c);

        JLabel labelIsCopy = new JLabel();
        labelIsCopy.setText(snapshotInfo.getCopyType().getName());
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(2, 4, 2, 2);
        this.add(labelIsCopy, c);
    }

    public SnapshotInfo getSnapshotInfo() {
        return snapshotInfo;
    }

    public void updateButton(boolean isSelected) {
        infoButton.setSelected(isSelected);
    }

    public void clickButton() {
        infoButton.doClick();
    }

    public void startRenameDialogAndRename() {
        TextInputDialog dialog = new TextInputDialog("Rename SnapshotInfo", "New name", snapshotInfo.getName());
        if(dialog.showDialog() == JOptionPane.OK_OPTION) {
            String newName = dialog.getNotNullNotBlankTextFieldContent();
            snapshotInfo.setName(newName);
            refreshRunnable.run();
        }
    }

}

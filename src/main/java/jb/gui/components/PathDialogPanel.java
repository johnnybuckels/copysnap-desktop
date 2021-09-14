package jb.gui.components;

import jb.gui.constants.CopySnapFonts;
import jb.gui.worker.FocusGetter;

import javax.swing.*;
import java.awt.*;

public class PathDialogPanel extends JPanel {

    private final PathSelectionBar pathSelectionBar;
    private final JLabel label;

    public PathDialogPanel() {
        pathSelectionBar = new PathSelectionBar("", 0.);
        label = new JLabel();
        arrangeDialogContent();
    }

    public int showDialog(Component parent, String initialPath, String title, String text) {
        pathSelectionBar.validateAndSetTextFieldValue(initialPath);
        label.setText(text);
        new FocusGetter(pathSelectionBar.getTextField()).tryToGetFocus();
        return JOptionPane.showConfirmDialog(
                parent,
                this,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private void arrangeDialogContent() {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        // text
        label.setFont(CopySnapFonts.LABEL_TEXT_FONT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        pathSelectionBar.setAlignmentX(LEFT_ALIGNMENT);
        this.add(label);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(pathSelectionBar);
        this.setPreferredSize(new Dimension(900, 100));
    }

    public PathSelectionBar getPathSelectionBar() {
        return pathSelectionBar;
    }
}

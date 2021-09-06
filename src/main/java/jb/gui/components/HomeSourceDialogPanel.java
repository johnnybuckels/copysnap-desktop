package jb.gui.components;

import jb.gui.constants.CopySnapFonts;
import jb.gui.windows.listeners.ComponentShownListener;

import javax.swing.*;
import java.awt.*;

public class HomeSourceDialogPanel extends JPanel{

    private final PathSelectionBar homeBar;
    private final PathSelectionBar sourceBar;

    public HomeSourceDialogPanel() {
        homeBar = new PathSelectionBar("Home directory location");
        sourceBar = new PathSelectionBar("Source directory location");
    }

    public int showDialog(Component parent, String initialHome, String initialSource, String title, String text) {
        homeBar.validateAndSetTextFieldValue(initialHome);
        sourceBar.validateAndSetTextFieldValue(initialSource);
        arrangeDialogContent(text);
        return JOptionPane.showConfirmDialog(
                parent,
                this,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private void arrangeDialogContent(String text) {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        // text
        JLabel label = new JLabel(text);
        label.setFont(CopySnapFonts.LABEL_TEXT_FONT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        homeBar.setAlignmentX(LEFT_ALIGNMENT);
        sourceBar.setAlignmentX(LEFT_ALIGNMENT);
        this.add(label);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(homeBar);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(sourceBar);
        this.setPreferredSize(new Dimension(900, 100));
        homeBar.addComponentListener(new ComponentShownListener(componentEvent -> homeBar.requestFocus()));
    }

    public PathSelectionBar getHomeBar() {
        return homeBar;
    }

    public PathSelectionBar getSourceBar() {
        return sourceBar;
    }
}

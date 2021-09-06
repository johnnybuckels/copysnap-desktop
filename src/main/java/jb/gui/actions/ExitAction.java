package jb.gui.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExitAction implements ActionListener {
    private final JFrame linkedFrame;

    public ExitAction(JFrame linkedFrame) {
        this.linkedFrame = linkedFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        linkedFrame.dispose();
        System.exit(0);
    }
}

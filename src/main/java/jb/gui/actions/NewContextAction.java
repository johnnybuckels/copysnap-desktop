package jb.gui.actions;

import jb.engine.core.Context;
import jb.gui.components.HomeSourceDialogPanel;
import jb.gui.exceptions.CopySnapException;
import jb.gui.windows.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class NewContextAction implements ActionListener {

    private final MainWindow linkedFrame;
    private final Consumer<Context> dialogResultConsumingAction;

    public NewContextAction(MainWindow linkedFrame, Consumer<Context> dialogResultConsumingAction) {
        this.linkedFrame = linkedFrame;
        this.dialogResultConsumingAction = dialogResultConsumingAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialogResultConsumingAction.accept(show(linkedFrame));
    }

    public Context show(JFrame parent) {
        HomeSourceDialogPanel homeSourceDialogPanel = new HomeSourceDialogPanel();
        Integer response = null;
        while(response == null) {
            Thread.onSpinWait();
            response = homeSourceDialogPanel.showDialog(parent,
                    homeSourceDialogPanel.getHomeBar().getPlainTextContent(),
                    homeSourceDialogPanel.getSourceBar().getPlainTextContent(),
                    "New Context",
                    "Choose a location where the new context's home directory should be created and where CopySnap can find the source directory."
            );
            if (response == JOptionPane.OK_OPTION) {
                Context context;
                try {
                    context = Context.createNewContextAndInitialise(homeSourceDialogPanel.getSourceBar().getPath(), homeSourceDialogPanel.getHomeBar().getPath());
                } catch (Exception e) {
                    throw new CopySnapException("Could not create new context: " + e, e);
                }
               return context;
            }
        }
        return null;
    }

}

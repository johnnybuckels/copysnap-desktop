package jb.gui.actions;

import jb.engine.core.Context;
import jb.gui.components.HomeSourceDialogPanel;
import jb.gui.exceptions.InvalidPathSelectionException;
import jb.gui.utils.MessageUtils;
import jb.gui.windows.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
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
        HomeSourceDialogPanel homeSourceDialogPanel = new HomeSourceDialogPanel();;
        boolean invalidPathsGiven = true;
        Context context = null;
        while(invalidPathsGiven) {
            int response = homeSourceDialogPanel.showDialog(parent,
                    homeSourceDialogPanel.getHomeBar().getPlainTextContent(),
                    homeSourceDialogPanel.getSourceBar().getPlainTextContent(),
                    "New Context",
                    "Choose a location where the new context's home directory should be created and where CopySnap can find the source directory."
            );
            if (response == JOptionPane.OK_OPTION) {
                // cancel option
                return null;
            }
            try {
                Path sourcePath = homeSourceDialogPanel.getSourceBar().getPath();
                Path homePath = homeSourceDialogPanel.getHomeBar().getPath();
                context = Context.createNewContextAndInitialise(sourcePath, homePath);
            } catch (InvalidPathSelectionException e) {
                MessageUtils.showInfoMessage(null, "Given paths were invalid: " + e, "Invalid paths");
            }
            invalidPathsGiven = false;
        }
        return context;
    }

}

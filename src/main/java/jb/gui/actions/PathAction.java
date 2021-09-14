package jb.gui.actions;

import jb.gui.components.PathDialogPanel;
import jb.gui.exceptions.InvalidPathSelectionException;
import jb.gui.utils.MessageUtils;
import jb.gui.windows.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Action showing a dialog asking for a Path.
 */
public class PathAction implements ActionListener {

    private final MainWindow linkedFrame;
    private final Consumer<Path> homePathConsumer;

    /**
     * Creates an action prompting the user for a path and returning that path. If the prompt is aborted, {@code null}
     * is returned.
     */
    public PathAction(MainWindow linkedFrame, Consumer<Path> homePathConsumer) {
        this.linkedFrame = linkedFrame;
        this.homePathConsumer = homePathConsumer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        homePathConsumer.accept(show(linkedFrame));
    }

    public Path show(JFrame parent) {
        PathDialogPanel pathDialogPanel = new PathDialogPanel();
        Path homePath = null;
        while(homePath == null) {
            int response = pathDialogPanel.showDialog(parent,
                    pathDialogPanel.getPathSelectionBar().getPlainTextContent(),
                    "Restore Context",
                    "Choose the home directory of the context that should be restored"
            );
            if (response != JOptionPane.OK_OPTION) {
                return null;
            }
            try {
                homePath = pathDialogPanel.getPathSelectionBar().getPath();
            } catch (InvalidPathSelectionException e) {
                MessageUtils.showErrorTextMessage(null, "Given path is invalid: " + e, "Invalid path");
            }
        }
        return homePath;
    }

}

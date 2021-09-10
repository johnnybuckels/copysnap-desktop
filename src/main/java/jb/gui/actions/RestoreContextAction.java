package jb.gui.actions;

import jb.engine.core.Context;
import jb.engine.reporting.CopyProgress;
import jb.gui.components.HomeSourceDialogPanel;
import jb.gui.exceptions.CopySnapException;
import jb.gui.utils.MessageUtils;
import jb.gui.windows.MainWindow;
import jb.gui.worker.BackgroundWorker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class RestoreContextAction implements ActionListener {

    private final MainWindow linkedFrame;
    private final Consumer<Context> dialogResultConsumingAction;

    private Context reconstructedContext = null;

    public RestoreContextAction(MainWindow linkedFrame, Consumer<Context> dialogResultConsumingAction) {
        this.linkedFrame = linkedFrame;
        this.dialogResultConsumingAction = dialogResultConsumingAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialogResultConsumingAction.accept(show(linkedFrame));
    }

    public Context show(JFrame parent) {
        HomeSourceDialogPanel homeSourceDialogPanel = new HomeSourceDialogPanel();
        reconstructedContext = null;
        Integer response = null;
        while(response == null) {
            Thread.onSpinWait();
            response = homeSourceDialogPanel.showDialog(parent,
                    homeSourceDialogPanel.getHomeBar().getPlainTextContent(),
                    homeSourceDialogPanel.getSourceBar().getPlainTextContent(),
                    "Restore Context",
                    "Choose the location of the suggested source directory and the home directory of the context that should be restored"
            );
            if (response == JOptionPane.OK_OPTION) {
                try {
                    Path sourcePath = homeSourceDialogPanel.getSourceBar().getPath();
                    Path homePath = homeSourceDialogPanel.getHomeBar().getPath();
                    BackgroundWorker.builderForJob(copyProgressConsumer -> Context.reconstructContext(sourcePath, homePath, copyProgressConsumer), CopyProgress.class)
                            .withJobName("Reconstructing context")
                            .withResultConsumer(ctxResult -> reconstructedContext = ctxResult)
                            .withStringMessage("", List.of()) // TODO: Create message
                            .showIntermediateResults(true);
                } catch (Exception e) {
                    throw new CopySnapException("Could not reconstruct context: " + e, e);
                }
                if(reconstructedContext == null) {
                    throw new CopySnapException("Reconstruction failed: resulting context was null");

                }
                MessageUtils.showInfoMessage(null,
                        "Successfully restored Context from filesystem at\n" +
                                reconstructedContext.getHomePath(),
                        "Restoration successful!"
                        );
                return reconstructedContext;
            }
        }
        return null;
    }

}

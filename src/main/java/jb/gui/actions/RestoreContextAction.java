package jb.gui.actions;

import jb.engine.core.Context;
import jb.gui.components.HomeSourceDialogPanel;
import jb.gui.exceptions.CopySnapException;
import jb.gui.utils.MessageUtils;
import jb.gui.windows.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.function.Consumer;

public class RestoreContextAction implements ActionListener {

    private final MainWindow linkedFrame;
    private final Consumer<Context> dialogResultConsumingAction;

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
                Context context;
                try {
                    // TODO: Add consumer for reconstruction copy progress: use a worker here.
                    context = Context.reconstructContext(homeSourceDialogPanel.getSourceBar().getPath(), homeSourceDialogPanel.getHomeBar().getPath(), x -> {});
                } catch (Exception e) {
                    throw new CopySnapException("Could not reconstruct context: " + e, e);
                }
                MessageUtils.showInfoMessage(null,
                        "Successfully restored Context from filesystem at\n" +
                                context.getHomePath(),
                        "Restoration successful!"
                        );
                return context;
            }
        }
        return null;
    }

    public static class HomeSourceContainer {
        private final Path initialPath;
        private final Path sourcePath;

        public HomeSourceContainer(Path initialPath, Path sourcePath) {
            this.initialPath = initialPath;
            this.sourcePath = sourcePath;
        }

        public Path getInitialPath() {
            return initialPath;
        }

        public Path getSourcePath() {
            return sourcePath;
        }
    }

}

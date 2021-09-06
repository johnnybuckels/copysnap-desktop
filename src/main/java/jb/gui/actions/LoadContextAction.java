package jb.gui.actions;

import jb.engine.core.Context;
import jb.engine.core.ContextInfoContainer;
import jb.engine.exceptions.DatabaseCommunicationException;
import jb.engine.exceptions.NotFoundException;
import jb.gui.exceptions.CopySnapException;
import jb.gui.windows.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class LoadContextAction implements ActionListener {

    private final MainWindow linkedFrame;
    private final Consumer<Context> dialogResultConsumingAction;

    public LoadContextAction(MainWindow linkedFrame, Consumer<Context> dialogResultConsumingAction) {
        this.linkedFrame = linkedFrame;
        this.dialogResultConsumingAction = dialogResultConsumingAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialogResultConsumingAction.accept(show(linkedFrame));
    }

    public Context show(JFrame parent) {
        ContextInfoContainer[] contextHomePaths;
        try {
            contextHomePaths = Context.getStoredContextInfo().toArray(new ContextInfoContainer[0]);
        } catch(DatabaseCommunicationException e) {
            throw new CopySnapException("Could not determine existing Contexts: " + e, e);
        }
        if(contextHomePaths.length == 0) {
            JOptionPane.showConfirmDialog(parent,
                    "There are no contexts to load",
                    "No Contexts",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
                    );
            return null;
        }
        Context loadedContext;
        String contextId = ((ContextInfoContainer) JOptionPane.showInputDialog(parent,
                "Choose a context to load:",
                "Load Context",
                JOptionPane.PLAIN_MESSAGE,
                null,
                contextHomePaths,
                contextHomePaths[0]
        )).getId();
        try {
            loadedContext = Context.loadContextById(contextId);
        } catch (NotFoundException | DatabaseCommunicationException e) {
            throw new CopySnapException("Could not load context with id: " + contextId + ": " + e, e);
        }
        return loadedContext;
    }

}

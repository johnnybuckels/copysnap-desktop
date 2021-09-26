package jb.gui.components.listeners;

import jb.gui.components.PathSelectionBar;
import jb.gui.utils.PathCompletionHelper;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TextFieldPathCompletionKeyListener implements KeyListener {

    private final Logger logger = Logger.getLogger(PathSelectionBar.class.getSimpleName());
    private final JTextField textField;
    private final Consumer<String> contentConsumerEnter;
    private final Consumer<String> contentConsumerPathIterator;

    private Iterator<Path> currentPathIterator = null;

    public TextFieldPathCompletionKeyListener(JTextField textField, Consumer<String> contentConsumerEnter, Consumer<String> contentConsumerPathIterator) {
        this.textField = textField;
        this.contentConsumerEnter = contentConsumerEnter;
        this.contentConsumerPathIterator = contentConsumerPathIterator;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * <li><b>Enter</b>: validate selection bar content and send the result to the bar's consumer</li>
     * <li><b>Tab</b> (initial): create a new path iterator for the currently displayed path and display the first item</li>
     * <li><b>Tab</b> (consecutive): display the current iterators next item</li>
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getSource() != textField) {
            logger.warning("event" + e + "does not reside from " + textField + " but from " + e.getSource());
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                contentConsumerEnter.accept(textField.getText());
                break;
            case KeyEvent.VK_TAB:
                if(currentPathIterator == null) {
                    initializeNewPathIterator();
                }
                displayNextPathOnSelectionBar();
                break;
            default:
                currentPathIterator = null;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void initializeNewPathIterator() {
        Path currentSelectionPathPlain = Path.of(textField.getText());
        Path searchBasePath;
        String searchPrefix;
        if(Files.exists(currentSelectionPathPlain)) {
            searchBasePath = currentSelectionPathPlain;
            searchPrefix = "";
        } else {
            searchBasePath = currentSelectionPathPlain.getParent();
            searchPrefix = currentSelectionPathPlain.getFileName().toString();
        }
        currentPathIterator = PathCompletionHelper.getPathIteratorFilteredByPrefix(searchBasePath, searchPrefix);
    }

    private void displayNextPathOnSelectionBar() {
        if(currentPathIterator == null || !currentPathIterator.hasNext()) {
            return;
        }
        // insert path into text field
        Path nextPath = currentPathIterator.next();
        contentConsumerPathIterator.accept(nextPath.toString());
        textField.requestFocusInWindow();
        // select filename
        int selectionStart = nextPath.getParent().toString().length();
        int textLength = textField.getText().length();
        textField.setCaretPosition(textLength);
        if(selectionStart < textLength) {
            textField.select(selectionStart, textLength);
        }
    }



}

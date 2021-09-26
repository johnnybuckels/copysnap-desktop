package jb.gui.components.listeners;

import jb.gui.components.PathSelectionBar;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public final class PathSelectionBarFocusListener implements FocusListener {
    private final PathSelectionBar pathSelectionBar;

    public PathSelectionBarFocusListener(PathSelectionBar pathSelectionBar) {
        this.pathSelectionBar = pathSelectionBar;
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        pathSelectionBar.validateAndSetTextFieldValue(pathSelectionBar.getValidatedTextFieldValueFromString(pathSelectionBar.getTextField().getText()));
    }
}

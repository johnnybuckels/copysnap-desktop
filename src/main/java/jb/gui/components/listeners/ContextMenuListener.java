package jb.gui.components.listeners;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ContextMenuListener extends MouseAdapter {

    private final JPopupMenu contextMenu;

    public ContextMenuListener(JPopupMenu contextMenu) {
        this.contextMenu = contextMenu;
    }

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showContextMenu(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    private void showContextMenu(MouseEvent e) {
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }
}

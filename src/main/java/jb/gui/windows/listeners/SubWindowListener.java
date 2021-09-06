package jb.gui.windows.listeners;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Used for progressbar window.
 */
public class SubWindowListener implements WindowListener {

    private final Runnable customClosedOperation;

    public SubWindowListener(Runnable customClosedOperation) {
        this.customClosedOperation = customClosedOperation;
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {
        customClosedOperation.run();
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}

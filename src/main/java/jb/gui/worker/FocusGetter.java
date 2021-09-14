package jb.gui.worker;

import javax.swing.*;
import java.awt.*;

/**
 * Background job that tries to get the focus on a component
 */
public class FocusGetter extends SwingWorker<Void, Void> {

    private final static long DEFAULT_TIMEOUT = 500;
    private final static long DEFAULT_RETRY_DELAY = 100;

    private final Component component;
    private final long timeout;
    private final long retryDelay;

    public FocusGetter(Component component) {
        this(component, DEFAULT_TIMEOUT, DEFAULT_RETRY_DELAY);
    }

    public FocusGetter(Component component, long retryDelay) {
        this(component, DEFAULT_TIMEOUT, retryDelay);
    }

    public FocusGetter(Component component, long timeoutMs, long retryDelay) {
        this.component = component;
        if(!component.isFocusable()) {
            throw new IllegalArgumentException("Given component " + component.getName() + " is not focusable");
        }
        if(timeoutMs < 0 || retryDelay < 0) {
            throw new IllegalArgumentException("timeout and delay need to be non negative");
        }
        this.timeout = timeoutMs;
        this.retryDelay = retryDelay;
    }

    public void tryToGetFocus() {
        try {
            execute();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        long start = System.currentTimeMillis();
        while(!component.hasFocus() && System.currentTimeMillis() - start < timeout) {
            Thread.sleep(retryDelay);
            if(component.hasFocus()) {
                break;
            }
            component.requestFocusInWindow();
        }
        return null;
    }
}

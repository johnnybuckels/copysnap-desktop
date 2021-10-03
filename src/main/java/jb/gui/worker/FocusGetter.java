package jb.gui.worker;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Background job that tries to get the focus on a component
 */
public class FocusGetter extends SwingWorker<Void, Void> {

    private final static int DEFAULT_RETRY_COUNT = 5;
    private final static long DEFAULT_RETRY_DELAY = 100;

    private final Component component;
    private final int maxRetryCount;
    private final long retryDelayMs;

    public FocusGetter(Component component) {
        this(component, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_DELAY);
    }

    public FocusGetter(Component component, long retryDelayMs) {
        this(component, DEFAULT_RETRY_COUNT, retryDelayMs);
    }

    public FocusGetter(Component component, int maxRetryCount, long retryDelayMs) {
        this.component = component;
        if(!component.isFocusable()) {
            throw new IllegalArgumentException("Given component " + component.getName() + " is not focusable");
        }
        if(maxRetryCount < 0 || retryDelayMs < 0) {
            throw new IllegalArgumentException("maxRetryCount and delay need to be non negative");
        }
        this.maxRetryCount = maxRetryCount;
        this.retryDelayMs = retryDelayMs;
    }

    public void tryToGetFocus() {
        try {
            execute();
        } catch (Exception ignored) {
            // do not do anything
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(maxRetryCount);
        boolean maxRetriesNotReached = true;
        while(maxRetriesNotReached) {
            maxRetriesNotReached = !countDownLatch.await(retryDelayMs, TimeUnit.MILLISECONDS);
            if(component.hasFocus()) {
                break;
            }
            component.requestFocusInWindow();
            countDownLatch.countDown();
        }
        return null;
    }

}

package jb.gui.windows.listeners;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.function.Consumer;

public class ComponentShownListener implements ComponentListener {

    private final Consumer<ComponentEvent> componentShownEventConsumer;

    public ComponentShownListener(Consumer<ComponentEvent> componentShownEventConsumer) {
        this.componentShownEventConsumer = componentShownEventConsumer;
    }

    @Override
    public void componentResized(ComponentEvent e) {

    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {
        componentShownEventConsumer.accept(e);
    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }
}

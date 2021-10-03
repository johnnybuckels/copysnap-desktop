package jb.gui.components.contextmenus;


import javax.swing.*;
import java.util.function.Consumer;

/**
 * Generic class representing a context menu for items within the CopySnap GUI.
 * @param <T> the type of the gui item this context menu is meant for
 */
public class ContextMenu<T> extends JPopupMenu {

    private final T itemToPerformActionWith;

    public static <X> ContextMenu<X> of(X itemToPerformActionWith) {
        return new ContextMenu<>(itemToPerformActionWith);
    }

    private ContextMenu(T itemToPerformActionWith) {
        this.itemToPerformActionWith = itemToPerformActionWith;
    }

    public ContextMenu<T> addAction(String itemName, Consumer<T> associatedAction) {
        JMenuItem newItem = new JMenuItem(itemName);
        newItem.addActionListener(clickAction -> associatedAction.accept(itemToPerformActionWith));
        add(newItem);
        return this;
    }

}

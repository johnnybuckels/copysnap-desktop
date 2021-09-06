package jb.gui.components;

import javax.swing.*;
import java.awt.*;

public class CopySnapMenuBar extends JMenuBar {

    private final JFrame parentFrame;
    private final int insetMargin = 2;
    private final Insets insets = new Insets(insetMargin, insetMargin, insetMargin, insetMargin);

    public CopySnapMenuBar(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.setBorderPainted(false);
        this.setMargin(insets);
    }

    public JFrame getParentFrame() {
        return parentFrame;
    }

    public int getInsetMargin() {
        return insetMargin;
    }

    @Override
    public Insets getInsets() {
        return insets;
    }
}

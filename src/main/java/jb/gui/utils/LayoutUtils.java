package jb.gui.utils;

import javax.swing.*;
import java.awt.*;

public class LayoutUtils {

    public static GridBagConstraints getBasicHorizontalGBC(int xPos, int yPos, double xWeight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = xPos;
        c.gridy = yPos;
        c.weightx = xWeight;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    public static GridBagConstraints getBasicVerticalGBC(int xPos, int yPos, double yWeight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = xPos;
        c.gridy = yPos;
        c.weightx = 1.0;
        c.weighty = yWeight;
        c.fill = GridBagConstraints.VERTICAL;
        return c;
    }

    public static void makeScrollbarsThin(JScrollPane jScrollPane) {
        jScrollPane.getVerticalScrollBar().setOpaque(true);
        jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        jScrollPane.getHorizontalScrollBar().setOpaque(true);
        jScrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
    }

    public static void makeScrollbarsInvisible(JScrollPane jScrollPane) {
        jScrollPane.getVerticalScrollBar().setBorder(null);
        jScrollPane.getVerticalScrollBar().setOpaque(false);
        jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        jScrollPane.getHorizontalScrollBar().setBorder(null);
        jScrollPane.getHorizontalScrollBar().setOpaque(false);
        jScrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
    }

}

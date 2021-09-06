package jb.gui.components;

import javax.swing.*;
import java.awt.*;

/**
 * JScrollPane in a JPanel with some copy snap adjustments.
 */
public class PanelWithScrollPanel extends JPanel {

    public PanelWithScrollPanel(JScrollPane scrollPane) {
        super(new GridBagLayout());
        this.setPreferredSize(new Dimension(0, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;
        this.add(scrollPane, c);
    }

}

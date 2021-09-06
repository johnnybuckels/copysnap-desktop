package jb.gui.components;

import jb.gui.constants.CopySnapGeometry;

import javax.swing.*;
import java.awt.*;

public class CopySnapButton extends JButton {

    public CopySnapButton(String text) {
        super(text);
        this.setPreferredSize(new Dimension(this.getPreferredSize().width, CopySnapGeometry.BUTTON_HEIGHT));
    }

    public CopySnapButton(Icon icon) {
        super(icon);
        this.setPreferredSize(new Dimension(this.getPreferredSize().width, CopySnapGeometry.BUTTON_HEIGHT));
    }
}

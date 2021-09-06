package jb.gui.components;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

public class CopySnapFileChooser extends JFileChooser {

    public CopySnapFileChooser() {
        super();
        this.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.setPreferredSize(new Dimension(600, 600));
        this.getActionMap().get("viewTypeDetails").actionPerformed(null);
    }

    public Path chooseAPathWithDialog(String title, Component parent, File startingDir) {
        this.setCurrentDirectory(startingDir);
        this.setDialogTitle(title);
        int returnVal = this.showDialog(parent, "Open Directory");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return getSelectedFile().toPath();
        } else {
            return null;
        }
    }

    public Path chooseAPathWithDialog(String title, Component parent) {
        this.setDialogTitle(title);
        int returnVal = this.showDialog(parent, "Open Directory");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return getSelectedFile().toPath();
        } else {
            return null;
        }
    }
}

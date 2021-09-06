package jb.gui;

import jb.engine.core.data.DatabaseManager;
import jb.gui.exceptions.GlobalExceptionHandler;
import jb.gui.windows.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        setUIDefaults();
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        DatabaseManager.initializeDefaultManager();
        javax.swing.SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.pack();
            mainWindow.setVisible(true);
            mainWindow.requestFocus();
        });
    }

    private static void setUIDefaults() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        Icon emptyIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {}
            @Override
            public int getIconWidth() { return 0; }
            @Override
            public int getIconHeight() { return 0; }
        };
        UIManager.put("Tree.collapsedIcon", emptyIcon);
        UIManager.put("Tree.expandedIcon", emptyIcon);
        JFrame.setDefaultLookAndFeelDecorated(false);
    }

}

package jb.gui.utils;

import jb.gui.constants.CopySnapFonts;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

import static jb.gui.utils.LayoutUtils.makeScrollbarsInvisible;

public class MessageUtils {

    private final static int MAX_SCROLL_PANE_WIDTH = 800;
    private final static int MIN_SCROLL_PANE_WIDTH = 300;
    private final static int MAX_SCROLL_PANE_HEIGHT = 600;
    private final static int MIN_SCROLL_PANE_HEIGHT = 80;
    private final static int WIDTH_PER_COLUMN = 8;
    private final static int HEIGHT_PER_LINE = 20;

    public static void showErrorTextMessage(Component parent, String textMessage, String messageTitle) {
        JTextArea area = new JTextArea(textMessage);
        area.setFont(CopySnapFonts.MESSAGE_FONT);
        area.setLineWrap(false);
        area.setEditable(false);
        area.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(getDimensionRelativeToTextArea(area));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JOptionPane.showMessageDialog(parent,
                scrollPane,
                messageTitle,
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void showInfoMessage(Component parent, String textMessage, String messageTitle) {
        JOptionPane.showMessageDialog(parent,
                getMessageContentScrollPane(textMessage, true),
                messageTitle,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static JScrollPane getMessageContentScrollPane(String textMessage, boolean invisibleScrollbar) {
        JTextArea area = new JTextArea(textMessage);
        area.setEditable(false);
        area.setFont(CopySnapFonts.MESSAGE_FONT);
        area.setOpaque(false);
        area.setLineWrap(false);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(getDimensionRelativeToTextArea(area));
        if(invisibleScrollbar) {
            makeScrollbarsInvisible(scrollPane);
        }
        return scrollPane;
    }

    private static Dimension getDimensionRelativeToTextArea(JTextArea area) {
        int longestLineWidth = Arrays.stream(area.getText().split("\n")).mapToInt(String::length).max().orElse(-1);
        return new Dimension(
                Math.min(MAX_SCROLL_PANE_WIDTH, Math.max(MIN_SCROLL_PANE_WIDTH, WIDTH_PER_COLUMN * longestLineWidth)),
                Math.min(MAX_SCROLL_PANE_HEIGHT, Math.max(MIN_SCROLL_PANE_HEIGHT, HEIGHT_PER_LINE * area.getLineCount()))
        );
    }

}

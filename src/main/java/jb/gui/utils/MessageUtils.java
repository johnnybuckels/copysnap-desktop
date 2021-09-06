package jb.gui.utils;

import jb.gui.constants.CopySnapFonts;

import javax.swing.*;
import java.awt.*;

import static jb.gui.utils.LayoutUtils.makeScrollbarsInvisible;

public class MessageUtils {

    public static void showErrorTextMessage(Component parent, String textMessage, String messageTitle) {
        JTextArea area = new JTextArea(textMessage);
        area.setFont(CopySnapFonts.MESSAGE_FONT);
        area.setLineWrap(false);
        area.setEditable(false);
        area.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(800, 600));
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
        scrollPane.setPreferredSize(new Dimension(600, 100));
        if(invisibleScrollbar) {
            makeScrollbarsInvisible(scrollPane);
        }
        return scrollPane;
    }

}

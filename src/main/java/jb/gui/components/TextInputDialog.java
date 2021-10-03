package jb.gui.components;

import jb.engine.utils.GeneralUtils;
import jb.gui.constants.CopySnapFonts;
import jb.gui.constants.CopySnapGeometry;
import jb.gui.worker.FocusGetter;

import javax.swing.*;
import java.awt.*;

/**
 * Simple class for showing a dialog to retrieve a user input String.
 */
public class TextInputDialog extends JPanel {

    private static final int MAX_STRING_LENGTH = 80;

    private final JTextField textField;
    private final String dialogTitle;

    public TextInputDialog(String dialogTitle, String labelText) {
        this(dialogTitle, labelText, GeneralUtils.getNowAsString());
    }

    public TextInputDialog(String dialogTitle, String labelText, String defaultTextFieldEntry) {
        this.dialogTitle = dialogTitle;
        this.setLayout(new GridBagLayout());
        this.textField = new JTextField(defaultTextFieldEntry);
        this.textField.select(0, textField.getText().length());
        arrangeContents(labelText);
    }

    private void arrangeContents(String labelText) {
        GridBagConstraints c = new GridBagConstraints();
        // label
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 10, 5, 10);
        JLabel label = new JLabel(labelText);
        label.setFont(CopySnapFonts.LABEL_TEXT_FONT);
        label.setPreferredSize(new Dimension(0, CopySnapGeometry.BUTTON_HEIGHT));
        this.add(label, c);
        // text field
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 10, 5, 10);
        textField.setPreferredSize(new Dimension(400, CopySnapGeometry.BUTTON_HEIGHT));
        this.add(textField, c);
        textField.selectAll();
    }

    public int showDialog() {
        new FocusGetter(textField).tryToGetFocus();
        return JOptionPane.showConfirmDialog(
                null,
                this,
                dialogTitle,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
    }

    public String getTextFieldContent() {
        if (textField.getText() == null) {
            return "";
        }
        return textField.getText().substring(Integer.min(textField.getText().length(), MAX_STRING_LENGTH));
    }

    /**
     * Returns the text field string if it is not blank or null or throws an IllegalStateException.
     */
    public String getNotNullNotBlankTextFieldContent() throws IllegalStateException {
        String text = textField.getText();
        if(text == null || text.isBlank()) {
            throw new IllegalStateException("Invalid text field content: " + text);
        }
        return text;
    }

}

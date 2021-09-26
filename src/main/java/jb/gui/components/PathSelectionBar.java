package jb.gui.components;

import jb.gui.components.listeners.PathSelectionBarFocusListener;
import jb.gui.components.listeners.TextFieldPathCompletionKeyListener;
import jb.gui.constants.CopySnapFonts;
import jb.gui.constants.CopySnapGeometry;
import jb.gui.exceptions.InvalidPathSelectionException;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class PathSelectionBar extends JPanel {

    private final static double BUTTON_WIDTH_RATIO = 0.05;

    private final JTextField textField;
    private final CopySnapButton button;
    private final String labelText;
    private final double labelWidthRatio;

    private final List<Consumer<Path>> pathConsumers = new LinkedList<>();
    private Path fixRootPath = null;

    public PathSelectionBar(String labelText, double labelWidthRatio) {
        if (labelWidthRatio < 0 || labelWidthRatio > 1.0 - BUTTON_WIDTH_RATIO) {
            throw new IllegalArgumentException("labelWidthRatio needs to be a value from 0 to " + (1.0 - BUTTON_WIDTH_RATIO));
        }
        this.button = new CopySnapButton(UIManager.getIcon("Tree.openIcon"));
        button.addActionListener(a -> showBrowsingDialog());
        this.textField = new JTextField(20);
        this.textField.setFont(CopySnapFonts.TEXT_FIELD_FONT);
        this.labelText = labelText;
        this.labelWidthRatio = labelWidthRatio;
        this.textField.setFocusTraversalKeysEnabled(false);
        this.textField.addKeyListener(new TextFieldPathCompletionKeyListener(this.textField,
                        str -> {
                            validateAndSetTextFieldValue(str);
                            pathConsumers.forEach(consumer -> consumer.accept(Path.of(str)));
                        },
                        this::validateAndSetTextFieldValue
        ));
        addFocusListener(new PathSelectionBarFocusListener(this));
        applyVisuals();
    }

    public void setFixRootPath(Path fixRootPath) {
        this.fixRootPath = fixRootPath;
        this.validateAndSetTextFieldValue(fixRootPath.toString());
    }

    public void addPathConsumer(Consumer<Path> pathConsumer) {
        this.pathConsumers.add(pathConsumer);
    }

    public void clearPathConsumers() {
        this.pathConsumers.clear();
    }

    public PathSelectionBar(String labelText) {
        this(labelText, 0.4);
    }

    private void applyVisuals() {
        GridBagLayout gbl = new GridBagLayout();
        this.setLayout(gbl);
        GridBagConstraints c;
        // label
        c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = labelWidthRatio;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(10, CopySnapGeometry.BUTTON_HEIGHT));
        label.setFont(CopySnapFonts.LABEL_TEXT_FONT);
        this.add(label, c);
        // text field
        c = new GridBagConstraints();
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        this.add(textField, c);
        textField.setPreferredSize(new Dimension(0, CopySnapGeometry.BUTTON_HEIGHT));
        // button
        c = new GridBagConstraints();
        c.gridx = 2;
        c.weightx = BUTTON_WIDTH_RATIO;
        c.fill = GridBagConstraints.HORIZONTAL;
        button.setPreferredSize(new Dimension(8, CopySnapGeometry.BUTTON_HEIGHT));
        this.add(button, c);
    }

    private void showBrowsingDialog() {
        CopySnapFileChooser fc = new CopySnapFileChooser();
        Path startingPath;
        try {
            startingPath = fixRootPath == null ? Path.of(textField.getText()) : fixRootPath;
        } catch (InvalidPathException e) {
            startingPath = Path.of(System.getProperty("user.home"));
        }
        Path chosenPath = fc.chooseAPathWithDialog("Choose a directory", this, startingPath.toFile());
        if (chosenPath != null) {
            validateAndSetTextFieldValue(chosenPath);
        }
    }

    public JTextField getTextField() {
        return textField;
    }

    /**
     * @return length of string contained in this bar's text field
     */
    public int getTextLength() {
        return textField.getText().length();
    }

    /**
     * @return {@code Path.of(textField.getText())}
     */
    public Path getPlainPath() {
        return Path.of(textField.getText());
    }

    /**
     * Returns the path stored within this PathSelectionBar's text field. If this path does not point to a directory on this filesystem,
     * an {@link java.nio.file.InvalidPathException} is thrown.
     */
    public Path getPath() throws InvalidPathSelectionException {
        return getPath(false);
    }

    /**
     * Returns the path stored within this PathSelectionBar's text field.
     */
    public Path getPath(boolean allowSimpleFiles) throws InvalidPathSelectionException {
        Path out;
        if (!textField.getText().isBlank()) {
            try {
                out = Path.of(textField.getText());
            } catch (InvalidPathException e) {
                throw new InvalidPathSelectionException("Path from text field could not tbe translated to a valid Path object: " + textField.getText() + ", " + e);
            }
        } else {
            out = null;
        }
        if (out == null || !Files.exists(out) || (Files.isRegularFile(out) && !allowSimpleFiles)) {
            throw new InvalidPathSelectionException("Path from text field is invalid, does not point to an existing file or points to a regular file: " + textField.getText());
        }
        return out;
    }

    public String getPlainTextContent() {
        return textField.getText();
    }

    /**
     * Sets this containers text field to the given value after validating the input against this containers fixed root path.
     * If a fixed root path was registered for this container, the given text string must be translatable to a valid path on
     * this filesystem and be a child path of that fixed root path.
     */
    public void validateAndSetTextFieldValue(String text) {
        setTextFieldValue(getValidatedTextFieldValueFromString(text));
    }

    /**
     * @see #validateAndSetTextFieldValue(String)
     */
    public void validateAndSetTextFieldValue(Path path) {
        setTextFieldValue(getValidatedTextFieldValueFromPath(path));
    }

    /**
     * Sets this text fields value to the given string and sends the string, if it can be translated to a valid Path-Object, to the attached consumer.
     * @param pathString
     */
    private void setTextFieldValue(String pathString) {
        this.textField.setText(pathString);
    }


    /**
     * Validates the input text. If the input text is null or valid text is returned. Otherwise, the registered next valid parent path of the given text is returned.
     * The validation consists of checking, the following points:
     * <p>
     *     1. Is text a valid path on this filesystem.<br>
     *     2. The path points to a file on this filesystem<br>
     *     3. If there is a fixRootPath registered, the given text denotes a child path of that root path.
     * </p>
     */
    public String getValidatedTextFieldValueFromString(String text) {
        if (text == null) {
            return null;
        }
        // check for valid path
        Path path;
        try {
            path = Path.of(text);
        } catch (InvalidPathException e1) {
            if(fixRootPath == null) {
                try {
                    path = Path.of(textField.getText());
                } catch (InvalidPathException e2) {
                    path = null;
                }
            } else {
                path =  fixRootPath;
            }
        }
        return getValidatedTextFieldValueFromPath(path);
    }

    /**
     * Returns the String value from the given path. The path is validated against the registered fixRootPath of this
     * container. If there is no such root path registered, this method acts like {@code Path.toString}.
     * Otherwise, this method checks, if the given path is a child path of the registered root path. If that is true, {@code pathString.toString()} is returned, else
     * it returns the string value of the registered root path.
     */
    public String getValidatedTextFieldValueFromPath(Path path) {
        if (path == null) {
            return null;
        }
        String out;
        if (fixRootPath == null || (path.startsWith(fixRootPath))) {
            // there is no root path OR (there is a root path registered and it is a parent of the given path)
            if(Files.exists(path)) {
                out = path.toString();
            } else {
                out = getValidatedTextFieldValueFromPath(path.getParent());
            }
        } else {
            out = fixRootPath.toString();
        }
        return out;
    }

    public List<Consumer<Path>> getPathConsumers() {
        return pathConsumers;
    }

}

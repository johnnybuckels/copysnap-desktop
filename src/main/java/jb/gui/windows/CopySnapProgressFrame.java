package jb.gui.windows;

import jb.gui.constants.CopySnapFonts;
import jb.gui.constants.CopySnapGeometry;

import javax.swing.*;
import java.awt.*;

public class CopySnapProgressFrame extends JFrame {

    private final static Dimension PREFERRED_DIMENSION = new Dimension(600, 240);
    private final static int PROGRESSBAR_PREFERRED_WIDTH = 540;

    private final JLabel label = new JLabel();
    private final JLabel additionalLabel = new JLabel();

    public CopySnapProgressFrame(JProgressBar progressBar, JButton cancelButton, String primaryLabelText) throws HeadlessException {
        super();
        setLayout(new GridBagLayout());
        label.setFont(CopySnapFonts.LABEL_TEXT_FONT);
        label.setText(primaryLabelText);
        arrangeContentsAndShow(progressBar, cancelButton);
    }

    public void setAdditionalLabelText(String text) {
        additionalLabel.setText(text);
    }

    private void arrangeContentsAndShow(JProgressBar progressBar, JButton cancelButton) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(10, 10, 5, 10);
        add(label, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(5, 10, 5, 10);
        add(additionalLabel, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 10, 10, 10);
        add(progressBar, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        add(cancelButton, c);

        setTitle(this.getClass().getSimpleName());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        progressBar.setPreferredSize(new Dimension(PROGRESSBAR_PREFERRED_WIDTH, CopySnapGeometry.BUTTON_HEIGHT));
        setPreferredSize(PREFERRED_DIMENSION);
        pack();
        setLocationRelativeTo(null);
    }
}

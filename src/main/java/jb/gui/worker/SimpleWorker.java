package jb.gui.worker;

import jb.gui.constants.CopySnapGeometry;
import jb.gui.exceptions.CopySnapException;
import jb.gui.windows.CopySnapProgressFrame;
import jb.gui.windows.listeners.SubWindowListener;

import javax.swing.*;
import java.awt.*;

/**
 * Class for executing any job returning void displaying a progress bar.
 */
@Deprecated
public class SimpleWorker extends SwingWorker<Void, Void> {

    private final Runnable jobToDo;
    private final Runnable afterDoneJob;
    private final JFrame progressFrame;
    private final String jobName;

    private Exception encounteredException = null;

    /**
     * Creates a Worker that can execute the given job in a background task while showing an indefinite progress bar.
     */
    public SimpleWorker(Runnable jobToDo, String jobName, Runnable afterDoneJob) {
        this.jobToDo = jobToDo;
        this.jobName = jobName;
        this.afterDoneJob = afterDoneJob;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(action -> this.cancelTaskAndDissolvePanel());
        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(300, CopySnapGeometry.BUTTON_HEIGHT));
        // progress bar
        progressBar.setIndeterminate(true);
        progressFrame = new CopySnapProgressFrame(progressBar, cancelButton, jobName);
        progressFrame.setTitle(jobName);
    }

    private void arrangeContentsAndShow() {
        progressFrame.addWindowListener(new SubWindowListener(() -> cancel(true)));
        progressFrame.setVisible(true);
    }

    @Override
    public Void doInBackground() {
        try {
            jobToDo.run();
        } catch (Exception e) {
            encounteredException = e;
        }
        return null;
    }

    @Override
    protected void done() {
        afterDoneJob.run();
        cancelTaskAndDissolvePanel();
        if(encounteredException != null) {
            throw new CopySnapException("Could not complete job " + jobName + ": " + encounteredException, encounteredException);
        }
    }

    public void showAndExecute() {
        arrangeContentsAndShow();
        this.execute();
    }

    private void cancelTaskAndDissolvePanel() {
        cancel(true);
        progressFrame.dispose();
    }

}

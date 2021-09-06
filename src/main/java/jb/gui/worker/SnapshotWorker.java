package jb.gui.worker;

import jb.engine.core.Context;
import jb.engine.reporting.CopyProgress;
import jb.engine.reporting.ProblemReport;
import jb.gui.constants.CopySnapGeometry;
import jb.gui.exceptions.CopySnapReportException;
import jb.gui.windows.CopySnapProgressFrame;
import jb.gui.windows.listeners.SubWindowListener;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

/**
 * Class for executing creating snapshot on a given context displaying a progress bar.
 */
@Deprecated
public class SnapshotWorker extends SwingWorker<ProblemReport, CopyProgress> {

    private static final String ANALYZED_FILE_COUNT_STRING_TEMPLATE = "Analyzed files: %s (%s regular files, %s directories)";
    private static final int PREFERRED_PROGRESS_BAR_WIDTH = 400;

    private final Context context;
    private final String runName;

    private final CopySnapProgressFrame copySnapProgressFrame;
    private final JProgressBar progressBar = new JProgressBar();
    private final JButton cancelButton= new JButton("Cancel");
    private final Runnable externalDoneRunnable;

    public SnapshotWorker(Context context, String runName, Runnable externalDoneRunnable) {
        this.context = context;
        this.runName = runName;
        this.externalDoneRunnable = externalDoneRunnable;
        cancelButton.addActionListener(action -> this.cancelTaskAndDissolvePanel());
        // progress bar
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
        progressBar.setPreferredSize(new Dimension(PREFERRED_PROGRESS_BAR_WIDTH, CopySnapGeometry.BUTTON_HEIGHT));
        copySnapProgressFrame = new CopySnapProgressFrame(progressBar, cancelButton, "Creating snapshot " + runName + "...");
        copySnapProgressFrame.setTitle("Creating Snapshot");
    }

    private void arrangeProgressPanelAndShow() {
        copySnapProgressFrame.addWindowListener(new SubWindowListener(() -> cancel(true)));
        copySnapProgressFrame.setVisible(true);
    }

    @Override
    protected void process(List<CopyProgress> chunks) {
        chunks.stream()
                .sorted(Comparator.comparingLong(CopyProgress::getProcessedCount))
                .forEach(copyProgress -> {
                    copySnapProgressFrame.setAdditionalLabelText(String.format(ANALYZED_FILE_COUNT_STRING_TEMPLATE,
                            copyProgress.getTotalFileCount(), copyProgress.getTrueFileCount(), copyProgress.getDirectoryCount()
                            ));
                    progressBar.setValue(copyProgress.getPercentage().intValue());
                });
    }

    @Override
    public ProblemReport doInBackground() {
        return context.snapshotAndSave(runName, this::publish);
    }

    @Override
    protected void done() {
        ProblemReport report;
        try {
            report = get();
            if(report.getEncounteredProblemCount() > 0) {
                throw new CopySnapReportException("Encountered problems", report);
            }
        } catch (Exception e) {
            throw new CopySnapReportException("Could not retrieve Report: " + e, e);
        } finally {
            cancelTaskAndDissolvePanel();
            externalDoneRunnable.run();
        }
    }

    public void showAndExecute() {
        arrangeProgressPanelAndShow();
        this.execute();
    }

    private void cancelTaskAndDissolvePanel() {
        cancel(true);
        copySnapProgressFrame.dispose();
    }

}

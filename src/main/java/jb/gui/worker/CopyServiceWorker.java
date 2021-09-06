package jb.gui.worker;

import jb.engine.reporting.CopyProgress;
import jb.engine.reporting.ProblemReport;
import jb.gui.constants.CopySnapGeometry;
import jb.gui.exceptions.CopySnapException;
import jb.gui.exceptions.CopySnapReportException;
import jb.gui.windows.CopySnapProgressFrame;
import jb.gui.windows.listeners.SubWindowListener;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class for executing any job returning a {@link ProblemReport} on a given context displaying a progress bar.
 */
@Deprecated
public class CopyServiceWorker extends SwingWorker<ProblemReport, CopyProgress>{

    // TODO: replace occurrences with BackgroundWorker

    private static final String ANALYZED_FILE_COUNT_STRING_TEMPLATE = "Analyzed files: %s (%s regular files, %s directories)";
    private static final int PREFERRED_PROGRESS_BAR_WIDTH = 400;

    private final Function<Consumer<CopyProgress>, ProblemReport> jobToDoWithIntermediateConsumer;
    private final boolean showIntermediateResults;
    private final String jobName;
    private final CopySnapProgressFrame copySnapProgressFrame;
    private final Runnable externalDoneRunnable;

    /**
     * Creates a Worker that can execute the given job in a background task while showing an <b>indefinite</b> progress bar.
     *
     * <p>The given job is of the form 'Consumer<CopyProgress> -> ProblemReport', where the given argument is a function consuming
     * a {@link CopyProgress} which is called whenever progress to the actual job that is producing the ProblemReport is made.
     * </p>
     * The external runnable is called when the job has been done, regardless of the result or exit reason.
     */
    public CopyServiceWorker(Function<Consumer<CopyProgress>, ProblemReport> jobToDoWithIntermediateConsumer, String jobName, Runnable externalDoneRunnable) {
        this.jobToDoWithIntermediateConsumer = jobToDoWithIntermediateConsumer;
        this.showIntermediateResults = true;
        this.jobName = jobName;
        this.externalDoneRunnable = externalDoneRunnable;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(action -> this.cancelTaskAndDissolvePanel());
        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(PREFERRED_PROGRESS_BAR_WIDTH, CopySnapGeometry.BUTTON_HEIGHT));
        // progress bar
        progressBar.setIndeterminate(true);
        copySnapProgressFrame = new CopySnapProgressFrame(progressBar, cancelButton, jobName);
        copySnapProgressFrame.setTitle(jobName);
    }

    /**
     * Creates a Worker that can execute the given job in a background task while showing an <b>indefinite</b> progress bar.
     * The external runnable is called when the job has been done, regardless of the result or exit reason.
     */
    public CopyServiceWorker(Supplier<ProblemReport> jobToDo, String jobName, Runnable externalDoneRunnable) {
        this.jobToDoWithIntermediateConsumer = copyProgressConsumer -> jobToDo.get();  // no intermediate consumer: ignore the argument of Function<Consumer<CopyProgress>, ProblemReport> jobToDo.
        this.showIntermediateResults = false;
        this.jobName = jobName;
        this.externalDoneRunnable = externalDoneRunnable;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(action -> this.cancelTaskAndDissolvePanel());
        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(300, CopySnapGeometry.BUTTON_HEIGHT));
        // progress bar
        progressBar.setIndeterminate(true);
        copySnapProgressFrame = new CopySnapProgressFrame(progressBar, cancelButton, jobName);
        copySnapProgressFrame.setTitle(jobName);
    }

    public CopyServiceWorker(Supplier<ProblemReport> jobToDoWithIntermediateConsumer, String jobName) {
        this(jobToDoWithIntermediateConsumer, jobName, () -> {});
    }

    private void arrangeContentsAndShow() {
        copySnapProgressFrame.addWindowListener(new SubWindowListener(() -> cancel(true)));
        copySnapProgressFrame.setVisible(true);
    }

    @Override
    public ProblemReport doInBackground() {
        return jobToDoWithIntermediateConsumer.apply(this::publish);
    }

    @Override
    protected void process(List<CopyProgress> chunks) {
        if(!showIntermediateResults) {
            return;
        }
        chunks.stream()
                .sorted(Comparator.comparingLong(CopyProgress::getProcessedCount))
                .forEach(copyProgress -> copySnapProgressFrame.setAdditionalLabelText(String.format(ANALYZED_FILE_COUNT_STRING_TEMPLATE,
                        copyProgress.getTotalFileCount(), copyProgress.getTrueFileCount(), copyProgress.getDirectoryCount()
                )));
    }

    @Override
    protected void done() {
        ProblemReport report;
        try {
            report = get();
            if(report == null) {
                throw new CopySnapReportException("The returned report of job " + jobName + " was null");
            }
            if(report.getEncounteredProblemCount() > 0) {
                throw new CopySnapReportException("Encountered problems while executing job " + jobName, report);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CopySnapException("Could not retrieve report from job " + jobName + ": " + e, e);
        } finally {
            cancelTaskAndDissolvePanel();
            externalDoneRunnable.run();
        }
    }

    public void showAndExecute() {
        arrangeContentsAndShow();
        this.execute();
    }

    private void cancelTaskAndDissolvePanel() {
        cancel(true);
        copySnapProgressFrame.dispose();
    }

}

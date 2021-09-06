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

/**
 * Class for executing any job returning a {@link ProblemReport}. It is also able to consume intermediate results in the
 * form of {@link CopyProgress}.<br>
 * This class is designed to <b>replace</b> all other Worker-Classes.
 */
public class BackgroundWorker extends SwingWorker<ProblemReport, CopyProgress> {

    private static final int PREFERRED_PROGRESS_BAR_WIDTH = 400;

    private final Function<Consumer<CopyProgress>, ProblemReport> jobToDoWithIntermediateConsumer;
    private final boolean showIntermediateResults;
    private final String jobName;
    private final Runnable externalDoneRunnable;
    private final List<Function<CopyProgress, Object>> valueGetter;
    private final Function<CopyProgress, Integer> progressFunction;
    private final String stringMessageTemplate;

    private CopySnapProgressFrame copySnapProgressFrame;
    private JProgressBar progressBar;

    public static BackgroundWorkerBuilder builder() {
        return new BackgroundWorkerBuilder();
    }

    /**
     * Creates a Worker that can execute the given job in a background task while showing an <b>indefinite</b> progress bar.
     *
     * <p>The given job is of the form 'Consumer<CopyProgress> -> ProblemReport', where the given argument is a function consuming
     * a {@link CopyProgress} which is called whenever progress to the actual job that is producing the ProblemReport is made.
     * </p>
     * The external runnable is called when the job has been done, regardless of the result or exit reason.
     */
    protected BackgroundWorker(Function<Consumer<CopyProgress>, ProblemReport> jobToDoWithIntermediateConsumer,
                            String jobName,
                            Runnable externalDoneRunnable,
                            boolean showIntermediateResults,
                            String stringMessageTemplate,
                            List<Function<CopyProgress, Object>> valueGetter,
                            Function<CopyProgress, Integer> progressFunction
    ) {
        this.jobToDoWithIntermediateConsumer = jobToDoWithIntermediateConsumer;
        this.showIntermediateResults = showIntermediateResults;
        this.jobName = jobName;
        this.externalDoneRunnable = externalDoneRunnable;
        this.stringMessageTemplate = stringMessageTemplate;
        this.valueGetter = valueGetter;
        this.progressFunction = progressFunction;
        arrangeContents();
    }

    private void arrangeContents() {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(action -> this.cancelTaskAndDissolvePanel());
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(PREFERRED_PROGRESS_BAR_WIDTH, CopySnapGeometry.BUTTON_HEIGHT));
        // progress bar
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setIndeterminate(progressFunction == null);
        copySnapProgressFrame = new CopySnapProgressFrame(progressBar, cancelButton, jobName + "...");
        copySnapProgressFrame.setTitle(jobName);
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
                .forEach(copyProgress -> {
                            copySnapProgressFrame.setAdditionalLabelText(
                                    String.format(stringMessageTemplate, valueGetter.stream().map(getter -> getter.apply(copyProgress)).toArray())
                            );
                            if(progressFunction != null) {
                                progressBar.setValue(progressFunction.apply(copyProgress));
                            }
                        }
                );
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
        copySnapProgressFrame.addWindowListener(new SubWindowListener(() -> cancel(true)));
        copySnapProgressFrame.setVisible(true);
        this.execute();
    }

    private void cancelTaskAndDissolvePanel() {
        cancel(true);
        copySnapProgressFrame.dispose();
    }

}

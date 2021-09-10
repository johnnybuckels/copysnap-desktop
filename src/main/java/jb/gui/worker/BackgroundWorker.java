package jb.gui.worker;

import jb.engine.reporting.CopyProgress;
import jb.engine.reporting.ProblemReport;
import jb.gui.constants.CopySnapGeometry;
import jb.gui.exceptions.CopySnapException;
import jb.gui.windows.CopySnapProgressFrame;
import jb.gui.windows.listeners.SubWindowListener;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class for executing any job returning a {@link ProblemReport}. It is also able to consume intermediate results in the
 * form of {@link CopyProgress}.<br>
 * This class is designed to <b>replace</b> all other Worker-Classes.
 * @param <T> return value type of the job executed by this worker
 * @param <U> intermediate return value type of the job executed by this worker
 */
public class BackgroundWorker<T, U> extends SwingWorker<T, U> {

    private static final int PREFERRED_PROGRESS_BAR_WIDTH = 400;

    private final Function<Consumer<U>, T> jobToDoWithIntermediateConsumer;
    private final boolean showIntermediateResults;
    private final String jobName;
    private final Consumer<T> resultConsumer;
    private final Runnable doneRunnable;
    private final List<Function<U, Object>> valueGetter;
    private final Function<U, Integer> progressFunction;
    private final String stringMessageTemplate;

    private CopySnapProgressFrame copySnapProgressFrame;
    private JProgressBar progressBar;

    // ----- Builder

    /**
     * Background job to do.
     */
    public static BackgroundWorkerBuilder<Void, Void> builderForJob(Runnable job) {
        return new BackgroundWorkerBuilder<>(copyProgressConsumer -> {
                    job.run();
                    return null;
                },
                Void.class
        );
    }

    /**
     * Background job to execute resulting in some object.
     */
    public static <X> BackgroundWorkerBuilder<X, Void> builderForJob(Supplier<X> job) {
        return new BackgroundWorkerBuilder<>(copyProgressConsumer -> job.get(), Void.class);
    }

    /**
     * The given job is of the form 'Consumer<CopyProgress> -> ProblemReport', where the given argument is a function consuming
     * a {@link CopyProgress} which is called whenever progress to the actual job that is producing the result is made.
     */
    public static <X, Y> BackgroundWorkerBuilder<X, Y> builderForJob(Function<Consumer<Y>, X> jobToDoWithIntermediateConsumer, Class<Y> intermediateResultType) {
        return new BackgroundWorkerBuilder<>(jobToDoWithIntermediateConsumer, intermediateResultType);
    }

    /**
     * Creates a Worker that can execute the given job in a background task while showing an <b>indefinite</b> progress bar.
     *
     * <p>The given job is of the form 'Consumer<CopyProgress> -> ProblemReport', where the given argument is a function consuming
     * a {@link CopyProgress} which is called whenever progress to the actual job that is producing the ProblemReport is made.
     * </p>
     * The done runnable is called when the job has been done, regardless of the result or exit reason.
     */
    protected BackgroundWorker(Function<Consumer<U>, T> jobToDoWithIntermediateConsumer,
                               String jobName,
                               Consumer<T> resultConsumer,
                               Runnable doneRunnable,
                               boolean showIntermediateResults,
                               String stringMessageTemplate,
                               List<Function<U, Object>> valueGetter,
                               Function<U, Integer> progressFunction
    ) {
        this.jobToDoWithIntermediateConsumer = jobToDoWithIntermediateConsumer;
        this.jobName = jobName;
        this.resultConsumer = resultConsumer;
        this.doneRunnable = doneRunnable;
        this.showIntermediateResults = showIntermediateResults;
        this.stringMessageTemplate = stringMessageTemplate;
        this.valueGetter = valueGetter;
        this.progressFunction = progressFunction;
        arrangeContents();
    }

    // ----- Functionality

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
    public T doInBackground() {
        return jobToDoWithIntermediateConsumer.apply(this::publish);
    }

    @Override
    protected void process(List<U> chunks) {
        if (!showIntermediateResults) {
            return;
        }
        chunks.forEach(chunk -> {
                    copySnapProgressFrame.setAdditionalLabelText(
                            String.format(stringMessageTemplate, valueGetter.stream().map(getter -> getter.apply(chunk)).toArray())
                    );
                    if (progressFunction != null) {
                        progressBar.setValue(progressFunction.apply(chunk));
                    }
                }
        );
    }

    @Override
    protected void done() {
        T result;
        try {
            result = get();
            resultConsumer.accept(result);
        } catch (InterruptedException | ExecutionException e) {
            throw new CopySnapException("Could not retrieve report from job " + jobName + ": " + e, e);
        } finally {
            cancelTaskAndDissolvePanel();
            doneRunnable.run();
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

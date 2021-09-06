package jb.gui.worker;

import jb.engine.reporting.CopyProgress;
import jb.engine.reporting.ProblemReport;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class for executing any job returning a {@link ProblemReport}. It is also able to consume intermediate results in the
 * form of {@link CopyProgress}.<br>
 * This class is designed to <b>replace</b> all other Worker-Classes.
 */
public class BackgroundWorkerBuilder {

    private Function<Consumer<CopyProgress>, ProblemReport> jobToDoWithIntermediateConsumer;
    private boolean showIntermediateResults = false;
    private String jobName = "CopySnap";
    private Runnable externalDoneRunnable = () ->{};
    private String messageTemplate = "";
    private List<Function<CopyProgress, Object>> valueGetter = List.of();
    private Function<CopyProgress, Integer> progressFunction = null;


    protected BackgroundWorkerBuilder() {}

    /**
     * Name of the job that should be executed in the background.
     */
    public BackgroundWorkerBuilder withJobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    /**
     * If true, intermediate CopyProgress instances will be evaluated to display a progressbar or a text message.
     */
    public BackgroundWorkerBuilder showIntermediateResults(boolean showIntermediateResults) {
        this.showIntermediateResults = showIntermediateResults;
        return this;
    }

    /**
     * Background job to do.
     */
    public BackgroundWorkerBuilder withJob(Runnable job) {
        this.jobToDoWithIntermediateConsumer = copyProgressConsumer -> {
            job.run();
            return new ProblemReport(0);
        };
        return this;
    }

    /**
     * Background job to execute resulting in a ProblemReport.
     */
    public BackgroundWorkerBuilder withJob(Supplier<ProblemReport> job) {
        this.jobToDoWithIntermediateConsumer = copyProgressConsumer -> job.get();
        return this;
    }

    /**
     * <p>The given job is of the form 'Consumer<CopyProgress> -> ProblemReport', where the given argument is a function consuming
     * a {@link CopyProgress} which is called whenever progress to the actual job that is producing the ProblemReport is made.
     */
    public BackgroundWorkerBuilder withJob(Function<Consumer<CopyProgress>, ProblemReport> jobToDoWithIntermediateConsumer) {
        this.jobToDoWithIntermediateConsumer = jobToDoWithIntermediateConsumer;
        return this;
    }

    /**
     * The external runnable is called when the job has been done, regardless of the result or exit reason.
     */
    public BackgroundWorkerBuilder withDoneRunnable(Runnable externalDoneRunnable) {
        this.externalDoneRunnable = externalDoneRunnable;
        return this;
    }

    /**
     * The String message template and an appropriate number of CopyProgress related getter methods to supply values.
     * Each such value will be set into the "%s" string parts of the message in the given order.
     */
    public BackgroundWorkerBuilder withStringMessage(String messageTemplate, List<Function<CopyProgress, Object>> valueGetter) {
        this.messageTemplate = messageTemplate;
        this.valueGetter = valueGetter;
        return this;
    }

    /**
     * The function used to fill a progress bar. If set to {@code null} (the default), an indefinite progress bar will be displayed.
     */
    public BackgroundWorkerBuilder withProgressFunction(Function<CopyProgress, Integer> progressFunction) {
        this.progressFunction = progressFunction;
        return this;
    }

    public BackgroundWorker build() {
        return new BackgroundWorker(jobToDoWithIntermediateConsumer, jobName, externalDoneRunnable, showIntermediateResults, messageTemplate, valueGetter, progressFunction);
    }



}

package jb.gui.worker;

import jb.engine.reporting.CopyProgress;
import jb.engine.reporting.ProblemReport;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class for executing any job returning a {@link ProblemReport}. It is also able to consume intermediate results in the
 * form of {@link CopyProgress}.<br>
 * This class is designed to <b>replace</b> all other Worker-Classes.
 */
public class BackgroundWorkerBuilder<T, U> {

    private final Function<Consumer<U>, T> jobToDoWithIntermediateConsumer;
    /**
     * only used in order to be able to pass a lambda expression as jobToDoWithIntermediateConsumer since otherwise the type U of the consumer (the argument of the given lambda)
     * is generic.
     */
    private final Class<U> intermediateResultType;
    private boolean showIntermediateResults = false;
    private String jobName = "CopySnap";
    private Runnable doneRunnable = () -> {};
    private Consumer<T> resultConsumer = t -> {};
    private String messageTemplate = "";
    private List<Function<U, Object>> valueGetter = List.of();
    private Function<U, Integer> progressFunction = null;


    protected BackgroundWorkerBuilder(Function<Consumer<U>, T> jobToDoWithIntermediateConsumer, Class<U> intermediateResultType) {
        this.jobToDoWithIntermediateConsumer = jobToDoWithIntermediateConsumer;
        this.intermediateResultType = intermediateResultType;
    }

    /**
     * Name of the job that should be executed in the background.
     */
    public BackgroundWorkerBuilder<T, U> withJobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    /**
     * If true, intermediate CopyProgress instances will be evaluated to display a progressbar or a text message.
     */
    public BackgroundWorkerBuilder<T, U> showIntermediateResults(boolean showIntermediateResults) {
        this.showIntermediateResults = showIntermediateResults;
        return this;
    }

    /**
     * The external runnable is called when the job has been done, regardless of the result or exit reason.
     */
    public BackgroundWorkerBuilder<T, U> withDoneRunnable(Runnable externalDoneRunnable) {
        this.doneRunnable = externalDoneRunnable;
        return this;
    }

    /**
     * The consumer that is called when the job has been successfully completed.
     */
    public BackgroundWorkerBuilder<T, U> withResultConsumer(Consumer<T> resultConsumer) {
        this.resultConsumer = resultConsumer;
        return this;
    }

    /**
     * The String message template and an appropriate number of CopyProgress related getter methods to supply values.
     * Each such value will be set into the "%s" string parts of the message in the given order.
     */
    public BackgroundWorkerBuilder<T, U> withStringMessage(String messageTemplate, List<Function<U, Object>> valueGetter) {
        this.messageTemplate = messageTemplate;
        this.valueGetter = valueGetter;
        return this;
    }

    /**
     * The function used to fill a progress bar. If set to {@code null} (the default), an indefinite progress bar will be displayed.
     * The progress is measured in a percentage int value ranging from 0 to 100. Ensure that the given percentage method always provides
     * an integer value from 0 to 100. Values outside this range will be rounded down or up respectively.
     */
    public BackgroundWorkerBuilder<T, U> withProgressFunction(Function<U, Integer> progressFunction) {
        this.progressFunction = progressFunction;
        return this;
    }

    public BackgroundWorker<T, U> build() {
        return new BackgroundWorker<>(jobToDoWithIntermediateConsumer, jobName, resultConsumer, doneRunnable, showIntermediateResults, messageTemplate, valueGetter, progressFunction);
    }



}

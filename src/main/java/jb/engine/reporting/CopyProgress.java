package jb.engine.reporting;

import jb.engine.utils.GeneralUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

/**
 * Class to represent the current progress status of a job being executed (for example using {@link jb.engine.services.CopyService}).
 */
public class CopyProgress {

    /**
     * Consumer that is notified whenever a change is made to one of these properties.
     */
    private final Consumer<CopyProgress> updateConsumer;
    private Long totalFileCount = 0L;
    private Long trueFileCount = 0L;
    private Long directoryCount = 0L;
    private Long processedCount = 0L;

    private String name;

    /**
     * Initializes the copy progress with 0 as the initial amount of files.
     * @param updateConsumer a consumer that is called with this CopyProgress whenever an update occurs.
     */
    public static CopyProgress withProgressConsumer(Consumer<CopyProgress> updateConsumer) {
        return new CopyProgress(GeneralUtils.getNowAsId(), updateConsumer);
    }

    /**
     * Initializes the copy progress with 0 as the initial amount of files.
     */
    public static CopyProgress withoutConsumer() {
        return new CopyProgress(GeneralUtils.getNowAsId(), cp -> {});
    }

    private CopyProgress(String name, Consumer<CopyProgress> updateConsumer) {
        this.name = name;
        this.updateConsumer = updateConsumer;
    }

    /**
     * Increase the total analyzed directory count of this CopyProgress by one and notify the attached Consumer.
     */
    public void increaseDirectoryCountAndNotify() {
        directoryCount += 1;
        totalFileCount += 1;
        updateConsumer.accept(this);
    }

    /**
     * Increase the total analyzed (true) file count of this CopyProgress by one and notify the attached Consumer.
     */
    public void increaseTrueFileCountAndNotify() {
        trueFileCount += 1;
        totalFileCount += 1;
        updateConsumer.accept(this);
    }

    /**
     * Increase the total processed file count of this CopyProgress by one and notify the attached Consumer.
     */
    public void increaseProcessedFileCountAndNotify() {
        processedCount += 1;
        updateConsumer.accept(this);
    }

    // ---------- Calculations

    /**
     * @return The percentage of so far processed files. If the initial file number was not defined or is negative, this method returns {@code null}.
     */
    public BigDecimal getPercentage() {
        if(totalFileCount == null || totalFileCount < 0) {
            return null;
        }
        return BigDecimal.valueOf((processedCount.doubleValue() / totalFileCount.doubleValue()) * 100.0).setScale(2, RoundingMode.HALF_DOWN);
    }

    // ---------- Getter


    public String getName() {
        return name;
    }

    public Long getTotalFileCount() {
        return totalFileCount;
    }

    public Long getProcessedCount() {
        return processedCount;
    }

    public Long getTrueFileCount() {
        return trueFileCount;
    }

    public Long getDirectoryCount() {
        return directoryCount;
    }

    // ---------- Setter

    public void setName(String name) {
        this.name = name;
    }
}

package jb.gui.exceptions;

import jb.engine.reporting.ProblemReport;

public class CopySnapReportException extends RuntimeException {

    private ProblemReport problemReport = null;

    public CopySnapReportException(String message, ProblemReport problemReport) {
        super(message);
        this.problemReport = problemReport;
    }

    public CopySnapReportException(String message) {
        super(message);
        this.problemReport = new ProblemReport(0);
    }

    public CopySnapReportException(String message, Throwable cause) {
        super(message, cause);
        this.problemReport = new ProblemReport(0);

    }

    public ProblemReport getProblemReport() {
        return problemReport;
    }
}

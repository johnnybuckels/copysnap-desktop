package jb.gui.exceptions;

import jb.gui.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final static int MAX_PROBLEM_LINES = 1000;

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if(throwable instanceof CopySnapReportException) {
            handleException(thread, (CopySnapReportException) throwable);
        } else if(throwable instanceof NullPointerException) {
            handleException(thread, (NullPointerException) throwable);
        } else {
            handleException(thread, throwable);
        }
        throwable.printStackTrace();
    }

    private void handleException(Thread t, CopySnapReportException e) {
        List<String> problemStrings = new ArrayList<>();
        problemStrings.add(e.getMessage());
        e.getProblemReport().getProblems()
                .stream()
                .limit(MAX_PROBLEM_LINES)
                .forEach(p -> problemStrings.add(p.toString()));
        MessageUtils.showErrorTextMessage(
                null,
                String.join("\n", problemStrings),
                t.getName() + ": " + e.getClass().getName()
        );
    }

    private void handleException(Thread t, NullPointerException e) {
        List<String> stackTrace = Arrays.stream(e.getStackTrace()).limit(MAX_PROBLEM_LINES).map(StackTraceElement::toString).collect(Collectors.toList());
        MessageUtils.showErrorTextMessage(
                null,
                String.join(System.lineSeparator(), stackTrace),
                t.getName() + ": " + e.getClass().getName()
        );
    }

    private void handleException(Thread t, Throwable e) {
        MessageUtils.showErrorTextMessage(
                null,
                e.getMessage(),
                t.getName() + ": " + e.getClass().getName()
        );
    }

}

package jb.engine.reporting;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for collecting Problems that occurred during the execution of some task.
 */
public class ProblemReport {
    
    private final int maxProblemsToStore;
    private final List<Problem> problems;
    
    private long encounteredProblemCount;

    public ProblemReport(int maxProblemsToStore) {
        this.maxProblemsToStore = maxProblemsToStore;
        this.problems = new ArrayList<>(maxProblemsToStore);
        encounteredProblemCount = 0;
    }

    public void addProblem(Problem problem) {
        if(encounteredProblemCount < maxProblemsToStore) {
            problems.add(problem);
        }
        encounteredProblemCount++;
    }

    public int getMaxProblemsToStore() {
        return maxProblemsToStore;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public long getEncounteredProblemCount() {
        return encounteredProblemCount;
    }

    public static class Problem {

        private final Path sourcePath;
        private final Path desiredTargetPath;
        private final Exception encounteredException;
        private final String infoText;

        public Problem(Path sourcePath, Path desiredTargetPath, Exception encounteredException, String infoText) {
            this.sourcePath = sourcePath;
            this.desiredTargetPath = desiredTargetPath;
            this.encounteredException = encounteredException;
            this.infoText = infoText;
        }

        @Override
        public String toString() {
            return "sourcePath=" + sourcePath +
                    ", desiredTargetPath=" + desiredTargetPath +
                    ", encounteredException=" + encounteredException +
                    ", infoText='" + infoText;
        }
    }

    public void printReport() {
        // TODO
    }
}

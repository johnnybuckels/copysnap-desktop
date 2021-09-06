package jb.engine.services;

import jb.engine.exceptions.UnresolvableFileException;
import jb.engine.reporting.CopyProgress;
import jb.engine.reporting.ProblemReport;
import jb.engine.utils.PathComparator;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CopyService {

    private final Path targetBasePath;
    private final Path sourceBasePath;

    public static CopyService createCopyService(Path targetBasePath, Path sourceBasePath) {
        return new CopyService(targetBasePath, sourceBasePath);
    }
    private CopyService(Path targetBasePath, Path sourceBasePath) {
        this.targetBasePath = targetBasePath;
        this.sourceBasePath = sourceBasePath;
    }

    public ProblemReport plainCopy() {
        return plainCopyRec(sourceBasePath, new ProblemReport(100), false);
    }

    public ProblemReport plainCopyOverride() {
        return plainCopyRec(sourceBasePath, new ProblemReport(100), true);
    }

    /**
     * Recursively copies all files from {@code currentSourceFilePath} into the target location
     */
    private ProblemReport plainCopyRec(Path currentSourcePath, ProblemReport problemReport, boolean overrideExistingFiles) {
        Path relativeSourcePath;
        Path parentOfSourceBasePath = sourceBasePath.getParent();
        if(parentOfSourceBasePath == null) {
            relativeSourcePath = Path.of("/");
        } else {
            relativeSourcePath = parentOfSourceBasePath.relativize(currentSourcePath);
        }
        if(Files.isDirectory(currentSourcePath)) {
            // be sure the order in which the paths are processed is the same each time.
            try (Stream<Path> dirStream = Files.list(currentSourcePath).sorted(new PathComparator())) {
                try {
                    Files.createDirectory(targetBasePath.resolve(relativeSourcePath));
                } catch(FileAlreadyExistsException e) {
                    // if override is active, delete old dir contents
                    if(overrideExistingFiles) {
                        deleteDirectoryContents(targetBasePath.resolve(relativeSourcePath));
                    } else {
                        throw e;
                    }
                }
                dirStream.forEach(path -> this.plainCopyRec(path, problemReport, overrideExistingFiles));
            } catch(IOException e) {
                // skip this directory
                handleProblem(problemReport, e, currentSourcePath, targetBasePath.resolve(relativeSourcePath), "Tried to perform a plain copy");
            }
        } else {
            try {
                if(overrideExistingFiles) {
                    Files.copy(currentSourcePath, targetBasePath.resolve(relativeSourcePath), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.copy(currentSourcePath, targetBasePath.resolve(relativeSourcePath));
                }
            } catch(IOException e) {
                // skip this file
                handleProblem(problemReport, e, currentSourcePath, targetBasePath.resolve(relativeSourcePath), "Tried to perform a plain copy");
            }
        }
        return problemReport;
    }

    /**
     * <p>This method creates a delta-copy of this classes source path. A delta-copy is file-system similar to the source file
     * system where unchanged files or directories are replaced with symbolic links.</p><br>
     * The following copy policy is implemented:
     * <p>For each source path registered in this copy service the following two conditions are checked:<br>
     *     1. Is the path of this file registered in the comparison checksum-map?<br>
     *     2. If yes, are the checksums identical?
     * </p>
     * <p>In the case that both conditions hold true, a symbolic link to the previously registered and saved comparison-file is created and
     * all possibly contained files are skipped. Otherwise, the respective source-file is copied to the target location - or simply newly created if
     * it is a directory. In the latter case, the copy process will continue normally for all contained files.</p>
     * @param sourceChecksumMap the map of checksums of the directory that is currently being copied
     * @param comparisonChecksumMapInverse the map of checksums used for detecting changed files
     * @param copyProgress the copy progress object to be updated during the execution of this method.
     */
    public ProblemReport createSnapshotCopy(HashMap<Path, ByteBuffer> sourceChecksumMap, HashMap<ByteBuffer, Path> comparisonChecksumMapInverse, CopyProgress copyProgress) {
        ProblemReport problemReport = new ProblemReport(100);
        String currentUnchangedParentDirectoryString = null;
        List<Path> keySetSorted = sourceChecksumMap.keySet().stream().sorted(new PathComparator()).collect(Collectors.toList());

        for(Path currentSourceFilePath : keySetSorted) {
            // increase progress
            copyProgress.increaseProcessedFileCountAndNotify();
            // check if the parent path was unchanged
            if(currentUnchangedParentDirectoryString != null && currentSourceFilePath.toString().startsWith(currentUnchangedParentDirectoryString)) {
                continue;
            }
            // --- Get paths of interest
            // compute the relative path of the current file
            Path relativeSourcePath;
            Path parentOfSourceBasePath = sourceBasePath.getParent();
            if(parentOfSourceBasePath == null) {
                relativeSourcePath = Path.of("/");
            } else {
                relativeSourcePath = parentOfSourceBasePath.relativize(currentSourceFilePath);
            }
            // compute the target file path
            Path targetFilePath = targetBasePath.resolve(relativeSourcePath);
            // --- Check if there were changes and act accordingly
            ByteBuffer sourceFileChecksum = sourceChecksumMap.get(currentSourceFilePath);
            if(comparisonChecksumMapInverse.containsKey(sourceFileChecksum)) {
                if(Files.isDirectory(currentSourceFilePath)) {
                    currentUnchangedParentDirectoryString = currentSourceFilePath.toString();
                }
                try {
                    Path targetOfSymbolicLink = comparisonChecksumMapInverse.get(sourceFileChecksum);
                    Files.createSymbolicLink(targetFilePath, targetOfSymbolicLink);
                } catch (IOException e) {
                    handleProblem(problemReport, e, currentSourceFilePath, targetFilePath, "Tried to set symbolic link");
                }
            } else {
                currentUnchangedParentDirectoryString = null;
                // the file was changed or does not exist in the comparison directory
                if(Files.isDirectory(currentSourceFilePath)) {
                    // source path is a directory: create a new directory at the target location
                    try {
                        Files.createDirectory(targetFilePath);
                    } catch (IOException e) {
                        handleProblem(problemReport, e, currentSourceFilePath, targetFilePath, "Tried to create new directory");
                    }
                } else if (Files.isRegularFile(currentSourceFilePath)) {
                    // source path is a regular file: copy the given file to the target location
                    try (BufferedOutputStream outStream = new BufferedOutputStream(Files.newOutputStream(targetFilePath))) {
                        Files.copy(currentSourceFilePath, outStream);
                    } catch (IOException e) {
                        handleProblem(problemReport, e, currentSourceFilePath, targetFilePath, "Tried to copy file");
                    }
                } else {
                    handleProblem(
                            problemReport,
                            new UnresolvableFileException(currentSourceFilePath),
                            currentSourceFilePath,
                            targetFilePath,
                            "File was not a directory and not a regular file"
                    );
                }
            }
        }
        return problemReport;
    }

    /**
     * Creates a new Problem and stores it in the handed Problemreport.
     */
    private void handleProblem(ProblemReport problemReport, Exception e, Path sourcePath, Path desiredTargetPath, String infoText) {
        System.out.println(e.toString());
        problemReport.addProblem(new ProblemReport.Problem(sourcePath, desiredTargetPath, e, infoText));
    }

    /**
     * Deletes contents of the directory. The directory will be empty after this method call succeeded.
     * @param dirPath the path to the directory on this filesystem.
     * @throws IOException if the file could not be deleted
     */
    private void deleteDirectoryContents(Path dirPath) throws IOException {
        if(!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Can only delete contents of a directory but " + dirPath + " is not a directory on this file system");
        }
        Files.walk(dirPath).skip(1).sorted(Comparator.reverseOrder()).forEach(p -> {
            try {
                Files.delete(p);
            } catch (IOException exception) {
                throw new UncheckedIOException("Could not delete file at " + dirPath, exception);
            }
        });
    }
}

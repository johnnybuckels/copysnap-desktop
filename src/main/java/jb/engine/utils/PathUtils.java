package jb.engine.utils;

import jb.engine.core.data.DatabaseManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Collection of utility functions concerning Path objects that are important to CopySnap.
 */
public class PathUtils {
    /**
     * Finds the Path to the directory where the currently executed .jar-File is located. If this application is not run from a .jar file, it returns
     * the "target" directory where this compiled class file is located.
     */
    public static Path findApplicationBasePath() {
        String pathString = DatabaseManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        return Path.of(pathString).getParent();
    }

    /**
     * Deletes the file or directory including its contents at the given path.
     * @param fileOrDirPath the path to the file or directory on this filesystem.
     * @throws IOException if the file could not be deleted
     */
    public static void deleteFileOrDirectory(Path fileOrDirPath) throws IOException {
        if(Files.isRegularFile(fileOrDirPath)) {
            Files.deleteIfExists(fileOrDirPath);
            return;
        }
        Files.walk(fileOrDirPath).sorted(Comparator.reverseOrder()).forEach(p -> {
            try {
                Files.delete(p);
            } catch (IOException exception) {
                throw new UncheckedIOException("Could not delete file at " + fileOrDirPath, exception);
            }
        });
    }
}

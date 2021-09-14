package jb.engine.core;

import jb.engine.core.data.*;
import jb.engine.exceptions.*;
import jb.engine.reporting.CopyProgress;
import jb.engine.reporting.ProblemReport;
import jb.engine.services.CopyService;
import jb.engine.services.HashService;
import jb.engine.utils.PathUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Central Class for creating copies and snapshots as well as loading and saving information about this context's snapshots.
 * These information include<br>
 * <ul>
 *  <li>important paths
 *  <li>information about already made snapshots (full copy or snapshot, with saved inverse map or without)
 * </ul>
 */
public class Context {

    // TODO: Add logging
    private static final Logger logger = Logger.getLogger(Context.class.getName());

    // Names of files and directories used by CopySnap context
    private static final String DIRECTORY_NAME_COPY_SNAP = "CopySnap";
    private static final String DIRECTORY_NAME_INTERNAL_DATA = ".copysnap";
    private static final String DIRECTORY_NAME_TARGET = "data";
    private static final String FILE_NAME_BACKUP = "backup.txt";
    private static final String BACKUP_FILE_DELIMITER = "=";

    // Keys for saving easily saving and loading a context
    public static final String HOME_KEY = "home";
    public static final String SOURCE_KEY = "source";
    public static final String TARGET_KEY = "target";
    public static final String INTERNAL_KEY = "internal";
    public static final String BACKUP_INFO = "backup";

    private static final String TIME_PATTERN = "yyyy-MM-dd-HH-mm-ss-SSSS";

    // ------------------------- Database Fields

    /**
     * Absolute source path. This path points to the directory to create snapshots from.
     */
    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.NOT_NULL}, constructorArgumentPositionIndex = 0)
    private final Path sourcePath;

    /**
     * Home path (absolute). The root of all internal directories and files.
     */
    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.NOT_NULL, SQLiteConstraint.UNIQUE}, constructorArgumentPositionIndex = 1)
    private final Path homePath;

    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.NOT_NULL}, constructorArgumentPositionIndex = 2)
    private final String name;

    @DataField(sqliteType = SQLiteType.INTEGER, sqliteConstraints = {SQLiteConstraint.NOT_NULL}, constructorArgumentPositionIndex = 3)
    private final Instant createdTime;

    @DataField(sqliteType = SQLiteType.TEXT, sqliteConstraints = {SQLiteConstraint.PRIMARY_KEY}, constructorArgumentPositionIndex = 4)
    private final String id;

    /**
     * Map from keys to paths representing all important places of this context.
     */
    private final Map<String, Path> allPaths; // used for convenient saving and loading

    private final Map<String, String> backupContents;


    private final List<SnapshotInfo> snapshotInfoList = new LinkedList<>();

    // ---------------------------------------------------------------------------

    // TODO: Integrate Settings into job execution of this context.
    private ContextSetting contextSetting;

    // -------------------- Create and load Context

    public static List<ContextInfoContainer> getStoredContextInfo() throws DatabaseCommunicationException {
        return DatabaseManager.getInstance().getInfoOfAllContext();
    }

    public static Context loadContextById(String contextId) throws NotFoundException, DatabaseCommunicationException {
        Context loadedContext = DatabaseManager.getInstance()
                .loadContext(contextId).orElseThrow(() -> new NotFoundException("Could not find any stored Context with id " + contextId));
        loadedContext.checkAndRestoreIntegrityOfLoadedPaths();
        return loadedContext;
    }

    /**
     * Creates and initialises a new Context by creating appropriated directories and files at the given location.
     * The newly created context is saved.
     */
    public static Context createNewContextAndInitialise(Path sourcePath, Path initialPath) {
        Context newContext = ContextFactory.createNewContext(sourcePath, initialPath);
        try {
            // initialize directories
            newContext.generateDirectoriesAndFiles();
            newContext.save();
        } catch (Exception e) {
            try {
                // rollback any written files on disc
                PathUtils.deleteFileOrDirectory(newContext.homePath);
            } catch (IOException e2) {
                logger.warning("Could not delete newly created home directory at: " + newContext.homePath);
            }
            throw new ContextException("Could not create home directory and could not save newly created context: " + e, e);
        }
        return newContext;
    }

    /**
     * Given a path pointing to a directory created by CopySnap, this method tries to recreate a context object from that
     * location by recomputing checksum maps.
     * @param homePath the path pointing to a home directory that was created by CopySnap
     * @param percentageConsumer a consumer that is notified whenever progress to the completion of this job was made,
     *                           transporting the percentage value in [0, 1].
     */
    public static Context reconstructContext(Path homePath, Consumer<BigDecimal> percentageConsumer) {
        // load source bath from backup file
        Path sourcePath = Path.of(Context.readBackupFileContent(homePath).get(SOURCE_KEY));
        logger.info("Source path info from backup file loaded: " + sourcePath);
        // search for source path and home path in database
        Optional<Context> foundContextOpt = restoreContextFromDatabase(homePath);
        if(foundContextOpt.isPresent()) {
            Context foundContext = foundContextOpt.get();
            if(!foundContext.getSourcePath().equals(sourcePath)) {
                throw new IllegalArgumentException("Found context in database with matching home path " + homePath + " but unequal source paths: (given source) " + sourcePath + ", (encountered source) " + foundContext.getSourcePath());
            }
            percentageConsumer.accept(BigDecimal.ONE);
            return foundContext;
        }
        // restore context from data on disk
        logger.info("Trying to restore context from disc");
        Context contextToRestore = new Context(sourcePath, homePath, homePath.getFileName() + "_restored", Instant.now(), DatabaseManager.getNewIdValue());
        contextToRestore.checkAndRestoreIntegrityOfLoadedPaths();
        // restore snapshot info objects
        List<Path> targetPaths;
        try (Stream<Path> dirStream = Files.list(contextToRestore.allPaths.get(TARGET_KEY)).filter(Files::isDirectory)) {
             targetPaths = dirStream.collect(Collectors.toList());
        } catch(IOException e) {
            throw new UncheckedIOException("Could not iterate over file stream to retrieve target directories in " + contextToRestore.allPaths.get(TARGET_KEY) + ": " + e, e);
        }
        targetPaths.sort(Comparator.reverseOrder());  // sort such that newest snapshot items are restored first
        int doneTargetPathCount = 0;
        for(Path targetPath : targetPaths) {
            percentageConsumer.accept(BigDecimal.valueOf((double)doneTargetPathCount/targetPaths.size()));
            Path actualPathForChecksum = targetPath.resolve(sourcePath.getFileName());
            if(!Files.isDirectory(actualPathForChecksum)) {
                logger.warning("Expected directory at " + actualPathForChecksum + ": Skipping reconstruction of snapshot info at path " + targetPath);
            }
            try {
                HashMap<Path, ByteBuffer> targetChecksumMap = HashService.computeChecksumMap(actualPathForChecksum);
                contextToRestore.addSnapshotInfoOfRun(targetPath.getFileName() + "_restored", targetPath, targetChecksumMap, CopyType.RESTORED, false);
            } catch (Exception e) {
                logger.warning("Could not compute checksum map: Skipping reconstruction of snapshot info at path " + targetPath);
            }
            doneTargetPathCount++;
        }
        percentageConsumer.accept(BigDecimal.valueOf((double)doneTargetPathCount/targetPaths.size()));
        contextToRestore.save();
        return contextToRestore;
    }

    /**
     * @return a context from the database matching the given home path. The home path should be unique among the contexts.
     *  The Optional is empty if there was no such context or an exception occurred.
     */
    private static Optional<Context> restoreContextFromDatabase(Path homePath) {
        Optional<Context> foundContextOpt;
        try {
            Optional<ContextInfoContainer> ciOpt = DatabaseManager.getInstance().getInfoOfAllContext()
                    .stream()
                    .filter(ci -> ci.getHomePath().equals(homePath))
                    .findAny();
            if(ciOpt.isPresent()) {
                foundContextOpt = DatabaseManager.getInstance().loadContext(ciOpt.get().getId());
            } else {
                foundContextOpt = Optional.empty();
            }
        } catch (DatabaseCommunicationException e) {
            logger.info("Error while restoring context from database with home path " + homePath +": " + e);
            foundContextOpt = Optional.empty();
        }
        return foundContextOpt;
    }

    /**
     * Creates a new Context instance containing no SnapshotInfo.
     */
    protected Context(Path sourcePath, Path homePath, String contextName, Instant createdTime, String id) {
        this.sourcePath = sourcePath;
        this.homePath = homePath;
        this.name = contextName;
        this.createdTime = createdTime;
        this.id = id;
        this.allPaths = createInternalPathMapOfContext(sourcePath, homePath);
        this.backupContents = generateBackupFileContent();
    }

    /**
     * Creates the path to the directory this context manages. The directory will be a subdirectory of
     * {@code initialPath} and its name will contain the filename of {@code sourcePath}
     */
    protected static Path determineHomeDirectoryPath(Path sourcePath, Path initialPath) {
        String normalizedSourceName = sourcePath.getFileName().toString().replace(" ","");
        String directoryName = String.format("%s-%s",
                DIRECTORY_NAME_COPY_SNAP,
                normalizedSourceName.substring(0, 1).toUpperCase() + normalizedSourceName.substring(1)
        );
        return initialPath.resolve(directoryName);
    }

    /**
     * Checks if the given map behaves as the realm path map that is created upon initialisation of a new context and
     * restores files if needed and possible.<br>
     */
    protected void checkAndRestoreIntegrityOfLoadedPaths() {
        if(!Files.isDirectory(allPaths.get(HOME_KEY))) {
            throw new IntegrityException("Home path of context " + name + " is invalid", new NotADirectoryException(allPaths.get(HOME_KEY)));
        }
        if (!Files.isDirectory(allPaths.get(SOURCE_KEY))) {
            throw new IntegrityException("Source path of context " + name + " is invalid", new NotADirectoryException(allPaths.get(HOME_KEY)));
        }
        if(!Files.isDirectory(allPaths.get(TARGET_KEY)) || !allPaths.get(TARGET_KEY).getFileName().toString().equals(DIRECTORY_NAME_TARGET)) {
            logger.warning("Target path of context " + name + " is invalid: " + allPaths.get(TARGET_KEY) + " is not a directory or is not named " + DIRECTORY_NAME_TARGET);
            try {
                Files.createDirectory(allPaths.get(TARGET_KEY));
            } catch (IOException e) {
                throw new IntegrityException("Could not restore integrity of target location " + allPaths.get(TARGET_KEY) + ": " + e, e);
            }
        }
        if(!Files.isDirectory(allPaths.get(INTERNAL_KEY)) || !allPaths.get(INTERNAL_KEY).getFileName().toString().equals(DIRECTORY_NAME_INTERNAL_DATA)) {
            logger.warning("Internal settings path of context " + name + " is invalid: " + allPaths.get(INTERNAL_KEY) + " is not a directory or is not named " + DIRECTORY_NAME_INTERNAL_DATA);
            try {
                Files.createDirectory(allPaths.get(INTERNAL_KEY));
                // also recreate backup file
                Files.writeString(allPaths.get(BACKUP_INFO), generateBackupFileContentString(), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new IntegrityException("Could not restore integrity of internal settings location " + allPaths.get(INTERNAL_KEY) + ": " + e, e);
            }
        } else if (!Files.isRegularFile(allPaths.get(BACKUP_INFO)) || !allPaths.get(BACKUP_INFO).getFileName().toString().equals(FILE_NAME_BACKUP)) {
            logger.warning("Internal backup file path of context " + name + " is invalid: " + allPaths.get(BACKUP_INFO) + " is not a regular file or is not named " + FILE_NAME_BACKUP);
            try {
                Files.writeString(allPaths.get(BACKUP_INFO), generateBackupFileContentString(), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new IntegrityException("Could not restore backup file of internal settings location " + allPaths.get(BACKUP_INFO) + ": " + e, e);
            }
        }
    }

    // -------------------- Public Core Methods

    public ProblemReport plainCopyAndSave(String runName, Consumer<CopyProgress> copyProgressConsumer) {
        Path runTargetPath = null; // init for potential rollback
        try {
             runTargetPath = getTargetDirectoryPathForRun();
            Files.createDirectory(runTargetPath);
            return createPlainCopy(runName, runTargetPath, copyProgressConsumer);
        } catch(Exception e) {
            performRollback(runTargetPath);
            throw new SnapshotException("Could not perform plain copy job", e);
        }
    }

    public ProblemReport snapshotAndSave(String runName, Consumer<CopyProgress> progressConsumer) {
        if(progressConsumer == null) {
            throw new IllegalArgumentException("ProgressConsumer can not be null");
        }
        Path runTargetPath = null;
        try {
            runTargetPath = getTargetDirectoryPathForRun();
            Files.createDirectory(runTargetPath);
            return createSnapshot(runName, runTargetPath, progressConsumer);
        } catch (Exception e) {
            try {
                performRollback(runTargetPath);
            } catch(RollbackException eRoll) {
                throw new SnapshotException("Could not perform snapshot job and tried to perform rollback jobs but failed: " + eRoll, e);
            }
            throw new SnapshotException("Could not perform snapshot job: " + e, e);
        }
    }

    /**
     * Deletes the Checksum-map file and the target directory of the given Snapshot from disk, removes the snapshot
     * from this context and saves the updated snapshot file to disk.
     */
    public void deleteSnapshotAndSave(SnapshotInfo snapshotInfo) {
        // delete snapshotinfo
        if(!snapshotInfoList.contains(snapshotInfo)) {
            throw new IllegalArgumentException(String.format("The given snapshot %s does not exist within this context (%s)", snapshotInfo.getName(), this.name));
        }
        try {
            // delete resources
            PathUtils.deleteFileOrDirectory(snapshotInfo.getRunTargetDirectory());
            // save updated snapshotinfo list
            snapshotInfoList.remove(snapshotInfo);
            save();
        } catch (Exception e) {
            throw new UnresolvableFileException("Could not delete snapshot " + snapshotInfo.getName() + ": " + e, e);
        }
    }

    // ----- Internal Core Methods

    private ProblemReport createPlainCopy(String runName, Path runTargetDirectory, Consumer<CopyProgress> copyProgressConsumer) {
        CopyProgress copyProgress = CopyProgress.withProgressConsumer(copyProgressConsumer);  // new empty progress
        HashMap<Path, ByteBuffer> currentSourceChecksumMap = computeCurrentSourceChecksumMap(copyProgress);
        ProblemReport problemReport = getCopyServiceForRun(runTargetDirectory).plainCopy();
        addSnapshotInfoOfRun(runName, runTargetDirectory, currentSourceChecksumMap, CopyType.PLAIN_COPY);
        save();
        return problemReport;
    }

    private ProblemReport createSnapshot(String runName, Path runTargetDirectory, Consumer<CopyProgress> progressConsumer) throws NotFoundException {
        CopyProgress copyProgress = CopyProgress.withProgressConsumer(progressConsumer);  // new empty progress
        HashMap<ByteBuffer, Path> comparisonMapInverted = loadLatestChecksumMapInverted();
        HashMap<Path, ByteBuffer> currentSourceChecksumMap = computeCurrentSourceChecksumMap(copyProgress);
        ProblemReport problemReport = getCopyServiceForRun(runTargetDirectory).createSnapshotCopy(currentSourceChecksumMap, comparisonMapInverted, copyProgress);  // TODO: Save problem reports to database
        addSnapshotInfoOfRun(runName, runTargetDirectory, currentSourceChecksumMap, CopyType.SNAPSHOT);
        save();
        return problemReport;
    }

    /**
     * Rolls back all changes on disk that occurred during the latest run:
     * <ul>
     *     <li>Deletes the latest snapshot from this context and saves changes to the database</li>
     *     <li>Deletes the target directory of this run</li>
     * </ul>
     */
    private void performRollback(Path runTargetPath) {
        if(runTargetPath == null) {
            return;
        }
        SnapshotInfo latestInfo = getLatestSnapshotInfo();
        if(latestInfo != null) {
            // delete snapshot info
            snapshotInfoList.remove(latestInfo);
            save();
        }
        // Try to delete target directory
        try {
            PathUtils.deleteFileOrDirectory(runTargetPath);
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            throw new RollbackException("Could not delete target directory", e);
        }
    }

    // -------------------- Internal Methods

    private static Map<String, String> readBackupFileContent(Path homePath) {
        Path expectedBackupFilePath = homePath.resolve(DIRECTORY_NAME_INTERNAL_DATA).resolve(FILE_NAME_BACKUP);
        List<String> lines;
        try {
            lines = Files.readAllLines(expectedBackupFilePath);
        } catch(IOException e) {
            throw new ContextException("Could not read  information from backup file at expected location " + expectedBackupFilePath + ": " + e, e);
        }
        Map<String, String> out = new HashMap<>();
        lines.forEach(line -> {
            String[] keyValue = line.split(BACKUP_FILE_DELIMITER);
            if(keyValue.length != 2) {
                throw new ContextException("Could not extract key and value of backup file line: " + line);
            }
            out.put(keyValue[0], keyValue[1]);
        });
        return out;
    }

    private Map<String, Path> createInternalPathMapOfContext(Path sourcePath, Path homePath) {
        Map<String, Path> allPaths = new HashMap<>();

        allPaths.put(Context.HOME_KEY, homePath);
        allPaths.put(Context.SOURCE_KEY, sourcePath);

        Path internalPath = homePath.resolve(DIRECTORY_NAME_INTERNAL_DATA);
        allPaths.put(Context.TARGET_KEY, homePath.resolve(DIRECTORY_NAME_TARGET));
        allPaths.put(Context.INTERNAL_KEY, internalPath);
        allPaths.put(Context.BACKUP_INFO, internalPath.resolve(FILE_NAME_BACKUP));

        return allPaths;
    }

    /**
     * @return A map of property mapping keys to values belonging to this context.
     */
    private Map<String, String> generateBackupFileContent() {
        Map<String, String> backupContents = new HashMap<>();
        backupContents.put(SOURCE_KEY, sourcePath.toString());
        return backupContents;
    }

    private void  generateDirectoriesAndFiles() throws IOException {
        Files.createDirectories(homePath);
        Files.createDirectories(allPaths.get(TARGET_KEY));
        Files.createDirectories(allPaths.get(INTERNAL_KEY));
        // write backup file
        Files.writeString(allPaths.get(BACKUP_INFO), generateBackupFileContentString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String generateBackupFileContentString() {
        StringBuilder sb = new StringBuilder();
        backupContents.forEach((k, v) -> sb.append(k).append(BACKUP_FILE_DELIMITER).append(v).append(System.lineSeparator()));
        return sb.toString();
    }

    /**
     * Saves this context and all attached SnapshotInfo items to the database.
     */
    public void save() {
        try {
            DatabaseManager.getInstance().safeOrUpdateContext(this);
        } catch (SQLException e) {
            throw new ContextException("Could not save this snapshot to database: " + e, e);
        }
    }

    /**
     * Deletes this context with its attached snapshot info from the database. The java-side object remains.
     */
    public void delete() {
        try {
            DatabaseManager.getInstance().deleteContext(this);
        } catch (DatabaseCommunicationException e) {
            throw new ContextException("Could not delete context " + name + "(id: " + id + ") from the database");
        }
    }

    /**
     * Returns the latest snapshot info. Returns null, if there are no SnapshotInfo objects available.
     */
    private SnapshotInfo getLatestSnapshotInfo() {
        if(snapshotInfoList.size() == 0) {
            return null;
        }
        snapshotInfoList.sort(SnapshotInfo::compareTo);
        return snapshotInfoList.get(snapshotInfoList.size()-1);
    }

    /**
     * Loads the map given in the latest-record-info-file. If there is no latest info, null is returned.
     * Throws SnapshotException if loading is not possible.
     */
    private HashMap<ByteBuffer, Path> loadLatestChecksumMapInverted() throws NotFoundException {
        SnapshotInfo latestInfo = getLatestSnapshotInfo();
        if(latestInfo == null) {
            throw new NotFoundException("Could not find an earlier checksum map since there is no earlier SnapshotInfo registered for this context");
        }
        return latestInfo.getInverseChecksumMap();
    }

    /**
     * Adds a new SnapshotInfo Object to this context's SnapshotInfo list. The given source checksum map wil be redirected.
     */
    private void addSnapshotInfoOfRun(String runName, Path runTargetDirectory, HashMap<Path, ByteBuffer> sourceChecksumMap, CopyType copyType) {
        addSnapshotInfoOfRun(runName, runTargetDirectory, sourceChecksumMap, copyType, true);
    }

    /**
     * Adds a new SnapshotInfo Object to this context's SnapshotInfo list.
     */
    private void addSnapshotInfoOfRun(String runName, Path runTargetDirectory, HashMap<Path, ByteBuffer> checksumMap, CopyType copyType, boolean redirectChecksumMap) {
        addSnapshotInfo(
                SnapshotInfoFactory.createNew(this.id,
                        runName,
                        runTargetDirectory,
                        redirectChecksumMap ? HashService.redirectChecksumMap(checksumMap, runTargetDirectory) : checksumMap,
                        copyType)
        );
    }

    private HashMap<Path, ByteBuffer> computeCurrentSourceChecksumMap(CopyProgress copyProgress) {
        try {
            return HashService.computeChecksumMap(sourcePath, copyProgress);
        } catch (FileNotFoundException e) {
            throw new SnapshotException("Could not compute source checksums", e);
        }
    }

    /**
     * Returns a Path to a File with name returned by {@link #getDateString()} located in this context target directory.
     */
    private Path getTargetDirectoryPathForRun() {
        return allPaths.get(Context.TARGET_KEY).resolve(getDateString());
    }

    /**
     * Returns a new CopyService with this contexts sourcePath and targetPath consisting of this context's general target
     * directory and the given dirName, where the ContextService's resulting copies and snapshots will be located.
     * @see jb.engine.services.CopyService
     */
    private CopyService getCopyServiceForRun(Path runTargetDirectoryPath) {
        return CopyService.createCopyService(runTargetDirectoryPath, sourcePath);
    }

    /**
     * Returns the current Date as a string in the form
     * <p>{@value #TIME_PATTERN}</p>
     */
    private String getDateString() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern(TIME_PATTERN));
    }

    static String getContextName(Path sourcePath) {
        return "Context - " + sourcePath.getFileName().toString();
    }

    // Setter

    private void addSnapshotInfo(SnapshotInfo snapshotInfo) {
        snapshotInfoList.add(snapshotInfo);
    }

    /**
     * replaces this context's snapshot info list with the given items. This method checks, if all given snapshot info
     * items are associated to this context.
     */
    public void setSnapshotInfoList(List<SnapshotInfo> snapshotInfoListToSet) {
        snapshotInfoListToSet.stream()
                .filter(si -> !si.getAssociatedContextId().equals(id))
                .findFirst()
                .ifPresent(si -> {
                    throw new IllegalArgumentException("Can not set snapshot of this context list contains a snapshot info with context id " +
                            si.getAssociatedContextId() + " that does not match this context's id " + id);
                });
        snapshotInfoList.clear();
        snapshotInfoList.addAll(snapshotInfoListToSet);
    }

    // Getter

    public String getId() {
        return id;
    }

    public Path getHomePath() {
        return homePath;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public List<SnapshotInfo> getSnapshotInfoList() {
        return snapshotInfoList;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public Map<String, Path> getAllPaths() {
        return allPaths;
    }
}

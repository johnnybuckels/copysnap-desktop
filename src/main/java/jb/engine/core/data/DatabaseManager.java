package jb.engine.core.data;

import jb.engine.core.*;
import jb.engine.exceptions.DatabaseCommunicationException;
import jb.engine.exceptions.DatabaseInitialisationException;
import jb.engine.exceptions.DatabaseUnexpectedSituationException;
import jb.engine.utils.PathUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Class for managing the connection and execution of queries to the sqlite database.
 */
public class DatabaseManager {

    private static final String DATABASE_DRIVER_NAME = "jdbc:sqlite";
    private static final String DATABASE_DIR_NAME = ".data";
    private static final String DATABASE_NAME = "copysnap.db";
    private static final String DEFAULT_MANAGER_NAME = "DefaultManager";

    // ----------------- Db Tools

    private static final DatabaseToolkit<Context> CONTEXT_DATABASE_TOOLKIT = DatabaseToolkit.forType(Context.class, new ContextFactory());
    private static final DatabaseToolkit<SnapshotInfo> SNAPSHOT_INFO_DATABASE_TOOLKIT = DatabaseToolkit.forType(SnapshotInfo.class, new SnapshotInfoFactory());
    private static final DatabaseToolkit<LatestState> LATEST_STATE_DATABASE_TOOLKIT = DatabaseToolkit.forType(LatestState.class, LatestState::createFromArgs);

    // ----------------- Error message prefixes

    private static final String INITIALISATION_DATABASE_ERROR_PREFIX = "Could not establish initial database connection: ";
    private static final String INITIALISATION_TABLES_ERROR_PREFIX = "Could not initialize tables: ";
    private static final String INSERT_CONTEXT_ERROR_PREFIX = "Could not insert context object: ";

    // ----------------- Initialisation of this class

    private static DatabaseManager DATABASE_MANAGER = null;

    private static boolean allowReinitialization = false;

    /**
     * @return {@code UUID.randomUUID().toString()}
     */
    public static String getNewIdValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * If set to false (the default) initializing a DatabaseManager more than once will result in an Exception.
     */
    public static void allowReinitialization(boolean value) {
        allowReinitialization = value;
    }

    /**
     * Initializes a default DatabaseManager that is accessible via {@link #getInstance()}.</br>
     * There can only be one DatabaseManager at a time.
     */
    public static void initializeDefaultManager() {
        if(allowReinitialization && DATABASE_MANAGER != null) {
            throw new IllegalStateException("There is an already initialized database manger in place and reinitialization is not allowed: " + DATABASE_MANAGER.managerName);
        }
        DATABASE_MANAGER = new DatabaseManager(DATABASE_NAME, DEFAULT_MANAGER_NAME);
    }

    /**
     * Initializes a custom DatabaseManager that is accessible via {@link #getInstance()} that creates or connects to a database
     * with the given name.</br>
     * There can only be one DatabaseManager at a time.
     */
    public static void initializeCustomManager(String databaseName, String managerName) {
        if(allowReinitialization && DATABASE_MANAGER != null) {
            throw new IllegalStateException("There is an already initialized database manger in place and reinitialization is not allowed: " + DATABASE_MANAGER.managerName);
        }
        DATABASE_MANAGER = new DatabaseManager(databaseName, managerName);
    }

    /**
     * Returns the default instance that is created upon application startup.
     */
    public static DatabaseManager getInstance() {
        if(DATABASE_MANAGER == null) {
            throw new IllegalStateException("There was no DatabaseManager initialized");
        }
        return DATABASE_MANAGER;
    }

    /**
     * Creates a new DatabaseManager instance and connects it to the database of the given name.
     */
    public static DatabaseManager getNewCustomInstance(String databaseName, String managerName) {
        return new DatabaseManager(databaseName, managerName);
    }

    /**
     * Creates a singelton-Style instance while trying to establish a connection to an existing database at the expected location.
     * If that databse-file does not exist, it will be created and initialised.
     */
    private DatabaseManager(String databaseName, String managerName) {
        Path basePath = PathUtils.findApplicationBasePath();
        try {
            // create database dir if needed
            Path dirToCreate = basePath.resolve(DATABASE_DIR_NAME);
            if(!Files.exists(dirToCreate)) {
                Files.createDirectory(dirToCreate);
                System.out.println("Created db dir at " + dirToCreate);
            }
        } catch (IOException e) {
            throw new DatabaseUnexpectedSituationException(INITIALISATION_DATABASE_ERROR_PREFIX + "Could not create database directory at " + basePath, e);
        }
        this.databaseLocation = basePath.resolve(DATABASE_DIR_NAME).resolve(databaseName);
        this.databaseConnectionName = DATABASE_DRIVER_NAME + ":" + databaseLocation;
        this.managerName = managerName;
        try {
            renewConnection(databaseConnectionName);
            printCurrentConnectionMetadata();
            initializeMissingTables();
        } catch (SQLException e) {
            throw new DatabaseUnexpectedSituationException(INITIALISATION_DATABASE_ERROR_PREFIX + e, e);
        }
    }


    /**
     * Prints some information about the metadata stored in the given connection.
     */
    private void printCurrentConnectionMetadata() throws SQLException {
        if(c == null) {
            System.out.println("Printing current connection metadata not possible: current connection is null");
        }
        DatabaseMetaData databaseMetaData =  c.getMetaData();
        System.out.println("Database connection of Manager " + managerName + ":");
        System.out.println("\tDriver: " + databaseMetaData.getDriverName());
        System.out.println("\tVersion: " + databaseMetaData.getDatabaseProductVersion());
        System.out.println("\tlocation: " + databaseMetaData.getURL());
    }

    private void initializeMissingTables() throws DatabaseInitialisationException {
        if(c == null) {
            throw new DatabaseInitialisationException(INITIALISATION_TABLES_ERROR_PREFIX + "connection is null");
        }
        CONTEXT_DATABASE_TOOLKIT.createTableIfNotExists(c);
        SNAPSHOT_INFO_DATABASE_TOOLKIT.createTableIfNotExists(c);
        LATEST_STATE_DATABASE_TOOLKIT.createTableIfNotExists(c);
    }
    // ----------------- Object Fields

    private final Path databaseLocation;
    private final String databaseConnectionName;
    private final String managerName;
    private Connection c;

    // ----------------- Usable methods

    public void renewConnection(String databaseConnectionName) throws SQLException {
        Connection connection = DriverManager.getConnection(databaseConnectionName);
        connection.setAutoCommit(false);
        c = connection;
    }

    public Connection getConnection() {
        return c;
    }

    public boolean isConnected() {
        try {
            return c != null && c.isValid(10);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Rolls back the current connection, closes the connection, sets this manager's connection to null.
     */
    public void disconnect() throws DatabaseCommunicationException {
        try {
            c.rollback();
            c.close();
            c = null;
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Could not close connection: " + e, e);
        }
    }

    /**
     * Creates a new database connection and initialises missing tables if needed.
     */
    public void connectToDatabase() throws DatabaseInitialisationException {
        try {
            renewConnection(databaseConnectionName);
            printCurrentConnectionMetadata();
        } catch (SQLException e) {
            throw new DatabaseInitialisationException(INITIALISATION_DATABASE_ERROR_PREFIX + e, e);
        }
        initializeMissingTables();
    }

    /**
     * Disconnects from the database and deletes the database file on disc.
     */
    public void deleteDatabase() throws DatabaseCommunicationException {
        disconnect();
        try {
            Files.delete(databaseLocation);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete database file expected at " + databaseLocation + ": " + e, e);
        }
    }

    /**
     * Loads and returns the latest used context.
     */
    public Optional<Context> loadLastUsedContext() throws DatabaseCommunicationException {
        Optional<LatestState> latestStateOpt = LATEST_STATE_DATABASE_TOOLKIT.findById(c, LatestState.STATE_ID);
        if(latestStateOpt.isPresent()) {
            return loadContext(latestStateOpt.get().getLastUsedContextId());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Saves the given LatestState as the new latest state.
     */
    public void saveOrUpdateLatestState(LatestState latestState) throws DatabaseCommunicationException{
        if(LATEST_STATE_DATABASE_TOOLKIT.exists(c, latestState)) {
            LATEST_STATE_DATABASE_TOOLKIT.update(c, latestState);
        } else {
            LATEST_STATE_DATABASE_TOOLKIT.insert(c, latestState);
        }
    }

    /**
     * Returns a list of information about all stored contexts.
     */
    public List<ContextInfoContainer> getInfoOfAllContext() throws DatabaseCommunicationException {
        List<ContextInfoContainer> out = new LinkedList<>();
        for(Context context : CONTEXT_DATABASE_TOOLKIT.findAll(c)) {
            out.add(new ContextInfoContainer(
                    context.getId(),
                    context.getHomePath(),
                    context.getName(),
                    context.getCreatedTime(),
                    SNAPSHOT_INFO_DATABASE_TOOLKIT.countByColumn(c, SnapshotInfo.CONTEXT_IDENTIFYING_SNAPSHOT_COLUMN_NAME, context.getId())
            ));
        }
        return out;
    }

    /**
     * Loads the context with the given id from the database.
     */
    public Optional<Context> loadContext(String contextId) throws DatabaseCommunicationException {
        if(contextId == null) {
            throw new IllegalArgumentException("Context id can not be null");
        }
        Optional<Context> storedContextOpt = CONTEXT_DATABASE_TOOLKIT.findById(c, contextId);
        // fill with snapshot info if present
        if(storedContextOpt.isPresent()) {
            storedContextOpt.get().setSnapshotInfoList(
                    SNAPSHOT_INFO_DATABASE_TOOLKIT.findByColumn(c, SnapshotInfo.CONTEXT_IDENTIFYING_SNAPSHOT_COLUMN_NAME, contextId)
            );
            saveOrUpdateLatestState(new LatestState(storedContextOpt.get().getId()));
        }
        return storedContextOpt;
    }

    /**
     * Inserts or updates the given context in the database. This will also cause inserts, updates or deletes on all
     * associated snapshot info objects.
     */
    public void safeOrUpdateContext(Context contextToSafe) throws DatabaseCommunicationException {
        if(contextToSafe == null) {
            return;
        } else if (c == null) {
            throw new DatabaseCommunicationException(INSERT_CONTEXT_ERROR_PREFIX + "Connection is null");
        }
        // search for existing context
        if(CONTEXT_DATABASE_TOOLKIT.exists(c, contextToSafe)) {
            // update context
            CONTEXT_DATABASE_TOOLKIT.update(c, contextToSafe);
        } else {
            CONTEXT_DATABASE_TOOLKIT.insert(c, contextToSafe);
        }
        // save, update or delete associated SnapshotInfo objects
        safeUpdateOrDeleteSnapshotInfo(contextToSafe.getId(), contextToSafe.getSnapshotInfoList());
        saveOrUpdateLatestState(new LatestState(contextToSafe.getId()));
    }


    /**
     * Determines objects that need to be updated, inserted or deleted and commits these changes to the database.
     */
    private void safeUpdateOrDeleteSnapshotInfo(String associatedContextId, List<SnapshotInfo> snapshotInfoList) throws DatabaseCommunicationException {
        if(snapshotInfoList == null) {
            return;
        } else if (c == null) {
            throw new DatabaseCommunicationException(INSERT_CONTEXT_ERROR_PREFIX + "Connection is null");
        }
        List<Object> alreadyExistingSnapshots
                = SNAPSHOT_INFO_DATABASE_TOOLKIT.findAllIdsByValue(c, SnapshotInfo.CONTEXT_IDENTIFYING_SNAPSHOT_COLUMN_NAME, associatedContextId);
        // insert or update
        for(SnapshotInfo si : snapshotInfoList) {
            if(SNAPSHOT_INFO_DATABASE_TOOLKIT.exists(c, si)) {
                alreadyExistingSnapshots.remove(si.getId());
                SNAPSHOT_INFO_DATABASE_TOOLKIT.update(c, si);
            } else {
                SNAPSHOT_INFO_DATABASE_TOOLKIT.insert(c, si);
            }
        }
        // delete remaining
        for(Object id : alreadyExistingSnapshots) {
            SNAPSHOT_INFO_DATABASE_TOOLKIT.deleteById(c, id);
        }
    }

    /**
     * Removes the given Context from the database.
     */
    public void deleteContext(Context context) throws DatabaseCommunicationException {
        if(context == null) {
            return;
        }
        // delete snapshot info
        for(SnapshotInfo si : context.getSnapshotInfoList()) {
            SNAPSHOT_INFO_DATABASE_TOOLKIT.delete(c, si);
        }
        // delete context itself
        CONTEXT_DATABASE_TOOLKIT.delete(c, context);
    }

}

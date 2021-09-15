package data;

import jb.engine.core.Context;
import jb.engine.core.data.DatabaseManager;
import jb.engine.exceptions.DatabaseCommunicationException;
import jb.engine.utils.PathUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseTest {

    private static final Path SOURCE_PATH = Path.of("/home/johannes/Dokumente/DokumenteHDD/Java/TestOut/TestSource");

    private static final String TEMP_DIR_STRING = System.getProperty("java.io.tmpdir");

    private static final Path HOME_PATH = Path.of(TEMP_DIR_STRING).resolve("copysnaptest1");
    private static final Path HOME_PATH2 = Path.of(TEMP_DIR_STRING).resolve("copysnaptest2");

    private static final String TEST_DB_NAME = "test.db";
    private static final String TEST_DATABASE_MANAGER_NAME = "TestManager";

    private static Context testContext;
    private static Context testContext2;



    @BeforeAll
    public static void setupDatabaseManager() throws IOException {
        DatabaseManager.initializeCustomManager(TEST_DB_NAME, TEST_DATABASE_MANAGER_NAME);
        Files.createDirectories(HOME_PATH);
        Files.createDirectories(HOME_PATH2);
    }

    @BeforeEach
    public void setup() {
        testContext = Context.createNewContextInitialiseAndSave(SOURCE_PATH, HOME_PATH);
        testContext2 = Context.createNewContextInitialiseAndSave(SOURCE_PATH, HOME_PATH2);
    }

    @AfterEach
    public void reset() {
        // clear database
        testContext.delete();
    }

    @AfterAll
    public static void tearDown() throws DatabaseCommunicationException, IOException {
        // delete home path of test context
        PathUtils.deleteFileOrDirectory(HOME_PATH);
        PathUtils.deleteFileOrDirectory(HOME_PATH2);
        // delete database file
        DatabaseManager.getInstance().deleteDatabase();
    }

    @Test
    public void addSnapshotSaveAndLoad() throws DatabaseCommunicationException {
        // load test context from db
        Context ctx = DatabaseManager.getInstance().loadContext(testContext.getId()).get();
        assertEquals(testContext.getId(), ctx.getId());

        // perform snapshots
        ctx.plainCopyAndSave("TestCopy", x -> {});
        ctx.snapshotAndSave("TestSnapshot", x -> {});

        // reload context and verify number of iterations
        ctx = DatabaseManager.getInstance().loadContext(testContext.getId()).get();
        assertEquals(2, ctx.getSnapshotInfoList().size());
    }

    @Test
    public void testContextRestoration() throws DatabaseCommunicationException {
        // load test context from db
        Context ctx = DatabaseManager.getInstance().loadContext(testContext.getId()).get();
        assertEquals(testContext.getId(), ctx.getId());

        // perform copy task and extract map
        ctx.plainCopyAndSave("TestCopy", x -> {});
        assertEquals(1, ctx.getSnapshotInfoList().size());
        HashMap<String, byte[]> copyMap = ctx.getSnapshotInfoList().get(0).getRedirectedChecksumMap();

        // delete context from database
        ctx.delete();

        // restore context
        Context restoredContext = Context.reconstructContext(ctx.getHomePath(), x -> {});
        assertEquals(1, restoredContext.getSnapshotInfoList().size());
        HashMap<String, byte[]> restoredMap = restoredContext.getSnapshotInfoList().get(0).getRedirectedChecksumMap();

        // assert equality of maps
        System.out.println("-------------------------- Original:");
        System.out.println(mapToPrettyString(copyMap));
        System.out.println("-------------------------- Restored:");
        System.out.println(mapToPrettyString(restoredMap));
        testMapEquality(copyMap, restoredMap);
    }

    @Test
    public void testLoadingLatestContext() throws DatabaseCommunicationException {
        DatabaseManager.getInstance().loadContext(testContext.getId());
        Context ctxLast = DatabaseManager.getInstance().loadLastUsedContext().get();
        assertEquals(testContext.getId(), ctxLast.getId());

        DatabaseManager.getInstance().loadContext(testContext2.getId());
        ctxLast = DatabaseManager.getInstance().loadLastUsedContext().get();
        assertEquals(testContext2.getId(), ctxLast.getId());
    }


    private void testMapEquality(Map<String, byte[]> map1, Map<String, byte[]> map2) {
        for(String key1 : map1.keySet()) {
            byte[] value1 = map1.get(key1);
            if(!map2.containsKey(key1)) {
                throw new AssertionError("The second map does not contain key " + key1);
            }
            byte[] value2 = map2.get(key1);
            if(!Arrays.equals(value1, value2)) {
                throw new AssertionError("Values at key \n" + key1 +"\nare not equal:\n" + Arrays.toString(value1) + "\n" + Arrays.toString(value2));
            }
        }
    }

    private String mapToPrettyString(Map<String, byte[]> map) {
        int maxPathLength = map.keySet().stream().mapToInt(String::length).max().getAsInt();
        StringBuilder sb = new StringBuilder();
        map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String keyString = entry.getKey();
                    sb
                            .append(keyString)
                            .append(" ".repeat(Integer.max(0, maxPathLength - keyString.length())))
                            .append(" -> ").append(Arrays.toString(entry.getValue()))
                            .append("\n");
                }
        );
        return sb.toString();
    }

}

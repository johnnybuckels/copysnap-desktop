package data;

import jb.engine.core.Context;
import jb.engine.core.data.DatabaseManager;
import jb.engine.exceptions.DatabaseCommunicationException;
import jb.engine.utils.PathUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseTest {

    private static Path SOURCE_PATH = Path.of("/home/johannes/Dokumente/DokumenteHDD/Java/TestOut/TestSource");
    private static Path INITIAL_PATH = Path.of("/home/johannes/Dokumente/DokumenteHDD/Java/TestOut/Managed");

    private static final String TEST_DB_NAME = "test.db";
    private static final String TEST_DATABASE_MANAGER_NAME = "TestManager";

    private static Context testContext;



    @BeforeAll
    public static void setupDatabaseManager() {
        DatabaseManager.initializeCustomManager(TEST_DB_NAME, TEST_DATABASE_MANAGER_NAME);
    }

    @BeforeEach
    public void setup() {
        testContext = Context.createNewContextAndInitialise(SOURCE_PATH, INITIAL_PATH);
    }

    @AfterEach
    public void reset() {
        // clear database
        testContext.delete();
    }

    @AfterAll
    public static void tearDown() throws DatabaseCommunicationException, IOException {
        // delete home path of test context
        PathUtils.deleteFileOrDirectory(testContext.getHomePath());
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
        Context restoredContext = Context.reconstructContext(SOURCE_PATH, ctx.getHomePath(), x -> {});
        assertEquals(1, restoredContext.getSnapshotInfoList().size());
        HashMap<String, byte[]> restoredMap = restoredContext.getSnapshotInfoList().get(0).getRedirectedChecksumMap();

        // assert equality of maps
        System.out.println("-------------------------- Original:");
        System.out.println(mapToPrettyString(copyMap));
        System.out.println("-------------------------- Restored:");
        System.out.println(mapToPrettyString(restoredMap));
        testMapEquality(copyMap, restoredMap);
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
                .sorted(Comparator.comparing(Map.Entry::getKey))
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

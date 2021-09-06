import jb.engine.core.data.DatabaseToolkit;
import jb.gui.filetree.FileTreeItem;
import org.junit.jupiter.api.Test;

import javax.swing.tree.TreePath;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Sandbox {
    @Test
    public void mapEquals() {
        Set<ByteBuffer> set = new HashSet<>();
        byte[] byteArray = new byte[]{Byte.parseByte("1"), Byte.parseByte("1")};
        byte[] byteArray2 = new byte[]{Byte.parseByte("1"), Byte.parseByte("1")};

        set.add(ByteBuffer.wrap(byteArray));
        assertTrue(set.contains(ByteBuffer.wrap(byteArray2)));
    }

    @Test
    public void addSameKeys() {
        Map<String, String> map = new HashMap<>();
        map.put("a", null);
        map.put("a", "b");
        System.out.println(map.get("a"));
    }

    @Test
    public void equalsOnTree() {
        Path p = Path.of("/a/b/c/d");
        TreePath tp1 = FileTreeItem.getTreePathOfFileTreeItem(p, 2);
        for(int i = 0; i<tp1.getPathCount(); i++) {
            System.out.println("tp1 " + i +": " + ((FileTreeItem)tp1.getPathComponent(i)).getIOPath().toString());

        }
    }

    @Test
    public void snakecaseMethod() {
        String s = "someCamelCaseString";
        String s2 = "notemptY";
        String s3 = "";
        System.out.println(DatabaseToolkit.toSnakeCase(s));
        System.out.println(DatabaseToolkit.toSnakeCase(s2));
        System.out.println(DatabaseToolkit.toSnakeCase(s3));
    }

    @Test
    public void dateTest() {
        Instant nowInstant = Instant.now();
        long epochSeconds = nowInstant.getEpochSecond();
        Instant nowFromSeconds = Instant.ofEpochMilli(epochSeconds * 1000);
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(nowFromSeconds)));
    }

}

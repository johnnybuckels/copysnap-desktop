package jb.gui.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

/**
 * Class for helping to complete paths by supplying suggestions.
 */
public class PathCompletionHelper {

    public static Iterator<Path> getPathIteratorFilteredByPrefix(Path basePathToSearchIn, String prefix) {
        Path basePathToUse = basePathToSearchIn;
        if(basePathToUse == null) {
            basePathToUse = Path.of("/");
        }
        String prefixToUse = prefix == null ? "" : prefix;
        try {
            return Files.list(basePathToUse)
                    .sorted()
                    .filter(p -> p.getFileName().toString().toLowerCase().startsWith(prefixToUse.toLowerCase()))
                    .iterator();
        } catch (IOException e) {
            return Collections.emptyIterator();
        }
    }
}

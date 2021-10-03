package jb.engine.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Class providing information about this application.
 */
public class CoreInfo {

    private static final String VERSION_KEY = "version";
    private static final String AUTHOR_KEY = "author";
    private static final String PROPERTIES_FILE_NAME = "properties.txt";
    private static final Logger logger = Logger.getLogger(CoreInfo.class.getName());
    private static final int BUFFER_SIZE = 64;
    private static final String LINE_BREAK_STRING = ";";
    private static final String KEY_VALUE_DELIMITER = "=";

    public static final Map<String, String> PROPERTIES = Collections.unmodifiableMap(getPropertiesFromPropertiesFile());
    public static final String VERSION = PROPERTIES.getOrDefault(VERSION_KEY, "");
    public static final String AUTHOR = PROPERTIES.getOrDefault(AUTHOR_KEY, "");

    private static Map<String, String> getPropertiesFromPropertiesFile() {
        Map<String, String> propertiesMap = new HashMap<>();
        String fileContent = loadPropertiesFileContentAsString();
        String[] lines = fileContent.split(LINE_BREAK_STRING);
        for(String line : lines) {
            String[] keyValue = line.split(KEY_VALUE_DELIMITER);
            if(keyValue.length != 2) {
                logger.warning("Could not read property key value pair: " + line);
            } else {
                propertiesMap.put(keyValue[0].toLowerCase().trim(), keyValue[1].trim());
            }
        }
        return propertiesMap;
    }

    private static String loadPropertiesFileContentAsString() {
        StringBuilder fileContent = new StringBuilder();
        try(InputStream is = CoreInfo.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is, "Could not create InputStream on resource " + PROPERTIES_FILE_NAME), StandardCharsets.UTF_8)
        ) {
            char[] charBuffer = new char[BUFFER_SIZE];
            int readChars;
            while((readChars = isr.read(charBuffer)) >= 0) {
                fileContent.append(charBuffer, 0, readChars);
            }
        } catch (IOException e) {
            logger.warning("Could not load properties file: " + e);
        }
        return fileContent.toString();
    }


}

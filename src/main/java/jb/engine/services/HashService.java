package jb.engine.services;


import jb.engine.exceptions.IntegrityException;
import jb.engine.exceptions.NotARegularFileException;
import jb.engine.reporting.CopyProgress;
import jb.engine.utils.PathComparator;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collecting paths of the source directory and sorts them in changed und unchanged files.
 */
public class HashService {

    private static final String HASHING_FUNCTION_NAME = "SHA1";
    private static final int BYTE_BUFFER_SIZE = (int) Math.pow(2,16);

    private final HashMap<Path, ByteBuffer> sourceChecksumMap = new HashMap<>();

    public static HashMap<Path, ByteBuffer> computeChecksumMap(Path path) throws FileNotFoundException {
        return computeChecksumMap(path, CopyProgress.withoutConsumer());
    }

    /**
     * Same as {@link #computeChecksumMap(Path)} but with a {@link CopyProgress} that is updated for each analyzed file.
     */
    public static HashMap<Path, ByteBuffer> computeChecksumMap(Path path, CopyProgress copyProgress) throws FileNotFoundException {
        if(!Files.isReadable(path)) {
            throw new FileNotFoundException(path.toString());
        }
        HashService hc = new HashService();

        hc.updateHashMap(path, copyProgress);
        return hc.sourceChecksumMap;
    }

    public static void saveRedirectedChecksumMap(HashMap<Path, ByteBuffer> map, Path targetFilePath, Path someBasePath) throws IOException {
        try(ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(targetFilePath, StandardOpenOption.CREATE_NEW))){
            oos.writeObject(redirectChecksumMap(map, someBasePath));
        }
    }

    public static HashMap<Path, ByteBuffer> loadChecksumMap(Path targetFileLocation) throws IOException {
        if(!Files.isRegularFile(targetFileLocation)) {
            throw new NotARegularFileException(targetFileLocation);
        }
        HashMap<Path, ByteBuffer> outMap = new HashMap<>();
        Object loadedMap;
        try(ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(targetFileLocation))) {
            loadedMap = ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);

        }
        if(loadedMap instanceof HashMap) {
            try {
                ((HashMap<String, byte[]>) loadedMap).forEach((key, value) -> outMap.put(Path.of(key), ByteBuffer.wrap(value)));
            } catch (ClassCastException e) {
                throw new IntegrityException("Could not cast loaded object to HashMap<String, byte[]>", e);
            }
        }
        return outMap;
    }

    public static HashMap<ByteBuffer, Path> loadAsInverseChecksumMap(Path targetFileLocation) throws IOException {
        if(!Files.isRegularFile(targetFileLocation)) {
            throw new NotARegularFileException(targetFileLocation);
        }
        HashMap<ByteBuffer, Path> outMap = new HashMap<>();
        Object loadedMap;
        try(ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(targetFileLocation))) {
            loadedMap = ois.readObject();
        } catch (ClassNotFoundException e) {
           throw new RuntimeException(e);

        }
        if(loadedMap instanceof HashMap) {
            try {
                ((HashMap<String, byte[]>) loadedMap).forEach((key, value) -> outMap.put(ByteBuffer.wrap(value), Path.of(key)));
            } catch (ClassCastException e) {
                throw new IntegrityException("Could not cast loaded object to HashMap<String, byte[]>", e);
            }
        }
        return outMap;
    }

    /**
     * Every key-path of the map will be redirected according to the provided base path before saving.
     * The redirecting is done by replacing a portion of every key-path with the provided base path.
     * The shortest key path in the key set is taken as the portion to be replaced. All but the farthest element will be replaced.
     * <p>Example:</p>
     * <p>
     *     Keys: a/b/c, a/b/c/d/e, a/b/c/d/f <br>
     *     Replacement part: a/b <br>
     *     some base path: x/y/z <br>
     *     result keys: x/y/z/c, x/y/z/c/d/e, x/y/z/c/d/f
     * </p>
     */
    public static <X> HashMap<Path, X> redirectChecksumMap(HashMap<Path, X> map, Path pathToRedirectTo) {
        int baseOffset = Integer.max(
                0,
                map.keySet().stream().min(Comparator.comparing(Path::toString)).orElseThrow(() -> new IllegalArgumentException("Could not determine minimum path in map keys")).getNameCount() - 1
        );
        // compute redirected Map
        HashMap<Path, X> mapRedirected = new HashMap<>();
        map.forEach((pathToRedirect, value) -> {
            Path redirectedPath = pathToRedirectTo.resolve(pathToRedirect.subpath(baseOffset, pathToRedirect.getNameCount()));
            mapRedirected.put(redirectedPath, value);
        });
        return mapRedirected;
    }

    /**
     * Inverts keys and values of the input map. If there are multiple equal value entries, only one of these values
     * will be stored in the output map.
     */
    public static <X, Y> HashMap<Y, X> invertHashMap(HashMap<X, Y> hashMap) {
        HashMap<Y, X> outMap = new HashMap<>();
        hashMap.forEach((key, value) -> outMap.put(value, key));
        return outMap;
    }

    /**
     * @param byteArrayString String of the form "[1, 2, 3, 4, 5]"
     * @return the equivalent byte array.
     */
    private static byte[] convertStringToByteArray(String byteArrayString) {
        if (byteArrayString == null || byteArrayString.length() < 3) {
            return new byte[0];
        }
        String[] stringArray = byteArrayString.substring(1, byteArrayString.length()-1).split(", ");
        byte[] outArray = new byte[stringArray.length];
        for(int i=0; i<stringArray.length; i++) {
            outArray[i] = Integer.valueOf(stringArray[i].trim()).byteValue();
        }
        return outArray;
    }

    /**
     * Unwraps the {@link ByteBuffer} values to their {@code byte[]} representation and returns a new map consisting of
     * these new mappings.
     */
    public static HashMap<String, byte[]> toSerializableChecksumMap(HashMap<Path, ByteBuffer> map) {
        return new HashMap<>(map.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().array())));
    }

    /**
     * Wraps the {@code byte[]} values to their {@link ByteBuffer} representation and returns a new map consisting of
     * these new mappings.
     */
    public static HashMap<Path, ByteBuffer> toCopySnapInternalChecksumMap(HashMap<String, byte[]> map) {
        return new HashMap<>(map.entrySet().stream().collect(Collectors.toMap(entry -> Path.of(entry.getKey()), entry -> ByteBuffer.wrap(entry.getValue()))));
    }

    /**
     * Runs recursively through the given path structure and computes a hash value for every encountered file or directory.
     * The computed values are stored in this objects field {@code sourceChecksumMap}.
     * <p>Hashing includes the filename and its contents.</p>
     */
    private byte[] updateHashMap(Path currentPath, CopyProgress copyProgress) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(HASHING_FUNCTION_NAME);
        } catch(NoSuchAlgorithmException ignored){
            throw new RuntimeException(HASHING_FUNCTION_NAME + " is not a valid algorithm name");
        }
        md.update(currentPath.getFileName().toString().getBytes(StandardCharsets.UTF_8));  // add file name of current path to hash
        byte[] digestValue;
        if(Files.isDirectory(currentPath)) {
            // be sure the order in which the paths are processed is the same each time.
            try (Stream<Path> dirStream = Files.list(currentPath).sorted(new PathComparator())) {
                dirStream.forEach(path -> md.update(updateHashMap(path, copyProgress)));
                copyProgress.increaseDirectoryCountAndNotify();
            } catch(IOException e) {
                // skip this directory
                md.update(new byte[0]);
            }
        } else {
            try(InputStream reader = Files.newInputStream(currentPath)) {
                for(byte[] readBytes = reader.readNBytes(BYTE_BUFFER_SIZE); readBytes.length > 0; readBytes = reader.readNBytes(BYTE_BUFFER_SIZE)) {
                    md.update(readBytes);
                }
                copyProgress.increaseTrueFileCountAndNotify();
            } catch(IOException e) {
                // skip this file
                md.update(new byte [0]);
            }
        }
        digestValue = md.digest();
        ByteBuffer bufferedDigestValue = ByteBuffer.wrap(digestValue);
        sourceChecksumMap.put(currentPath, bufferedDigestValue);
        return digestValue;
    }

}

package jb.engine.utils;

import jb.engine.exceptions.SerDeException;

import java.io.*;

public class SerDeUtils {

    private static final int BUFFER_SIZE = (int) (Math.pow(2, 10));

    /**
     * Writes the serializable object to a byte array.
     */
    public static byte[] serialize(Serializable s) {
        try(ByteArrayOutputStream boos = new ByteArrayOutputStream(BUFFER_SIZE); ObjectOutputStream oos = new ObjectOutputStream(boos)) {
            oos.writeObject(s);
            oos.flush();
            return boos.toByteArray();
        } catch (IOException e) {
            throw new SerDeException("Could not serialize object " + s.toString() + ": " + e, e);
        }
    }

    /**
     * Deserializes a byte array to a java object and tries to convert it to the given type.
     */
    public static <T> T deserialize(byte[] data, Class<T> targetType) {
        Object o;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bis)) {
             o = ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerDeException("Could not deserialize bytes to an object of type " + targetType + ": " + e, e);
        }
        if(targetType.isInstance(o)) {
            return targetType.cast(o);
        } else {
            throw new SerDeException("Deserialized object of type " + o.getClass() + " does not match the desired output type " + targetType);
        }
    }

}

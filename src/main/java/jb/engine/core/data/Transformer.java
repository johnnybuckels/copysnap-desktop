package jb.engine.core.data;

import jb.engine.services.HashService;
import jb.engine.utils.SerDeUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

enum Transformer {
    /**
     * No transformation is made.
     */
    NONE(Function.identity(), Function.identity()),
    /**
     * Only applicable to {@link Boolean} fields. Maps {@link Boolean#TRUE} to {@code 1} and eevery other possible value to {@code 0}.
     */
    BOOLEAN_TO_INT(object -> applyMethodWithTypeCast(Boolean.class, bool -> Boolean.TRUE.equals(bool) ? 1 : 0, object),
            object -> applyMethodWithTypeCast(Integer.class, integer -> integer == 1 ? Boolean.TRUE : Boolean.FALSE, object)
    ),
    /**
     * Only applicable to {@link Instant} fields. Extracts the epoch seconds as int.
     */
    INSTANT_TO_INT(object -> applyMethodWithTypeCast(Instant.class, Instant::getEpochSecond, object),
            object -> applyMethodWithTypeCast(Integer.class, integer -> Instant.ofEpochMilli(Long.valueOf(integer) * 1000), object)
    ),
    /**
     * Only applicable to {@link Path} fields. Extracts the string representation.
     */
    PATH_TO_STRING(object -> applyMethodWithTypeCast(Path.class, Path::toString, object),
            object -> applyMethodWithTypeCast(String.class, Path::of, object)
    ),
    /**
     * Only applicable to {@link HashMap} fields. Serializes the field value to a byte array.
     */
    HASHMAP_OF_STRING_BYTEBUFFER_TO_BYTE(object -> applyMethodWithTypeCast(HashMap.class, SerDeUtils::serialize, object),
            object -> applyMethodWithTypeCast(byte[].class, blob -> HashService.toCopySnapInternalChecksumMap(SerDeUtils.deserialize(blob, HashMap.class)), object)
    ),
    /**
     * Only applicable to {@link CopyType} fields. Serializes the field value to an integer.
     */
    COPY_TYPE_TO_INT(object -> applyMethodWithTypeCast(CopyType.class, CopyType::ordinal, object),
            object -> applyMethodWithTypeCast(Integer.class, integer -> CopyType.values()[integer], object)
    )
    ;

    Transformer(Function<Object, Object> toDbTransformer, Function<Object, Object> fromDbTransformer) {
        this.toDbTransformer = toDbTransformer;
        this.fromDbTransformer = fromDbTransformer;
    }

    private final Function<Object, Object> toDbTransformer;
    private final Function<Object, Object> fromDbTransformer;

    private static final Map<DatabaseToolkit.JavaClassSqlTypePair, Transformer> JAVA_AND_SQLTYPE_TO_TRANSFORMER_MAP = Map.ofEntries(
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(String.class, SQLiteType.TEXT), NONE),
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(Integer.class, SQLiteType.INTEGER), NONE),
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(byte[].class, SQLiteType.BLOB), NONE),
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(Boolean.class, SQLiteType.INTEGER), BOOLEAN_TO_INT),
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(Instant.class, SQLiteType.INTEGER), INSTANT_TO_INT),
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(Path.class, SQLiteType.TEXT), PATH_TO_STRING),
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(HashMap.class, SQLiteType.BLOB), HASHMAP_OF_STRING_BYTEBUFFER_TO_BYTE),
            Map.entry(DatabaseToolkit.JavaClassSqlTypePair.of(CopyType.class, SQLiteType.INTEGER), COPY_TYPE_TO_INT)
    );

    /**
     * Returns a suiting transformer for teh given type pair. Throws {@link NullPointerException} if no transformer could be found.
     */
    public static Transformer getTransformerFor(DatabaseToolkit.JavaClassSqlTypePair javaClassSqlTypePair) {
        return Objects.requireNonNull(JAVA_AND_SQLTYPE_TO_TRANSFORMER_MAP.getOrDefault(javaClassSqlTypePair, null),
                "Could not find a transformer for pair " + javaClassSqlTypePair);
    }

    private static <T> Object applyMethodWithTypeCast(Class<T> typeToCastTo, Function<T, Object> typedFunction, Object objectToApplyMethodTo) {
        try {
            return typedFunction.apply(typeToCastTo.cast(objectToApplyMethodTo));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Failed to apply this method to object of type " + objectToApplyMethodTo.getClass() + " when expecting type " + typeToCastTo + ": " + e, e);
        }
    }

    /**
     * Transforms the given value to the type used in the database.
     */
    public Object toDb(Object o) {
        return toDbTransformer.apply(o);
    }

    /**
     * Transforms the given object, assuming it comes from the database.
     */
    public Object fromDb(Object o) {
        return fromDbTransformer.apply(o);
    }

}

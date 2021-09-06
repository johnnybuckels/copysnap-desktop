package jb.engine.core.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field corresponds to a column in a database representation of the object containing this field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataField {
    /**
     * Declares the column name corresponding to this field. Defaults to the camel case name of this field.
     */
    String columnName() default "";

    /**
     * Declares teh SQLite type of this column.
     */
    SQLiteType sqliteType();

    /**
     * Declares the constraints for this column.
     */
    SQLiteConstraint[] sqliteConstraints() default {};

    /**
     * Declares the position this field takes in a constructor of the class type this annotated field belongs to. Index count starts at 0. Negative
     * values indicate no constructor relevance.
     */
    int constructorArgumentPositionIndex() default -1;

}

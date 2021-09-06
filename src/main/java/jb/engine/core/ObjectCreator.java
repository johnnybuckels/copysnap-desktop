package jb.engine.core;

import jb.engine.core.data.DataField;
import jb.engine.exceptions.ObjectCreatorException;

/**
 * Implementing Classes are able to create object instances with an array of arguments.
 * @param <T> The class type this ObjectCreator instantiates.
 */
public interface ObjectCreator<T> {

    /**
     * Creates an instance of the type associated with this ObjectCreator. Fields from that type feeding into this constructor-like method
     * should usually be annotated with {@link jb.engine.core.data.DataField} and have an index declared via {@link DataField#constructorArgumentPositionIndex()}.
     */
    T createFromArgs(Object[] args) throws ObjectCreatorException;

}

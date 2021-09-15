package jb.engine.core;

import jb.engine.core.data.DatabaseManager;
import jb.engine.exceptions.NotADirectoryException;
import jb.engine.exceptions.ObjectCreatorException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class ContextFactory implements ObjectCreator<Context> {

    /**
     * Creates a new context instance. This context is not yet initialised on disc and not saved.
     */
    public static Context createNewContext(Path sourcePath, Path newHomePath) {
        if(!Files.isDirectory(sourcePath)) {
            throw new NotADirectoryException(sourcePath);
        } else if(!Files.isDirectory(newHomePath)) {
            throw new NotADirectoryException(newHomePath);
        }
        return new Context(
                sourcePath,
                newHomePath,
                Context.getContextName(sourcePath),
                Instant.now(),
                DatabaseManager.getNewIdValue()
        );
    }

    @Override
    public Context createFromArgs(Object[] args) throws ObjectCreatorException {
        Class<?>[] requiredTypes = {Path.class, Path.class, String.class, Instant.class, String.class};  // 6 args
        if(args.length != requiredTypes.length) {
            throw new ObjectCreatorException("Could not create Context instance: got " + args.length + "arguments but expected " + requiredTypes.length);
        }
        // check types
        for(int i = 0; i < requiredTypes.length; i++) {
            Object arg = args[i];
            if(arg == null) {
                throw new ObjectCreatorException("Could not create Context instance: the given required argument at position " + i + " was null");
            } else if(!requiredTypes[i].isAssignableFrom(args[i].getClass())) {
                throw new ObjectCreatorException("Could not create Context instance: the argument at position " + i + " of type " + arg.getClass() + " can not be assigned to the required type " + requiredTypes[i]);
            }
        }
        Context createdContext;
        try {
            createdContext =  new Context(
                    (Path) args[0],
                    (Path) args[1],
                    (String) args[2],
                    (Instant) args[3],
                    (String) args[4]
            );
        } catch (ClassCastException e) {
            throw new ObjectCreatorException("Could not create Context instance: Error while calling constructor: " + e, e);
        }
        return createdContext;
    }

}

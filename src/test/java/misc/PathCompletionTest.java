package misc;

import jb.engine.utils.PathUtils;
import jb.gui.components.PathSelectionBar;
import jb.gui.components.listeners.TextFieldPathCompletionKeyListener;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class PathCompletionTest {

    private static final Path INITIAL_PATH = PathUtils.findApplicationBasePath();

    private final PathSelectionBar psb = new PathSelectionBar("Blubb");
    private final KeyEvent TAB_EVENT = new KeyEvent(psb, 0, 0, 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED);


    @Test
    public void testPathCompletion() {
        psb.validateAndSetTextFieldValue(INITIAL_PATH);
        TextFieldPathCompletionKeyListener listener = (TextFieldPathCompletionKeyListener) psb.getKeyListeners()[0];
        int maxIterations = 4;
        List<String> resultStrings = new LinkedList<>();
        for(int i = 0; i < maxIterations; i++) {
            listener.keyPressed(TAB_EVENT);
            String currentResult = psb.getPlainPath().toString();
            resultStrings.add(currentResult);
        }
        resultStrings.forEach(s -> System.out.println(s + "\n"));
    }

}

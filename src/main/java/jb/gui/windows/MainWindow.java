package jb.gui.windows;

import jb.engine.core.Context;
import jb.engine.core.SnapshotInfo;
import jb.engine.exceptions.DatabaseCommunicationException;
import jb.gui.actions.ExitAction;
import jb.gui.actions.LoadContextAction;
import jb.gui.actions.NewContextAction;
import jb.gui.actions.PathAction;
import jb.gui.components.CopySnapDisplay;
import jb.gui.components.CopySnapMenuBar;
import jb.gui.components.CopySnapSidebar;
import jb.gui.exceptions.CopySnapException;
import jb.gui.utils.MessageUtils;
import jb.gui.worker.BackgroundWorker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Path;
import java.util.Optional;

import static jb.gui.constants.CopySnapFonts.MENU_FONT;
import static jb.gui.constants.CopySnapFonts.MENU_ITEM_FONT;

public class MainWindow extends JFrame {

    private static final String FRAME_TITLE = "CopySnap";

    private final CopySnapSidebar sidebar;
    private final CopySnapDisplay display;

    public MainWindow() {
        super(FRAME_TITLE);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setExtendedState(MAXIMIZED_BOTH);
        initialiseFonts();
        createAndAddMenuBar();
        // initialise sub-components
        sidebar = new CopySnapSidebar(this::transmitInformationToDisplay);
        display = new CopySnapDisplay();
        arrangeMainComponents();
        loadLatestContext();
    }

    /**
     * Loads the latest loaded or saved context from the database.
     */
    private void loadLatestContext() {
        BackgroundWorker.builderForJob(() -> {
                    Optional<Context> latestContext;
                    try {
                        latestContext = Context.loadLatestUsedContext();
                    } catch (DatabaseCommunicationException e) {
                        throw new CopySnapException("Could not retrieve last used context: " + e, e);
                    }
                    latestContext.ifPresentOrElse(this::receiveContext,
                            () -> MessageUtils.showInfoMessage(this, "Create a new Context!", "No Context found")
                    );
                }
        )
                .withJobName("Loading last used context")
                .build()
                .showAndExecute();
    }

    private void arrangeMainComponents() {
        Container frameContent = getContentPane();
        frameContent.add(sidebar, BorderLayout.WEST);
        frameContent.add(display, BorderLayout.CENTER);
    }

    private void createAndAddMenuBar() {
        CopySnapMenuBar menuBar = new CopySnapMenuBar(this);
        populateMenu(menuBar);
        this.setJMenuBar(menuBar);
    }

    /**
     * Create and add submenus.
     */
    private void populateMenu(JMenuBar menuBar) {
        // File
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem fileItemExit = new JMenuItem("Exit");
        fileItemExit.addActionListener(new ExitAction(this));
        fileMenu.add(fileItemExit);
        fileItemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        // Context
        JMenu contextMenu = new JMenu("Context");
        menuBar.add(contextMenu);
        JMenuItem contextItemNewContext = new JMenuItem("New Context...");
        contextItemNewContext.addActionListener(new NewContextAction(this, this::receiveContext));
        contextItemNewContext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        contextMenu.add(contextItemNewContext);
        JMenuItem contextItemLoadContext = new JMenuItem("Load Context...");
        contextItemLoadContext.addActionListener(new LoadContextAction(this, this::receiveContext));
        contextItemLoadContext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
        contextMenu.add(contextItemLoadContext);
        JMenuItem contextItemRestoreContext = new JMenuItem("Restore from disc...");
        contextItemRestoreContext.addActionListener(new PathAction(this, this::reconstructContext));
        contextItemRestoreContext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        contextMenu.add(contextItemRestoreContext);
        // Action
        JMenu actionMenu = new JMenu("Action");
        menuBar.add(actionMenu);
        JMenuItem doPlainCopy = new JMenuItem("Plain copy...");
        doPlainCopy.addActionListener(action -> sidebar.plainCopy());
        actionMenu.add(doPlainCopy);
        doPlainCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
        JMenuItem doSnapshot = new JMenuItem("Snapshot...");
        doSnapshot.addActionListener(action -> sidebar.snapshot());
        actionMenu.add(doSnapshot);
        doSnapshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
        JMenuItem restore = new JMenuItem("Restore...");
        restore.addActionListener(action -> display.restore());
        actionMenu.add(restore);
        actionMenu.addSeparator();
        JMenuItem deleteSnapshot = new JMenuItem("Delete snapshot...");
        deleteSnapshot.addActionListener(action -> sidebar.deleteSelectedSnapshot());
        actionMenu.add(deleteSnapshot);
        // View
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        JMenuItem toggleTreeLink = new JMenuItem("Toggle tree link");
        toggleTreeLink.addActionListener(action -> display.toggleTreeLink());
        viewMenu.add(toggleTreeLink);
        toggleTreeLink.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK));
    }

    private void initialiseFonts() {
        UIManager.put("Menu.font", MENU_FONT);
        UIManager.put("MenuItem.font", MENU_ITEM_FONT);
    }

    /**
     * Sets the frame title according to the given context name. If context is {@code null}, the window title is reset.
     * @param context the context to extract the name from.
     */
    public void updateFrameTitle(Context context) {
        if(context == null) {
            setTitle(FRAME_TITLE);
            return;
        }
        setTitle(FRAME_TITLE + " " + context.getName());
    }

    private void receiveContext(Context context) {
        if(context == null) {
            return;
        }
        updateFrameTitle(context);
        transmitContextToSidebar(context);
        showSuccessfulContextRetrievalMessage(context);
    }

    private void reconstructContext(Path homePath) {
        if(homePath == null) {
            return;
        }
        BackgroundWorker.builderForJob(bigDecimalConsumer -> Context.reconstructContext(homePath, bigDecimalConsumer), BigDecimal.class)
                .withJobName("Reconstructing context")
                .withProgressFunction(bigDecimal -> bigDecimal.multiply(BigDecimal.valueOf(100)).round(new MathContext(0)).intValue())
                .withResultConsumer(this::receiveContext)
                .showIntermediateResults(true)
                .build()
                .showAndExecute();
    }

    private void showSuccessfulContextRetrievalMessage(Context context) {
        String prefix = "    ";
        String homeRowInit =    "Home directory at      ";
        String sourceRowInit =  "Source directory at    ";
        MessageUtils.showInfoMessage(null,
                "Successfully set up CopySnap Context " + context.getName() + "\n" +
                        prefix + homeRowInit  + context.getHomePath().toString() + "\n" +
                        prefix + sourceRowInit + context.getSourcePath().toString(),
                "Loading completed"
        );
    }

    /**
     * Transmits the given Context to the sidebar container.
     * @param context the context to transmit.
     */
    private void transmitContextToSidebar(Context context) {
        if(context == null) {
            return;
        }
        sidebar.setContext(context);
        this.requestFocus();
    }

    private void transmitInformationToDisplay(Context context, SnapshotInfo snapshotInfo) {
        display.retrieveContextAndSnapshotInfo(context, snapshotInfo);
    }

}

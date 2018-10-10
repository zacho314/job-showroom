package distedit;

import distedit.event.TextInsertEvent;
import distedit.event.dispatch.ClientDispatcher;
import distedit.event.dispatch.LocalDispatcher;
import distedit.event.dispatch.ServerDispatcher;
import distedit.event.dispatch.TextEventDispatcher;
import distedit.event.history.StandardTextEventHistory;
import distedit.event.history.TextEventHistory;
import distedit.event.sequencer.TextEventSequencer;
import distedit.net.*;
import distedit.net.strategies.ClientAcceptSocketStrategy;
import distedit.net.strategies.ClientReceiveEventStrategy;
import distedit.net.strategies.ServerAcceptSocketStrategy;
import distedit.net.strategies.ServerReceiveEventStrategy;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A text editor that is able to run locally or act as a server or a client
 * and establish connections to other text editors.
 */
public class DistributedTextEditor extends JFrame {

    // Text areas and text fields in the GUI
    private JTextArea area1 = new JTextArea(40,120);
    private JTextField ipaddress = new JTextField("localhost");
    private JTextField portNumber = new JTextField("40403");

    // The eventReplayer is responsible for replaying events in area2
    private EventReplayer eventReplayer;
    private Thread eventReplayerThread;

    // A JFileChooser for choosing where to save a document
    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    // Current file name
    private String currentFile = "Untitled";

    // True if the file has been changed, false if not
    private boolean changed = false;

    // A data structure for keeping track of events to be replayed in area2
    private TextEventHistory textEventHistory = new StandardTextEventHistory();

    // The documentEventCapturer captures and dispatches events typed in area1
    private DocumentEventCapturer documentEventCapturer;

    // The connection manager is responsible for establishing and
    // managing internet connections to other text editors
    private ConnectionManager manager = null;

    // Save this instance for access in anonymous classes
    private DistributedTextEditor editor = this;

    /**
     * Instantiate and build the text editor.
     * When instantiated, the editor works as a local editor
     * until the user tries to establish a connection to another
     * editor.
     */
    public DistributedTextEditor() {
        // Build area1
        area1.setFont(new Font("Monospaced",Font.PLAIN,12));

        // Set layout
        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Add vertical and horisontal scroll bars to area1 and area2
        JScrollPane scroll1 =
                new JScrollPane(area1,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1,BorderLayout.CENTER);

        // Add text fields for entering IP address and port number
        content.add(ipaddress,BorderLayout.CENTER);
        content.add(portNumber,BorderLayout.CENTER);

        // Build the menu bar
        JMenuBar JMB = new JMenuBar();
        setJMenuBar(JMB);
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMB.add(file);
        JMB.add(edit);

        file.add(Listen);
        file.add(Connect);
        file.add(Disconnect);
        file.addSeparator();
        file.add(Save);
        file.add(SaveAs);
        file.add(Quit);

        edit.add(Copy);
        edit.add(Paste);
        edit.getItem(0).setText("Copy");
        edit.getItem(1).setText("Paste");

        Save.setEnabled(false);
        SaveAs.setEnabled(false);
        Disconnect.setEnabled(false);

        // Add a window listener so the user is prompted
        // to save the document when closing the window
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveOld();
                System.exit(0);
            }
        });
        pack();

        // Listen for changes in area1
        area1.addKeyListener(k1);

        // Set initial title to "Disconnected" and make the editor visible
        setTitle("Disconnected");
        setVisible(true);

        // Initialize the event replayer with a history and an area where events should be replayed
        eventReplayer = new EventReplayer(textEventHistory, area1);

        documentEventCapturer = new DocumentEventCapturer(new LocalDispatcher(textEventHistory), eventReplayer);
        ((AbstractDocument)area1.getDocument()).setDocumentFilter(documentEventCapturer);

        eventReplayer.setDocumentEventCapturer(documentEventCapturer);
        eventReplayerThread = new Thread(eventReplayer);
        eventReplayerThread.start();
    }

    /**
     * For registering changes to the document
     */
    private KeyListener k1 = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            changed = true;
            Save.setEnabled(true);
            SaveAs.setEnabled(true);
        }
    };

    /**
     * When this action is invoked from the menu bar, the editor
     * becomes a server listening for clients on a given port through
     * a {@link ConnectionManager}.
     *
     * If the port is not specified correctly, a message dialog is shown,
     * informing the user to input a proper port.
     */
    private final Action Listen = new AbstractAction("Listen") {
        // Become a server listening for connections on some port.
        public void actionPerformed(ActionEvent e) {
            try {

                // Ask to save document if changed
                saveOld();

                // Clear both text areas
                clearTextAreas();

                // Enable and disable menu bar buttons
                Save.setEnabled(false);
                SaveAs.setEnabled(false);
                Listen.setEnabled(false);
                Connect.setEnabled(false);
                Disconnect.setEnabled(true);

                // Initialize a new event history
                textEventHistory = new StandardTextEventHistory();

                TextEventSequencer sequencer = new TextEventSequencer(textEventHistory);
                TextEventDispatcher dispatcher = new ServerDispatcher(sequencer);
                // Tell the documentEventCapturer how to dispatch captured events
                documentEventCapturer.setTextEventDispatcher(dispatcher);

                // Instantiate a connection manager that listens for incoming connections
                manager = new ConnectionManager(editor,
                                                new ServerAcceptSocketStrategy(sequencer),
                                                new ServerReceiveEventStrategy(sequencer),
                                                textEventHistory,
                                                true);

                sequencer.setManager(manager);
                manager.start();

                // Tell the user on which IP and port the server is listening by setting the editor's title
                setTitle("I'm listening on " + manager.getMyIP() + ":" + manager.getMyPort());

                // Restart the event replayer with the new history
                restartEventReplayer();

            } catch (NumberFormatException nfe) {
                // NumberFormatException - problems with the port
                System.err.println("User needs to input proper port syntax!");
                JOptionPane.showMessageDialog(editor, "Error. You need to input a proper port");
            }
        }
    };

    /**
     * When this action is invoked from the menu bar, the editor tries
     * to establish a connection through a {@link ConnectionManager} to a given IP and port.
     *
     * If the port is not specified correctly, a message dialog is shown,
     * informing the user to input a proper port.
     */
    private final Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            try {
                // Read host address and port from text fields
                String serverIP = ipaddress.getText();
                int serverPort = Integer.parseInt(portNumber.getText());

                // Ask to save document if changed
                saveOld();

                // Clear both text areas
                clearTextAreas();

                // Tell the user on which IP and port the editor is connecting to by setting the editor's title
                setTitle("Connecting to " + serverIP + ":" + serverPort + "...");

                // Enable and disable menu bar buttons
                Save.setEnabled(false);
                SaveAs.setEnabled(false);
                Disconnect.setEnabled(true);
                Listen.setEnabled(false);
                Connect.setEnabled(false);

                // Initialize a new event history
                textEventHistory = new StandardTextEventHistory();

                // Instantiate a connection manager that listens for incoming connections
                manager = new ConnectionManager(editor,
                        new ClientAcceptSocketStrategy(),
                        new ClientReceiveEventStrategy(textEventHistory),
                        textEventHistory,
                        false);

                manager.connectToServer(serverIP, serverPort);
                manager.start();

                TextEventDispatcher dispatcher = new ClientDispatcher(manager);

                // Tell the documentEventCapturer how to dispatch captured events
                documentEventCapturer.setTextEventDispatcher(dispatcher);

                // Restart the event replayer with the new history
                restartEventReplayer();

            // Handle exceptions:
                // NumberFormatException - problems with the port
            } catch (NumberFormatException nfe) {
                System.err.println("User needs to input proper port syntax!");
                JOptionPane.showMessageDialog(editor, "Error. You need to input a proper port");
            }
        }
    };

    /**
     * When this action is invoked from the menu bar, the editor disconnects
     * from the active connection and goes back to running locally.
     */
    private final Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            disconnect();
        }
    };

    /**
     * Clear both text areas
     */
    private void clearTextAreas() {
        EventQueue.invokeLater(() -> {
            documentEventCapturer.deactivate();
            area1.setText("");
            documentEventCapturer.activate();
        });
        changed = false;
    }

    /**
     * Restart the event replayer
     */
    private void restartEventReplayer() {
        eventReplayerThread.interrupt();
        eventReplayer = new EventReplayer(textEventHistory, area1);
        eventReplayer.setDocumentEventCapturer(documentEventCapturer);
        // Give the DEC the new EventReplayer
        documentEventCapturer.setEventReplayer(eventReplayer);
        eventReplayerThread = new Thread(eventReplayer);
        eventReplayerThread.start();
    }


    public void becomeServer() {
        // Initialize a new event history
        textEventHistory = new StandardTextEventHistory();

        TextEventSequencer sequencer = new TextEventSequencer(textEventHistory);
        TextEventDispatcher dispatcher = new ServerDispatcher(sequencer);
        // Tell the documentEventCapturer how to dispatch captured events
        documentEventCapturer.setTextEventDispatcher(dispatcher);

        manager.setAcceptSocketStrategy(new ServerAcceptSocketStrategy(sequencer));
        manager.setReceiveEventStrategy(new ServerReceiveEventStrategy(sequencer));

        sequencer.setManager(manager);

        String currentText = area1.getText();
        clearTextAreas();
        try {
            sequencer.sequenceEvent(new TextInsertEvent(0,currentText,-1));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * When this action is invoked, the document is either saved
     * as a new document, if it is not already saved, or it is saved
     * to the already existing file
     */
    private Action Save = new AbstractAction("Save") {
        public void actionPerformed(ActionEvent e) {
            if(!currentFile.equals("Untitled"))
                saveFile(currentFile);
            else
                saveFileAs();
        }
    };

    /**
     * Save as a new document
     */
    private Action SaveAs = new AbstractAction("Save as...") {
        public void actionPerformed(ActionEvent e) {
            saveFileAs();
        }
    };

    /**
     * Quit the editor after asking to save
     */
    private Action Quit = new AbstractAction("Quit") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            System.exit(0);
        }
    };

    // Used for getting copy and paste actions
    private ActionMap m = area1.getActionMap();

    /**
     * Action for copying selected text
     */
    private Action Copy = m.get(DefaultEditorKit.copyAction);

    /**
     * Action for pasting selected text
     */
    private Action Paste = m.get(DefaultEditorKit.pasteAction);

    /**
     * Prompt the user to save a new file
     */
    private void saveFileAs() {
        if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    /**
     * If changed, prompt the user to save the file
     */
    public void saveOld() {
        if (changed) {
            if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?","Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION) {
                Save.actionPerformed(null);
            }
            changed = false;
        }
    }

    /**
     * Save the file with a given name
     * @param fileName the file name to save
     */
    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            area1.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        }
        catch(IOException e) {
            System.err.print(e);
            JOptionPane.showMessageDialog(this, "An I/O exception was caught, when trying to save the file.");
        }
    }

    /**
     * Disconnect sets the Editor to its disconnected state and asks if changes should be saved
     * In disconnected state
     *  <ul>
     *      <li>no TCP/IP connection is running</li>
     *      <li>menu items allow for listening and connecting</li>
     *      <li>{@link TextEventHistory} is replaced by a new empty one</li>
     *      <li>text fields are cleared</li>
     *      <li>changes in area1 are reproduced locally in area2</li>
     *  </ul>
     */
    public void disconnect() {
        if (manager != null) {
            manager.disconnect();
            manager = null;
        }

        Disconnect.setEnabled(false);
        Listen.setEnabled(true);
        Connect.setEnabled(true);
        saveOld();

        textEventHistory = new StandardTextEventHistory();
        documentEventCapturer.setTextEventDispatcher(new LocalDispatcher(textEventHistory));
        clearTextAreas();
        setTitle("Disconnected");
        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        restartEventReplayer();
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }
}

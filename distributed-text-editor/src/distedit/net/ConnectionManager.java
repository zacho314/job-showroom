package distedit.net;

import com.sun.corba.se.spi.orbutil.threadpool.ThreadPoolManager;
import distedit.DistributedTextEditor;
import distedit.event.MyTextEvent;
import distedit.event.history.TextEventHistory;
import distedit.net.election.Election;
import distedit.net.election.ElectionResult;
import distedit.net.strategies.AcceptSocketStrategy;
import distedit.net.strategies.ReceiveEventStrategy;
import distedit.threads.ThreadManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionManager {

    private List<Connection> connections;
    private List<Client> clients;

    private boolean isCoordinator;
    private boolean receivedResponse;

    private ServerSocket mySocket;
    private String myIP;
    private int myPort;

    private String serverIP;
    private int serverPort;

    private BlockingQueue<MyTextEvent> incomingEvents;
    private BlockingQueue<MyTextEvent> outgoingEvents;

    private DistributedTextEditor editor;

    private AcceptSocketStrategy acceptSocketStrategy;
    private ReceiveEventStrategy receiveEventStrategy;
    private TextEventHistory localHistory;

    private int myID;

    private ThreadManager threadManager;

    public ConnectionManager(DistributedTextEditor editor,
                             AcceptSocketStrategy acceptSocketStrategy,
                             ReceiveEventStrategy receiveEventStrategy,
                             TextEventHistory localHistory,
                             boolean isCoordinator) {

        try {
            myIP = InetAddress.getLocalHost().getHostAddress();
            myPort = 40403;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.editor = editor;
        connections = new ArrayList<>();
        incomingEvents = new LinkedBlockingQueue<>();
        outgoingEvents = new LinkedBlockingQueue<>();
        clients = new ArrayList<>();
        threadManager = new ThreadManager();

        this.acceptSocketStrategy = acceptSocketStrategy;
        this.receiveEventStrategy = receiveEventStrategy;
        this.localHistory = localHistory;
        this.isCoordinator = isCoordinator;

        openMySocket();
    }

    public void start() {
        startListeningForConnections();
        startBroadcastingEvents();
        startReceivingEvents();
    }

    private void openMySocket() {
        try {
            mySocket = new ServerSocket(myPort);
            mySocket.setReuseAddress(true);
            editor.setTitle("I'm a client. Contact me on: " + mySocket.getInetAddress().getHostAddress() + ":" + myPort);
        } catch (IOException e) {
            System.out.println("There was an error opening a connection on the specified myPort. Trying next port...");
            myPort++;
            openMySocket();
        }
    }

    /**
     * Listen for a connection in a new thread
     */
    private void startListeningForConnections() {

        // Start a new thread that listens for an incoming connection from a client
        Thread connectionListener = new Thread(() -> {
            try {
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    try {
                        String title;
                        if (isCoordinator) {
                            title = "I'm the coordinator. ";
                        } else {
                            title = "I'm a client. ";
                        }
                        title += "Listening on " + myIP + ":" + myPort;
                        editor.setTitle(title);

                        // Block and wait for a client to connect
                        Socket socket = mySocket.accept();
                        socket.setReuseAddress(true);
                        // Delegate the handling of the new connection
                        acceptSocketStrategy.accept(socket, this);

                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("Something when wrong, when trying to accept an incoming connection..");
                        System.err.println(e);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("[ConnectionManager] Connection listener interrupted.");
            }
        });
        threadManager.add(connectionListener);
        connectionListener.start();
    }

    private void startBroadcastingEvents() {
        Thread eventBroadcaster = new Thread(() -> {
            try {
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    MyTextEvent event = outgoingEvents.take();

                    if (!isCoordinator) {
                        // Timeout for sequencer
                        receivedResponse = false;
                        startResponseTimer();
                    }

                    for (Connection c : connections) {
                        c.sendEvent(event);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("[ConnectionManager] Event broadcaster interrupted.");
            }
        });
        threadManager.add(eventBroadcaster);
        eventBroadcaster.start();
    }

    private void startResponseTimer() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if(!receivedResponse) {
                    startElection();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Start an election and wait until a result is ready
     */
    protected void startElection() {
        Election.startAnElection(clients, myID, myIP, myPort);
        ElectionResult result = Election.getResult();

        while(result.getWinnerId() == null) {
            try {
                // Block until a result is ready
                result.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(result.getWinnerId().equals(myID)) {
            // I am the new coordinator!
            editor.becomeServer();

        } else {
            connectToNewCoordinatorFromElectionResult(result);
        }
    }

    /**
     * Connect to the new coordinator.
     * The connections of a ConnectionManager for a client ONLY contains the coordinator connection
     * @param result
     */
    private void connectToNewCoordinatorFromElectionResult(ElectionResult result) {
        try {
            Socket coordinatorSocket = new Socket(result.getWinnerIp(), result.getWinnerPort());
            Connection coordinator = new Connection(coordinatorSocket, this);
            connections.clear();
            connections.add(coordinator);
            // TODO : Maybe handle synchronization problems?
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startReceivingEvents() {
        Thread eventReceiver = new Thread(() -> {
            try {
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    MyTextEvent event = incomingEvents.take();

                    // Do not timeout
                    receivedResponse = true;

                    receiveEventStrategy.process(event);
                }
            } catch (InterruptedException e) {
                System.out.println("[ConnectionManager] Event receiver interrupted.");
            }
        });
        threadManager.add(eventReceiver);
        eventReceiver.start();
    }

    public void queueOutgoingEvent(MyTextEvent event) throws InterruptedException {
        outgoingEvents.put(event);
    }

    public void queueIncomingEvent(MyTextEvent event) throws InterruptedException {
        incomingEvents.put(event);
    }

    public void connectToServer(String IP, int port) {
        Socket socket = null;
        try {
            socket = new Socket(IP, port);
            // Make sure the socket can use the given port even though another socket has
            // recently disconnected from it
            socket.setReuseAddress(true);
            try {
                // Get inputStream from other end
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());


                // Our own little protocol:
                // - If connected to a client, ClientAcceptSocketStrategy will send the boolean false
                // - If connected to the server, ServerAcceptSocketStrategy will send the boolean true
                Boolean connectedToServer = (Boolean) in.readObject();

                if (!connectedToServer) {
                    // Get servers address from the other client
                    String serverIP = (String) in.readObject();
                    int serverPort = in.readInt();

                    // Make a recursive call with the servers IP and port
                    connectToServer(serverIP, serverPort);
                } else {
                    // We have contacted the server
                    // Follow the rest of the protocol defined in Connection.sendServerHello

                    // Send my port for listening
                    out.writeObject(myPort);
                    // Read the log of textevents from the server
                    List<MyTextEvent> log = (List<MyTextEvent>) in.readObject();
                    // Add these to the local history to be replayed
                    localHistory.addAll(log);

                    //We instantiate a connection-object for the connection to the server and add it
                    Connection connToServer = new Connection(socket, this);
                    connToServer.setInputStream(in);
                    connToServer.setOutputStream(out);
                    connToServer.start();
                    addConnection(connToServer);
                    // Remember server's IP and port, to tell other clients trying to connect to the network
                    serverIP = IP;
                    serverPort = port;
                    editor.setTitle("Connected to " + serverIP + ":" + serverPort);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("An error occurred when trying to contact the server on IP: " + IP + ", and port: " + port);
            e.printStackTrace();
        }
    }

    public void addConnection(Connection connection) {
        connections.add(connection);
    }

    public void addClient(Client client) {
        clients.add(client);
    }

    public List<Client> getClients() {
        return clients;
    }

    public String getMyIP() {
        return myIP;
    }
    public int getMyPort() {
        return myPort;
    }
    public String getServerIP() {
        return serverIP;
    }
    public int getServerPort() {
        return serverPort;
    }
    public int getMyID() {
        return myID;
    }

    public void disconnect() {
        try {
            mySocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadManager.stop();
        for (Connection c : connections) {
            c.disconnect();
        }
    }

    public void setAcceptSocketStrategy(AcceptSocketStrategy acceptSocketStrategy) {
        this.acceptSocketStrategy = acceptSocketStrategy;
    }

    public void setReceiveEventStrategy(ReceiveEventStrategy receiveEventStrategy) {
        this.receiveEventStrategy = receiveEventStrategy;
    }

    public void multicastClient(Client client) {
        clients.add(client);

        for(Connection conn : connections) {
            conn.sendClient(client);
        }
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }

    public void removeConnection(Connection connection) {
        connections.remove(connection);
    }
}

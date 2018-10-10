package distedit.net;

import distedit.event.MyTextEvent;
import distedit.net.election.Election;
import distedit.net.election.ElectionMessage;
import distedit.threads.ThreadManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection implements Serializable {

    private final ThreadManager threadManager;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    private Socket socket;

    private final String socketIP;
    private final int socketPort;

    private ConnectionManager manager;
    private BlockingQueue<MyTextEvent> outgoingEventQueue;

    public Connection(Socket socket, ConnectionManager manager) {
        this.socket = socket;
        this.socketIP = socket.getInetAddress().getHostAddress();
        this.socketPort = socket.getPort();
        this.manager = manager;
        threadManager = new ThreadManager();

        outgoingEventQueue = new LinkedBlockingQueue<>();
    }

    public void start() {
        startSendingEvents();
        startReceivingObjects();
    }

    /**
     * Start sending events to the peer from the {@link Connection#outgoingEventQueue} in a new thread.
     */
    private void startSendingEvents() {
        // Start a new thread that sends events to the peer
        Thread eventSender = new Thread(() -> {
            try {
                // Loop, taking events from the event queue and writing them to the output stream
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    MyTextEvent event = outgoingEventQueue.take();
                    writeObjectToStream(event);
                }
            } catch (InterruptedException e) {
                System.out.println("[ConnectionManager] Event sender interrupted.");
            }
        });
        threadManager.add(eventSender);
        eventSender.start();
    }

    /**
     * Start receiving objects from the peer in a new thread.
     * If instance of MyTextEent: Pass on to this connections manager
     * If instance of ElectionMessage: Have Election class process the message
     */
    private void startReceivingObjects() {
        // Start a new thread that receives events from the peer
        Thread eventReceiver = new Thread(() -> {
            // Loop, reading events from the socket and passing them to the manager
            try {
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    Object obj = readObjectFromStream();
                    if (obj instanceof MyTextEvent) {
                        // Text events are put in the managers queue
                        manager.queueIncomingEvent((MyTextEvent) obj);
                    } else if (obj instanceof Client) {
                        // Information on new clients are added to the managers list of clients
                        Client client = (Client) obj;
                        manager.addClient(client);
                    } else if (obj instanceof ElectionMessage) {
                        // Election messages are processed by the Election class
                        Election.processElectionMessage((ElectionMessage) obj,
                                manager.getClients(),
                                manager.getMyID(),
                                manager.getMyIP(),
                                manager.getMyPort());
                    } else {
                        System.out.println("Connection received an unknown message: " + obj);
                    }
                }
            } catch (SocketException e) {
                System.out.println("[Connection] Socket closed.");
            } catch (ClassNotFoundException | IOException e) {
                System.out.print("Woops! Other side closed the connection.. ");

                if (manager.isCoordinator()) {
                    System.out.println("Luckily it was only a client!");
                } else {
                    System.out.println("I better start an election!");
                    manager.startElection();
                }
            } catch (InterruptedException e) {
                System.out.println("Event receiver interrupted.");
            }
        });
        threadManager.add(eventReceiver);
        eventReceiver.start();
    }

    public void sendEvent(MyTextEvent event) throws InterruptedException {
        outgoingEventQueue.put(event);
    }

    public void sendClient(Client client) {
        writeObjectToStream(client);
    }
    /**
     *  Our own little protocol:
     *
     * 1) The server sends a boolean to indicate, that the client has successfully found the server
     * 2) The server reads the port, that the client listens for connections on
     * 4) The server sends the log of current textevents
     * @param log
     */
    public Client sendServerHello(List<MyTextEvent> log) throws IOException, ClassNotFoundException {
        Boolean youHaveConnectedToServer = true;
        // 1)
        writeObjectToStream(youHaveConnectedToServer);
        // 2)
        int clientPortForListening = (Integer) readObjectFromStream();
        // 3)
        writeObjectToStream(log);

        // Use port to return a new client
        return new Client(clientPortForListening,socketIP);
    }

    private void writeObjectToStream(Object obj) {
        try {
            if(outputStream == null) {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
            }
            outputStream.writeObject(obj);
        } catch (IOException e) {
            manager.removeConnection(this);
        }
    }

    private Object readObjectFromStream() throws IOException, ClassNotFoundException {
        if(inputStream == null) {
            inputStream = new ObjectInputStream(socket.getInputStream());
        }
        return inputStream.readObject();
    }

    public String getSocketIP() {
        return socketIP;
    }

    public int getSocketPort() {
        return socketPort;
    }

    public void setInputStream(ObjectInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(ObjectOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void disconnect() {
        try {
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadManager.stop();
    }
}

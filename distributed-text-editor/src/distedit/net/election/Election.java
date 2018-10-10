package distedit.net.election;

import distedit.net.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * A class that implements the bully algorithm for electing a new coordinator
 */
public class Election {

    private static ElectionResult electionResult = new ElectionResult();

    public static void startAnElection(List<Client> clients, int invokersId, String invokersIP, int invokersPort) {

        messageAllClients(clients, invokersId, invokersIP, invokersPort);
    }

    /**
     * Part of the bully algorithm: Sends out messages to all clients with higher id than this
     *
     * @param clients
     * @param fromId
     * @param fromIP
     * @param fromPort
     */
    private static void messageAllClients(List<Client> clients, int fromId, String fromIP, int fromPort) {
        Thread timer = getTimeOutThread();
        timer.start();

        for(Client client : clients) {
            if(client.getId() > fromId) {
                try {
                    sendElectionMessage(client, fromIP, fromPort, timer);
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("An exception occurred during election when trying to contact client on port: " + client.getPort());
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * Send election message to another client
     * @param client
     * @param myIP
     * @param myPort @throws IOException
     * @param timer
     */
    private static void sendElectionMessage(Client client, String myIP, int myPort, Thread timer) throws IOException, ClassNotFoundException {
        // Get socket to client
        Socket clientSocket = new Socket(client.getIP(), client.getPort());
        // And streams for reading/ writing
        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

        // Send the election message
        out.writeObject(new ElectionMessage(myIP, myPort, "ELECTION"));
        waitForResponse(in, timer);
    }

    /**
     * Read the response of an election message
     * @param in
     * @param timer
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void waitForResponse(ObjectInputStream in, Thread timer) throws IOException, ClassNotFoundException {
        ElectionMessage response = (ElectionMessage) in.readObject();

        if(response.getMessage().equals("OK")) {
            timer.interrupt();
        }
    }

    /**
     * Process an election message by sending out messages to everyone else and replying back "OK".
     * If no one responds
     *
     * @param message
     * @param clients
     * @param fromId
     * @param fromIP
     * @param fromPort
     */
    public static void processElectionMessage(ElectionMessage message,
                                              List<Client> clients, int fromId, String fromIP, int fromPort) {

        if(!message.getMessage().equals("ELECTION")) {
            return;
        }
        // Send out messages to everyone else
        messageAllClients(clients, fromId, fromIP, fromPort);

        // Reply back "OK"
        try {
            Socket toOther = new Socket(fromIP, fromPort);
            ObjectOutputStream out = new ObjectOutputStream(toOther.getOutputStream());
            out.writeObject(new ElectionMessage("OK"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * If no one with higher Id has responded, the election is over
     */
    private static Thread getTimeOutThread() {
        return new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // The thread has been interrupted by an "OK" message,
                // therefore we do not end the election
                return;
            }
            // If the thread is not interrupted we end the election
            endElection();
        });
    }

    private static void endElection() {
        // TODO : End election
        electionResult.notifyAll();

        System.out.println("Election has ended!");
    }

    public static ElectionResult getResult() {
        return electionResult;
    }
}

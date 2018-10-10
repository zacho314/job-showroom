package distedit.net.strategies;

import distedit.event.MyTextEvent;
import distedit.event.sequencer.TextEventSequencer;
import distedit.net.Client;
import distedit.net.Connection;
import distedit.net.ConnectionManager;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ServerAcceptSocketStrategy implements AcceptSocketStrategy {

    private TextEventSequencer sequencer;

    public ServerAcceptSocketStrategy(TextEventSequencer sequencer) {
        this.sequencer = sequencer;
    }


    @Override
    public void accept(Socket socket, ConnectionManager manager) throws IOException, ClassNotFoundException {
        // Make a new connection object
        Connection connection = new Connection(socket, manager);
        // Get log of current textevents
        List<MyTextEvent> log = sequencer.getLog();
        // Send a list of messages to the client and get the client object corresponding to the user
        Client client = connection.sendServerHello(log);
        // Start the connection threads for reading/writing textevents
        connection.start();

        manager.addConnection(connection);

        for (Client c : manager.getClients()) {
            connection.sendClient(c);
        }

        // Send out new client to all users of the system
        manager.multicastClient(client);
    }
}

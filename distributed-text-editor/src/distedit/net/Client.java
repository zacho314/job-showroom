package distedit.net;

import distedit.IDs;

import java.io.Serializable;

/**
 * This class represents a user of the DistributedTextEditor. It has the fields:
 *
 * - port: The port that the client uses to listen for new connections
 * - IP:   ip of the client
 * - id:   uniquely identifies the client
 *
 * It used by the server, who multicasts a list of all clients, when a new client connects
 */
public class Client implements Serializable {
    private int port;
    private String IP;
    private int id;


    public Client(int port, String ip) {
        this.port = port;
        this.IP = ip;
        this.id = IDs.getNextId();
    }

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }
}

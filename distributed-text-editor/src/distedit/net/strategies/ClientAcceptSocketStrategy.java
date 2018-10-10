package distedit.net.strategies;

import distedit.net.ConnectionManager;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientAcceptSocketStrategy implements AcceptSocketStrategy {
    @Override
    public void accept(Socket socket, ConnectionManager manager) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            // TODO : Handle incoming election messages

            Boolean youHaveConnectedToServer = false;
            String serverIP = manager.getServerIP();
            int serverPort = manager.getServerPort();

            //Send client to the right server
            out.writeObject(youHaveConnectedToServer);
            out.writeObject(serverIP);
            out.writeInt(serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

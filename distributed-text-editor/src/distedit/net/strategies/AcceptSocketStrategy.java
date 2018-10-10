package distedit.net.strategies;

import distedit.net.ConnectionManager;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by fuve on 29/05/2017.
 */
public interface AcceptSocketStrategy {
    void accept(Socket socket, ConnectionManager manager) throws IOException, ClassNotFoundException;
}

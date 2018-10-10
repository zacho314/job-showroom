package distedit.net.election;

public class ElectionMessage {

    private String sendersIP;
    private int sendersPort;
    private String message;

    private ElectionResult result;

    public ElectionMessage(String myIP, int myPort, String message) {
        this.sendersIP = myIP;
        this.sendersPort = myPort;
        this.message = message;
    }

    public ElectionMessage(String message) {
        this.message = message;
    }


    public String getMessage() {
        return message;
    }

    public ElectionResult getResult() {
        return result;
    }
}

package distedit.net.election;

public class ElectionResult {

    private String winnerId;
    private String winnerIp;
    private int winnerPort;

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinnerIp() {
        return winnerIp;
    }

    public int getWinnerPort() {
        return winnerPort;
    }
}

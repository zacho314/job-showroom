package distedit.net.strategies;

import distedit.event.MyTextEvent;
import distedit.event.history.TextEventHistory;

/**
 * Created by fuve on 29/05/2017.
 */
public class ClientReceiveEventStrategy implements ReceiveEventStrategy {

    private TextEventHistory history;

    public ClientReceiveEventStrategy(TextEventHistory history) {
        this.history = history;
    }

    @Override
    public void process(MyTextEvent event) {
        try {
            history.put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

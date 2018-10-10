package distedit.net.strategies;

import distedit.event.MyTextEvent;

/**
 * Created by fuve on 29/05/2017.
 */
public interface ReceiveEventStrategy {

    void process(MyTextEvent event);
}

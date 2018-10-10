package distedit.event.dispatch;

import distedit.event.MyTextEvent;
import distedit.net.ConnectionManager;

/**
 * Dispatch text events to a peer via a {@link ConnectionManager}
 */
public class ClientDispatcher implements TextEventDispatcher {
    private ConnectionManager manager;

    /**
     * Construct the distributed dispatcher with a {@link ConnectionManager}
     * @param manager the connector used to send text events to a peer
     */
    public ClientDispatcher(ConnectionManager manager) {
        this.manager = manager;
    }

    /**
     * Dispatch a {@link MyTextEvent} by sending it to a peer through the {@link ConnectionManager}
     * @param textEvent the text event to dispatch
     */
    @Override
    public void dispatch(MyTextEvent textEvent) {
        try {
            manager.queueOutgoingEvent(textEvent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

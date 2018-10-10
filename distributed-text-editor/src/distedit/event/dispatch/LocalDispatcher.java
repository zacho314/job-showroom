package distedit.event.dispatch;

import distedit.event.history.TextEventHistory;
import distedit.event.MyTextEvent;

/**
 * Dispatch {@link MyTextEvent} to a local {@link TextEventHistory}
 *
 * @author DA4-03
 * @version 2017-04-10
 */
public class LocalDispatcher implements TextEventDispatcher {
    private TextEventHistory textEventHistory;

    /**
     * Construct the local dispatcher with a {@link TextEventHistory}
     * @param textEventHistory the history to add events to
     */
    public LocalDispatcher(TextEventHistory textEventHistory) {
        this.textEventHistory = textEventHistory;
    }

    /**
     * Dispatch an event by adding it to a local {@link TextEventHistory}
     * @param textEvent the text event to dispatch
     */
    @Override
    public void dispatch(MyTextEvent textEvent) {
        try {
            textEventHistory.put(textEvent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

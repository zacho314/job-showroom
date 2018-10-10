package distedit.event.dispatch;

import distedit.event.MyTextEvent;
import distedit.event.sequencer.TextEventSequencer;


/**
 * Dispatch {@link MyTextEvent} to a local {@link TextEventSequencer}
 *
 * @author DA4-03
 * @version 2017-04-10
 */
public class ServerDispatcher implements TextEventDispatcher {
    TextEventSequencer sequencer;

    /**
     * Construct the local dispatcher with a {@link TextEventSequencer}
     * @param sequencer the sequencer to pass events to
     */
    public ServerDispatcher(TextEventSequencer sequencer) {
        this.sequencer = sequencer;
    }

    /**
     * Dispatch an event by passing it to a local {@link TextEventSequencer}
     * @param textEvent the text event to dispatch
     */
    @Override
    public void dispatch(MyTextEvent textEvent) {
        try {
            sequencer.sequenceEvent(textEvent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

package distedit.event.dispatch;

import distedit.event.MyTextEvent;

/**
 * Dispatches {@link MyTextEvent}s
 *
 * @author DA4-03
 * @version 2017-04-10
 */
public interface TextEventDispatcher {
    /**
     * Dispatch a {@link MyTextEvent}
     * @param textEvent the text event to dispatch
     */
    void dispatch(MyTextEvent textEvent);
}

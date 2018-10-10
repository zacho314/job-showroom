package distedit.event.history;

import distedit.event.MyTextEvent;

import java.util.List;

/**
 * Used for storing {@link MyTextEvent}s in a suitable way and order
 *
 * @author DA4-03
 * @version 2017-04-10
 */
public interface TextEventHistory {
    /**
     * Put a new {@link MyTextEvent} into the history, blocking if necessary
     * @param event the event to put into the history
     * @throws InterruptedException if the thread is interrupted
     */
    void put(MyTextEvent event) throws InterruptedException;

    /**
     * Take the oldest {@link MyTextEvent} from the history, blocking if necessary
     * @return the oldest event from the history
     * @throws InterruptedException if the thread is interrupted
     */
    MyTextEvent take() throws InterruptedException;

    void addAll(List<MyTextEvent> eventList);
}

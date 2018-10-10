package distedit.event.history;

import distedit.event.MyTextEvent;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Used for storing {@link MyTextEvent}s in a linear history queue
 *
 * @author DA4-03
 * @version 2017-04-10
 */
public class StandardTextEventHistory implements TextEventHistory {
    /*
     * We are using a blocking queue for two reasons:
     * 1) They are thread safe, i.e., we can have two threads add and take elements
     *    at the same time without any race conditions, so we do not have to do
     *    explicit synchronization.
     * 2) It gives us a member take() which is blocking, i.e., if the queue is
     *    empty, then take() will wait until new elements arrive, which is what
     *    we want, as we then don't need to keep asking until there are new elements.
     */
    private LinkedBlockingQueue<MyTextEvent> queue = new LinkedBlockingQueue<>();

    @Override
    public void put(MyTextEvent event) throws InterruptedException {
        queue.put(event);
        //TODO: Log events
    }

    @Override
    public MyTextEvent take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public void addAll(List<MyTextEvent> eventList) {
        queue.addAll(eventList);
    }
}



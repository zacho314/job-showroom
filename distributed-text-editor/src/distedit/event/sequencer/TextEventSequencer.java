package distedit.event.sequencer;

import distedit.event.MyTextEvent;
import distedit.event.TextInsertEvent;
import distedit.event.TextRemoveEvent;
import distedit.event.history.TextEventHistory;
import distedit.net.ConnectionManager;

import java.util.LinkedList;
import java.util.List;

/**
 * A standard implementation that sequences {@link MyTextEvent}s
 *
 * @author DA4-03
 * @version 2017-05-08
 */
public class TextEventSequencer {
    private ConnectionManager manager;
    private TextEventHistory history;
    private int nextEventNumber;
    private List<MyTextEvent> log;

    /**
     * Constructs the sequencer with a {@link ConnectionManager} and a {@link TextEventHistory}
     * @param history used for adding sequenced events to the local history
     */
    public TextEventSequencer(TextEventHistory history) {
        this.history = history;
        nextEventNumber = 0;
        this.log = new LinkedList<>();
    }

    /**
     * Controls the ordering of events
     * Keeps a log of all sequenced events
     * If necessary, the events are modified, so that they reflect the changes of the events in the log.
     * @param e
     * @throws InterruptedException
     */
    public synchronized void sequenceEvent(MyTextEvent e) throws InterruptedException {
        // Set the sequence number of the event
        // to be the next event number
        e.setSequenceNumber(nextEventNumber);

        if(e.getSequenceNumber() != (e.getBasedOn() + 1)) {
            // e is not based on the latest event number
            transformEvent(e);
        }
        nextEventNumber++;
        manager.queueOutgoingEvent(e); // Send the event to the client
        history.put(e);             // Put the event in the local history
        log.add(e);                 // Put the event in the log, so that future events, that depends
                                    // this event, can be adjusted to reflect the changes of this event.
    }

    /**
     * Iterate through the log of events, and modify the event,
     * so that it reflects the changes of the events in the log
     * @param event
     */
    private void transformEvent(MyTextEvent event) {
        for (MyTextEvent nextEvent : log) {
            if (nextEvent.getSequenceNumber() > event.getBasedOn() && nextEvent.getSequenceNumber() <= event.getSequenceNumber()) {
                //nextEvent is an event that will be replayed after the event corresponding to event.basedOn and before event.
                if (nextEvent.getOffset() <= event.getOffset()) {
                    //event is dependent on nextEvent.
                    if (nextEvent instanceof TextInsertEvent) {
                        //the offset of event is adjusted by the length of the string inserted by nextEvent.
                        event.setOffset(event.getOffset()+((TextInsertEvent) nextEvent).getText().length());
                    } else if (nextEvent instanceof TextRemoveEvent){
                        //the offset of nextEvent is adjusted by the length being removed by nextEvent.
                        event.setOffset(event.getOffset()-((TextRemoveEvent) nextEvent).getLength());

                        // Avoid IllegalRemove-exceptions
                        if(event.getOffset() < 0) {
                            event.setOffset(0);
                        }
                    }
                }
            }
        }
    }

    public void setManager(ConnectionManager manager) {
        this.manager = manager;
    }

    public List<MyTextEvent> getLog() {
        return log;
    }
}

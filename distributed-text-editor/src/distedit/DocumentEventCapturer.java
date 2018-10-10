package distedit;

import distedit.event.dispatch.TextEventDispatcher;
import distedit.event.TextInsertEvent;
import distedit.event.TextRemoveEvent;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * This class captures and dispatches the text events of the given document on
 * which it is put as a filter. Normally a filter is used to put restrictions
 * on what can be written in a buffer. In our case we just use it to see all
 * the events and make a copy.
 *
 * @author Jesper Buus Nielsen (modified DA4-03 2017)
 */
public class DocumentEventCapturer extends DocumentFilter {

    private TextEventDispatcher textEventDispatcher;
    private boolean isActive = true;
    private EventReplayer eventReplayer;

    /**
     * Constructor for the DocumentEventCapturer. The capturer is constructed
     * with a {@link TextEventDispatcher}.
     * @param textEventDispatcher Dispatches events after they are captured
     * @param eventReplayer       Attaches the number of events, that the captured event depends on
     */
    public DocumentEventCapturer(TextEventDispatcher textEventDispatcher, EventReplayer eventReplayer) {
        super();
        this.textEventDispatcher = textEventDispatcher;
        this.eventReplayer = eventReplayer;
    }

    @Override
    public void insertString(FilterBypass fb, int offset,
                             String str, AttributeSet a)
            throws BadLocationException {

        if(isActive) {
            // If the capturer is active, the event has been produced
            // by the local user, and the event is dispatched.
	        textEventDispatcher.dispatch(new TextInsertEvent(offset, str, eventReplayer.getLatestReplayedEventNumber()));

        } else {

            // The capturer is not active, so the event has been replayed from the eventReplayer
            super.insertString(fb,offset,str,a);
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {

        if(isActive) {
            // Ditto
            textEventDispatcher.dispatch(new TextRemoveEvent(offset, length, eventReplayer.getLatestReplayedEventNumber()));

        } else {

            // Ditto
            super.remove(fb,offset,length);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset,
                        int length,
                        String str, AttributeSet a)
            throws BadLocationException {

        if(isActive){
	        // Ditto
            if (length > 0) {
                textEventDispatcher.dispatch(new TextRemoveEvent(offset, length, eventReplayer.getLatestReplayedEventNumber()));
            }
            textEventDispatcher.dispatch(new TextInsertEvent(offset, str, eventReplayer.getLatestReplayedEventNumber()));

        } else {

            //Ditto
            super.replace(fb,offset,length,str,a);
        }
    }

    /**
     * Set the dispatcher field. The dispatcher handles dispatching events to the right location
     * @param textEventDispatcher The dispatcher
     */
    public void setTextEventDispatcher(TextEventDispatcher textEventDispatcher) {
        this.textEventDispatcher = textEventDispatcher;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;
    }

    public void setEventReplayer(EventReplayer eventReplayer) {
        this.eventReplayer = eventReplayer;
    }

    public TextEventDispatcher getDispatcher() {
        return textEventDispatcher;
    }
}

package distedit;

import distedit.event.MyTextEvent;
import distedit.event.TextInsertEvent;
import distedit.event.TextRemoveEvent;
import distedit.event.history.TextEventHistory;

import javax.swing.JTextArea;
import java.awt.EventQueue;

/**
 * Takes the events from a TextEventHistory and replays
 * them in a JTextArea.
 *
 * @author Jesper Buus Nielsen (modified DA4-03 2017)
 */
public class EventReplayer implements Runnable {

    private TextEventHistory textEventHistory;
    private JTextArea area;
    private DocumentEventCapturer documentEventCapturer;
    private int latestReplayedEventNumber;

    /**
     * Constructor for the EventReplayer.
     * @param textEventHistory the event history where events are taken from
     * @param area the area where the events should be replayed
     */
    public EventReplayer(TextEventHistory textEventHistory, JTextArea area) {
        this.textEventHistory = textEventHistory;
        this.area = area;

        latestReplayedEventNumber = -1;
    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = textEventHistory.take();
                latestReplayedEventNumber = mte.getSequenceNumber();
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent)mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            // Set the latest replayed event number to the value,
                            // of the sequence number of the event, that is replayed
                            latestReplayedEventNumber = mte.getSequenceNumber();

                            // Deactivate the capturer, to avoid infinite playbacks
                            documentEventCapturer.deactivate();
                            area.insert(tie.getText(), tie.getOffset());
                            documentEventCapturer.activate();
                        } catch (Exception e) {
                            System.err.println(e);
			                e.printStackTrace();

                /* We catch all exceptions, as an uncaught exception would make the
                 * EDT unwind, which is not healthy.
                 */
                        }
                    });
                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent)mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            // Set the latest replayed event number to the value,
                            // of the sequence number of the event, that is replayed
                            latestReplayedEventNumber = mte.getSequenceNumber();

                            // Deactivate the capturer, to avoid infinite playbacks
                            documentEventCapturer.deactivate();
                            area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
                            documentEventCapturer.activate();
                        } catch (Exception e) {
                            System.err.println(e);
			                e.printStackTrace();

                /* We catch all exceptions, as an uncaught exception would make the
                 * EDT unwind, which is not healthy.
                 */
                        }
                    });
                }
            } catch (Exception e) {
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }

    public int getLatestReplayedEventNumber() {
        return latestReplayedEventNumber;
    }

    public void setDocumentEventCapturer(DocumentEventCapturer documentEventCapturer) {
        this.documentEventCapturer = documentEventCapturer;
    }
}

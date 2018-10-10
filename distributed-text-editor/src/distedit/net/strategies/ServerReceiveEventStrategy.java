package distedit.net.strategies;

import distedit.event.MyTextEvent;
import distedit.event.sequencer.TextEventSequencer;

public class ServerReceiveEventStrategy implements ReceiveEventStrategy {
    private TextEventSequencer sequencer;

    public ServerReceiveEventStrategy(TextEventSequencer sequencer) {
        this.sequencer = sequencer;
    }

    @Override
    public void process(MyTextEvent event) {
        try {
            sequencer.sequenceEvent(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

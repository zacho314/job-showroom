package distedit.event;

import java.io.Serializable;

/**
 * @author Jesper Buus Nielsen
 */
public class MyTextEvent implements Serializable {

    private int sequenceNumber;

    private int basedOn;

    private int offset;

    public MyTextEvent(int offset, int latestReplayedEventNumber) {
        this.offset = offset;
        this.basedOn = latestReplayedEventNumber;
    }

    public int getOffset() {
        return offset;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getBasedOn() {
        return basedOn;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}

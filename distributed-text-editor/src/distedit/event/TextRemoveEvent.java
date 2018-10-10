package distedit.event;

public class TextRemoveEvent extends MyTextEvent {

	private int length;
	
	public TextRemoveEvent(int offset, int length, int latestReplayedEventNumber) {
		super(offset, latestReplayedEventNumber);
		this.length = length;
	}
	
	public int getLength() { return length; }
}

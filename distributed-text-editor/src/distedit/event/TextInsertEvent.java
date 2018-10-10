package distedit.event;

/**
 * @author Jesper Buus Nielsen
 */
public class TextInsertEvent extends MyTextEvent {

	private String text;
	
	public TextInsertEvent(int offset, String text, int latestReplayedEventNumber) {
		super(offset, latestReplayedEventNumber);
		this.text = text;
	}
	public String getText() { return text; }
}


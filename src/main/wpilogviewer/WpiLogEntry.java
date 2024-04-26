package wpilogviewer;

public class WpiLogEntry {
	// TODO Is it fine to lock ourselves into this representation? (Instead of
	// using methods)
	public final long id;
	public final String name;
	public final String type;
	public String metadata;

	public WpiLogEntry(long id, String name, String type, String metadata) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.metadata = metadata;
	}
}

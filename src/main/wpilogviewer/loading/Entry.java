package wpilogviewer.loading;

import java.util.NavigableMap;
import java.util.TreeMap;

public class Entry {
	private final long startTimestamp;
	private long endTimestamp = -1;
	private final long id;
	private final String name;
	private final String type;
	private NavigableMap<Long, String> timestampToMetadata = new TreeMap<>();
	private NavigableMap<Long, Record> timestampToRecord = new TreeMap<>();

	public Entry(long startTimestamp, long id, String name, String type, String metadata) {
		this.startTimestamp = startTimestamp;
		this.id = id;
		this.name = name;
		this.type = type;
		timestampToMetadata.put(startTimestamp, metadata);
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public long getEndTimestamp() {
		return endTimestamp;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getMetadata(long timestamp) {
		Long floorTimestamp = timestampToMetadata.floorKey(timestamp);
		if (floorTimestamp == null) {
			return null;
		}
		return timestampToMetadata.get(floorTimestamp);
	}

	public Record getRecord(long timestamp) {
		Long floorTimestamp = timestampToRecord.floorKey(timestamp);
		if (floorTimestamp == null) {
			return null;
		}
		return timestampToRecord.get(floorTimestamp);
	}

	public void finish(long timestamp) {
		endTimestamp = timestamp;
	}

	public void setMetadata(long timestamp, String metadata) {
		if (endTimestamp >= 0 && timestamp < endTimestamp) {
			System.err.println("WARNING: Entry.setMetadata: Timestamp (" + timestamp + ") was before end timestamp (" + endTimestamp + ")");
		}
		timestampToMetadata.put(timestamp, metadata);
	}

	public void addRecord(long timestamp, Record record) {
		if (record.getTimestamp() != timestamp) {
			System.err.println("WARNING: Entry.addRecord: Record timestamp (" + record.getTimestamp() + ") didn't match timestamp (" + timestamp + ")");
		}
		if (endTimestamp >= 0 && timestamp < endTimestamp) {
			System.err.println("WARNING: Entry.addRecord: Timestamp (" + timestamp + ") was before end timestamp (" + endTimestamp + ")");
		}
		timestampToRecord.put(timestamp, record);
	}
}
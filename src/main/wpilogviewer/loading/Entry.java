package wpilogviewer.loading;

import java.util.NavigableMap;
import java.util.TreeMap;

public class Entry {
	private final long startTimestamp;
	private long endTimestamp = -1;
	private boolean hasEnded = false;
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

	public boolean isExpiredAt(long timestamp) {
		return hasEnded() && Long.compareUnsigned(endTimestamp, timestamp) < 0;
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public long getEndTimestamp() {
		return endTimestamp;
	}

	public boolean hasEnded() {
		return hasEnded;
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
		if (isExpiredAt(timestamp)) {
			System.err.println("WARNING: Entry.setMetadata: Timestamp (" + Long.toUnsignedString(timestamp) + ") was after end timestamp (" + Long.toUnsignedString(endTimestamp) + ")");
		}
		timestampToMetadata.put(timestamp, metadata);
	}

	public void addRecord(long timestamp, Record record) {
		if (record.getTimestamp() != timestamp) {
			System.err.println("WARNING: Entry.addRecord: Record timestamp (" + Long.toUnsignedString(record.getTimestamp()) + ") didn't match timestamp (" + Long.toUnsignedString(timestamp) + ")");
		}
		if (isExpiredAt(timestamp)) {
			System.err.println("WARNING: Entry.addRecord: Timestamp (" + Long.toUnsignedString(timestamp) + ") was after end timestamp (" + Long.toUnsignedString(endTimestamp) + ")");
		}
		timestampToRecord.put(timestamp, record);
	}
}
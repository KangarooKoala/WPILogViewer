package wpilogviewer.loading;

import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import wpilogviewer.WpiLogProcessor;

public class Loader {
	public static enum Verbosity {
		SILENT,
		QUIET,
		NORMAL,
		VERBOSE;
	}

	private final InputStream inputStream;
	private final LoaderLogger logger;
	private final Map<Long, NavigableMap<Long, Entry>> idToStartToEntry;

	public Loader(InputStream inputStream, Verbosity verbosity) {
		this.inputStream = inputStream;
		this.logger = new LoaderLogger(verbosity);
		// This is a live reference to the logger's map, so it'll be updated for us
		this.idToStartToEntry = logger.getIdToStartToEntry();
	}

	public Loader(InputStream inputStream) {
		this(inputStream, Verbosity.NORMAL);
	}

	public void load() throws IOException {
		WpiLogProcessor.process(inputStream, logger);
	}

	public Set<Long> getIds() {
		return Collections.unmodifiableSet(idToStartToEntry.keySet());
	}

	public Set<Long> getEntryStartTimestamps(long id) {
		if (!idToStartToEntry.containsKey(id)) {
			return null;
		}
		var startToEntry = idToStartToEntry.get(id);
		return Collections.unmodifiableSet(startToEntry.keySet());
	}

	public Entry getEntry(long id, long timestamp) {
		if (!idToStartToEntry.containsKey(id)) {
			return null;
		}
		var startToEntry = idToStartToEntry.get(id);
		Long floorTimestamp = startToEntry.floorKey(timestamp);
		if (floorTimestamp == null) {
			return null;
		}
		var floorEntry = startToEntry.get(floorTimestamp);
		if (floorEntry.getEndTimestamp() > 0 && floorEntry.getEndTimestamp() < timestamp) {
			// Entry got closed
			return null;
		}
		return floorEntry;
	}
}

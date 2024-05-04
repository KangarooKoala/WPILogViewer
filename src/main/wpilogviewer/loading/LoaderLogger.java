package wpilogviewer.loading;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import wpilogviewer.Logger;
import wpilogviewer.Util;
import wpilogviewer.loading.Loader.Verbosity;

class LoaderLogger implements Logger {
	private static final long VALUE_COUNT_PERIOD = 10_000;

	private Map<Long, NavigableMap<Long, Entry>> idToStartToEntry = new HashMap<>();
	private final Verbosity verbosity;
	private long valueCount = 0;

	public LoaderLogger(Verbosity verbosity) {
		this.verbosity = verbosity;
	}

	private void logErr(String message) {
		System.err.println(message);
	}

	private void logWarning(String message) {
		if (verbosity.ordinal() >= Verbosity.QUIET.ordinal()) {
			System.out.println(message);
		}
	}

	private void logInfo(String message) {
		if (verbosity.ordinal() >= Verbosity.NORMAL.ordinal()) {
			System.out.println(message);
		}
	}

	private void logDebug(String message) {
		if (verbosity.ordinal() >= Verbosity.VERBOSE.ordinal()) {
			System.out.println(message);
		}
	}

	Map<Long, NavigableMap<Long, Entry>> getIdToStartToEntry() {
		return idToStartToEntry;
	}

	private boolean hasEntry(long id, long timestamp) {
		if (!idToStartToEntry.containsKey(id)) {
			return false;
		}
		var startToEntry = idToStartToEntry.get(id);
		Long floorTimestamp = startToEntry.floorKey(timestamp);
		if (floorTimestamp == null) {
			return false;
		}
		var floorEntry = startToEntry.get(floorTimestamp);
		if (floorEntry.getEndTimestamp() > 0 && floorEntry.getEndTimestamp() < timestamp) {
			// Entry got closed
			return false;
		}
		return true;
	}

	private void addEntry(long id, long timestamp, Entry entry) {
		var startToEntry = idToStartToEntry.computeIfAbsent(id, key -> new TreeMap<>());
		Long floorTimestamp = startToEntry.floorKey(timestamp);
		if (floorTimestamp == null) {
			startToEntry.put(timestamp, entry);
			return;
		}
		var floorEntry = startToEntry.get(floorTimestamp);
		if (floorEntry.getEndTimestamp() < 0) {
			floorEntry.finish(timestamp);
		}
		startToEntry.put(timestamp, entry);
	}

	private Entry finishEntry(long id, long timestamp) {
		var entry = getEntry(id, timestamp);
		if (entry != null) {
			entry.finish(timestamp);
		}
		return entry;
	}

	private Entry getEntry(long id, long timestamp) {
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

	@Override
	public void logStart(long entryId, String entryName, String entryType, String entryMetadata,  long timestamp) {
		logDebug("Log start: entryId=" + entryId);
		if (hasEntry(entryId, timestamp)) {
			var oldEntry = getEntry(entryId, timestamp);
			logWarning("Note: Overriding existing entry with id " + oldEntry.getId() + " and name " + oldEntry.getName() + "!");
		}
		var entry = new Entry(timestamp, entryId, entryName, entryType, entryMetadata);
		addEntry(entryId, timestamp, entry);
		logDebug("Done with log start");
	}

	@Override
	public void logFinish(long entryId, long timestamp) {
		logDebug("Log finish: entryId=" + entryId);
		if (!hasEntry(entryId, timestamp)) {
			logErr("Could not end non-existent ID " + entryId + " at timestamp " + timestamp + "!");
			return;
		}
		finishEntry(entryId, timestamp);
		logDebug("Done with log finish");
	}

	@Override
	public void logSetMetadata(long entryId, long timestamp, String newMetadata) {
		logDebug("Log set metadata: entryId=" + entryId);
		if (!hasEntry(entryId, timestamp)) {
			logErr("Could not set metadata of entry with non-existent ID " + entryId + " at timestamp " + timestamp + "!");
			return;
		}
		var entry = getEntry(entryId, timestamp);
		entry.setMetadata(timestamp, newMetadata);
		logDebug("Done with log set metadata");
	}

	@Override
	public void logValue(long entryId, long timestamp, Supplier<byte[]> payloadSupplier) {
		logDebug("Log value: entryId=" + entryId);
		if (valueCount % VALUE_COUNT_PERIOD == 0) {
			logInfo("Processing value #" + valueCount);
		}
		++valueCount;
		if (!hasEntry(entryId, timestamp)) {
			logErr("Cannot log to entry with non-existent ID " + entryId + " at timestamp " + timestamp + "!");
			return;
		}
		var entry = getEntry(entryId, timestamp);
		byte[] payload = payloadSupplier.get();
		final Record record;
		switch (entry.getType()) {
			case "raw", "rawBytes" -> {
				record = Record.rawRecord(timestamp, payload);
			}
			case "boolean" -> {
				if (payload.length != 1) {
					logWarning("Got invalid payload for boolean entry " + entry.getId() + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				if (payload[0] < 0 || payload[0] > 1) {
					logWarning("Got invalid payload for boolean entry " + entry.getId() + ": " + payload[0] + "!");
					return;
				}
				boolean value = payload[0] == 1;
				record = Record.booleanRecord(timestamp, value);
			}
			case "int64" -> {
				if (payload.length != 8) {
					logWarning("Got invalid payload for int64 entry " + entry.getId() + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				long value = Util.longFromBytes(payload);
				record = Record.int64Record(timestamp, value);
			}
			case "float" -> {
				if (payload.length != 4) {
					logWarning("Got invalid payload for float entry " + entry.getId() + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				float value = Float.intBitsToFloat(Util.intFromBytes(payload));
				record = Record.floatRecord(timestamp, value);
			}
			case "double" -> {
				if (payload.length != 8) {
					logWarning("Got invalid payload for double entry " + entry.getId() + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				double value = Double.longBitsToDouble(Util.longFromBytes(payload));
				record = Record.doubleRecord(timestamp, value);
			}
			case "json", "string" -> {
				String value = Util.utf8StringFromBytes(payload);
				record = Record.stringRecord(timestamp, value);
			}
			case "boolean[]" -> {
				boolean[] value = new boolean[payload.length];
				for (int i = 0; i < payload.length; ++i) {
					if (payload[i] < 0 || payload[i] > 1) {
						System.out.println("Got invalid payload for boolean array entry " + entry.getId() + ": " + Arrays.toString(payload));
						return;
					}
					value[i] = payload[i] == 1;
				}
				record = Record.booleanArrayRecord(timestamp, value);
			}
			case "int64[]" -> {
				if (payload.length % 8 != 0) {
					logWarning("Got invalid payload for int64[] entry " + entry.getId() + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				long[] value = new long[payload.length / 8];
				for (int i = 0; i < value.length; ++i) {
					value[i] = Util.longFromBytes(payload, 8 * i, 8);
				}
				record = Record.int64ArrayRecord(timestamp, value);
			}
			case "float[]" -> {
				if (payload.length % 4 != 0) {
					logWarning("Got invalid payload for float[] entry " + entry.getId() + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				float[] value = new float[payload.length / 4];
				for (int i = 0; i < value.length; ++i) {
					value[i] = Float.intBitsToFloat(Util.intFromBytes(payload, 4 * i, 4));
				}
				record = Record.floatArrayRecord(timestamp, value);
			}
			case "double[]" -> {
				if (payload.length % 8 != 0) {
					logWarning("Got invalid payload for double[] entry " + entry.getId() + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				double[] value = new double[payload.length / 8];
				for (int i = 0; i < value.length; ++i) {
					value[i] = Double.longBitsToDouble(Util.longFromBytes(payload, 8 * i, 8));
				}
				record = Record.doubleArrayRecord(timestamp, value);
			}
			case "string[]" -> {
				int arrayLength = Util.intFromBytes(payload, 0, 4);
				String[] value = new String[arrayLength];
				int payloadIndex = 4;
				for (int i = 0; i < value.length; ++i) {
					int stringLength = Util.intFromBytes(payload, payloadIndex, 4);
					payloadIndex += 4;
					value[i] = Util.utf8StringFromBytes(payload, payloadIndex, stringLength);
					payloadIndex += stringLength;
				}
				if (payloadIndex != payload.length) {
					logWarning("Warning: string array did not consume last " + (payload.length - payloadIndex) + " bytes of the payload");
				}
				record = Record.stringArrayRecord(timestamp, value);
			}
			default -> {
				record = Record.unknownRecord(timestamp, payload);
			}
		}
		entry.addRecord(timestamp, record);
		logDebug("Done with log value");
	}
}

package wpilogviewer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PrintLogger implements Logger {
	private static class Entry {
		public final long id;
		public final String name;
		public final String type;
		public String metadata;

		public Entry(long id, String name, String type, String metadata) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.metadata = metadata;
		}
	}

	private final String nameFilter;
	private final boolean logControl;
	private final boolean logValue;
	private final Map<Long, Entry> idToEntry = new HashMap<>();

	public PrintLogger(String nameFilter, boolean logControl, boolean logValue) {
		this.nameFilter = nameFilter;
		this.logControl = logControl;
		this.logValue = logValue;
	}

	public void logStart(long entryId, String entryName, String entryType, String entryMetadata, long timestamp) {
		if (idToEntry.containsKey(entryId)) {
			var oldEntry = idToEntry.get(entryId);
			System.out.println("Note: Overriding existing entry with id " + entryId + " and name " + oldEntry.name + "!");
		}
		var entry = new Entry(entryId, entryName, entryType, entryMetadata);
		idToEntry.put(entryId, entry);
		if (!logControl) {
			return;
		}
		System.out.println("Got Start record at " + timestamp + " for entry ID " + entry.id + ", name \"" + entry.name + "\", type \"" + entry.type + "\", and metadata \"" + entry.metadata + "\"");
	}

	public void logFinish(long entryId, long timestamp) {
		if (!idToEntry.containsKey(entryId)) {
			System.err.println("Could not end entry with non-existent ID " + entryId + "!");
			return;
		}
		var entry = idToEntry.remove(entryId);
		if (!logControl) {
			return;
		}
		System.out.println("Got Finish record at " + timestamp + " for entry ID " + entry.id + " (name \"" + entry.name + "\")");
	}

	public void logSetMetadata(long entryId, long timestamp, String newMetadata) {
		if (!idToEntry.containsKey(entryId)) {
			System.err.println("Could not set metadata of entry with non-existent ID " + entryId + "!");
			return;
		}
		var entry = idToEntry.get(entryId);
		entry.metadata = newMetadata;
		if (!logControl) {
			return;
		}
		System.out.println("Got Set Metadata record at " + timestamp + " for entry ID " + entry.id + " (name \"" + entry.name + "\") to \"" + newMetadata + "\"");
	}

	public void logValue(long entryId, long timestamp, Supplier<byte[]> payloadSupplier) {
		// If we don't log values, we can completely skip getting the entry
		if (!logValue) {
			return;
		}
		if (!idToEntry.containsKey(entryId)) {
			System.err.println("Cannot log to entry with non-existent ID " + entryId + "!");
			return;
		}
		var entry = idToEntry.get(entryId);
		if (nameFilter != null && !entry.name.equals(nameFilter)) {
			return;
		}
		byte[] payload = payloadSupplier.get();
		boolean knownType = true;
		final String valueString;
		switch (entry.type) {
			case "raw", "rawBytes" -> {
				valueString = Arrays.toString(payload);
			}
			case "boolean" -> {
				if (payload.length != 1) {
					System.out.println("Got invalid payload for boolean entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				if (payload[0] < 0 || payload[0] > 1) {
					System.out.println("Got invalid payload for boolean entry " + entry.id + ": " + payload[0] + "!");
					return;
				}
				boolean value = payload[0] == 1;
				valueString = String.valueOf(value);
			}
			case "int64" -> {
				if (payload.length != 8) {
					System.out.println("Got invalid payload for int64 entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				long value = Util.longFromBytes(payload);
				valueString = String.valueOf(value);
			}
			case "float" -> {
				if (payload.length != 4) {
					System.out.println("Got invalid payload for float entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				float value = Float.intBitsToFloat(Util.intFromBytes(payload));
				valueString = String.valueOf(value);
			}
			case "double" -> {
				if (payload.length != 8) {
					System.out.println("Got invalid payload for double entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				double value = Double.longBitsToDouble(Util.longFromBytes(payload));
				valueString = String.valueOf(value);
			}
			case "json", "string" -> {
				String value = Util.utf8StringFromBytes(payload);
				valueString = "\"" + value + "\"";
			}
			case "boolean[]" -> {
				boolean[] value = new boolean[payload.length];
				for (int i = 0; i < payload.length; ++i) {
					if (payload[i] < 0 || payload[i] > 1) {
						System.out.println("Got invalid payload for boolean array entry " + entry.id + ": " + Arrays.toString(payload));
						return;
					}
					value[i] = payload[i] == 1;
				}
				valueString = Arrays.toString(value);
			}
			case "int64[]" -> {
				if (payload.length % 8 != 0) {
					System.out.println("Got invalid payload for int64[] entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				long[] value = new long[payload.length / 8];
				for (int i = 0; i < value.length; ++i) {
					value[i] = Util.longFromBytes(payload, 8 * i, 8);
				}
				valueString = Arrays.toString(value);
			}
			case "float[]" -> {
				if (payload.length % 4 != 0) {
					System.out.println("Got invalid payload for float[] entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				float[] value = new float[payload.length / 4];
				for (int i = 0; i < value.length; ++i) {
					value[i] = Float.intBitsToFloat(Util.intFromBytes(payload, 4 * i, 4));
				}
				valueString = Arrays.toString(value);
			}
			case "double[]" -> {
				if (payload.length % 8 != 0) {
					System.out.println("Got invalid payload for double[] entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
					return;
				}
				double[] value = new double[payload.length / 8];
				for (int i = 0; i < value.length; ++i) {
					value[i] = Double.longBitsToDouble(Util.longFromBytes(payload, 8 * i, 8));
				}
				valueString = Arrays.toString(value);
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
					System.out.println("Warning: string array did not consume last " + (payload.length - payloadIndex) + " bytes of the payload");
				}
				valueString = Arrays.toString(value);
			}
			default -> {
				knownType = false;
				valueString = Arrays.toString(payload);
			}
		}
		if (knownType) {
			System.out.println("entry " + entry.id + " (type " + entry.type + ") at " + timestamp + " got value " + valueString);
		} else {
			System.out.println("entry " + entry.id + " (unknown type " + entry.type + ") at " + timestamp + " got value " + valueString);
		}
	}
}

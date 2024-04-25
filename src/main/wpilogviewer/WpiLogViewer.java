import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WpiLogViewer {
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

	private static class Logger {
		private final String nameFilter;
		private final boolean logControl;
		private final boolean logValue;

		public Logger(String nameFilter, boolean logControl, boolean logValue) {
			this.nameFilter = nameFilter;
			this.logControl = logControl;
			this.logValue = logValue;
		}

		public void logStart(Entry entry, long timestamp) {
			if (!logControl) {
				return;
			}
			System.out.println("Got Start record at " + timestamp + " for entry ID " + entry.id + ", name \"" + entry.name + "\", type \"" + entry.type + "\", and metadata \"" + entry.metadata + "\"");
		}

		public void logFinish(Entry entry, long timestamp) {
			if (!logControl) {
				return;
			}
			System.out.println("Got Finish record at " + timestamp + " for entry ID " + entry.id + " (name \"" + entry.name + "\")");
		}

		public void logSetMetadata(Entry entry, long timestamp, String newMetadata) {
			if (!logControl) {
				return;
			}
			System.out.println("Got Set Metadata record at " + timestamp + " for entry ID " + entry.id + " (name \"" + entry.name + "\") to \"" + newMetadata + "\"");
		}

		public void logValue(Entry entry, long timestamp, ByteReader payloadSupplier) throws IOException {
			if (!logValue) {
				return;
			}
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
					long value = longFromBytes(payload);
					valueString = String.valueOf(value);
				}
				case "float" -> {
					if (payload.length != 4) {
						System.out.println("Got invalid payload for float entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
						return;
					}
					float value = Float.intBitsToFloat(intFromBytes(payload));
					valueString = String.valueOf(value);
				}
				case "double" -> {
					if (payload.length != 8) {
						System.out.println("Got invalid payload for double entry " + entry.id + " of size " + payload.length + "! (" + Arrays.toString(payload) + ")");
						return;
					}
					double value = Double.longBitsToDouble(longFromBytes(payload));
					valueString = String.valueOf(value);
				}
				case "json", "string" -> {
					String value = utf8StringFromBytes(payload);
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
						value[i] = longFromBytes(payload, 8 * i, 8);
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
						value[i] = Float.intBitsToFloat(intFromBytes(payload, 4 * i, 4));
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
						value[i] = Double.longBitsToDouble(longFromBytes(payload, 8 * i, 8));
					}
					valueString = Arrays.toString(value);
				}
				case "string[]" -> {
					int arrayLength = intFromBytes(payload, 0, 4);
					String[] value = new String[arrayLength];
					int payloadIndex = 4;
					for (int i = 0; i < value.length; ++i) {
						int stringLength = intFromBytes(payload, payloadIndex, 4);
						payloadIndex += 4;
						value[i] = utf8StringFromBytes(payload, payloadIndex, stringLength);
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
			

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: java WpiLogViewer.java <file> [-topic <topic>]");
		}
		String fileName = null;
		String topicFilter = null;
		boolean logControl = true;
		boolean logValue = true;
		boolean argIsTopic = false;
		for (String arg : args) {
			if (arg.equals("-topic")) {
				argIsTopic = true;
			} else if (arg.equals("-control")) {
				logControl = true;
			} else if (arg.equals("-nocontrol")) {
				logControl = false;
			} else if (arg.equals("-value")) {
				logValue = true;
			} else if (arg.equals("-novalue")) {
				logValue = false;
			} else {
				if (argIsTopic) {
					topicFilter = arg;
					argIsTopic = false;
				} else {
					if (fileName != null) {
						System.err.println("Cannot specify multiple files!");
						return;
					} else {
						fileName = arg;
					}
				}
			}
		}
		var logger = new Logger(topicFilter, logControl, logValue);
		if (fileName.equals("-")) {
			try {
				processLog(System.in, logger);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			try {
				try (var inputStream = new FileInputStream(fileName)) {
					try {
						processLog(inputStream, logger);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			} catch (FileNotFoundException e) {
				throw new UncheckedIOException(e);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static int intFromBytes(byte[] bytes) {
		int value = 0;
		if (bytes.length > 4) {
			System.err.println("Byte array size of " + bytes.length + " is too big for an int!");
			return -1;
		}
		for (int i = bytes.length - 1; i >= 0; --i) {
			value <<= 8;
			value |= ((int) bytes[i]) & 0xFF;
		}
		return value;
	}

	private static int intFromBytes(byte[] bytes, int start, int length) {
		if (length > 4) {
			System.err.println("length of " + length + " is too big for a int!");
			return -1;
		}
		if (length > bytes.length - start) {
			length = bytes.length - start;
		}
		int value = 0;
		for (int i = start + length - 1; i >= start; --i) {
			value <<= 8;
			value |= ((int) bytes[i]) & 0xFF;
		}
		return value;	
	}	

	private static long longFromBytes(byte[] bytes) {
		if (bytes.length > 8) {
			System.err.println("Byte array size of " + bytes.length + " is too big for a long!");
			return -1;
		}
		long value = 0;
		for (int i = bytes.length - 1; i >= 0; --i) {
			value <<= 8;
			value |= ((long) bytes[i]) & 0xFF;
		}
		return value;
	}

	private static long longFromBytes(byte[] bytes, int start, int length) {
		if (length > 8) {
			System.err.println("length of " + length + " is too big for a long!");
			return -1;
		}
		if (length > bytes.length - start) {
			length = bytes.length - start;
		}
		long value = 0;
		for (int i = start + length - 1; i >= start; --i) {
			value <<= 8;
			value |= ((long) bytes[i]) & 0xFF;
		}
		return value;
	}

	private static String utf8StringFromBytes(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static String utf8StringFromBytes(byte[] bytes, int start, int length) {
		return new String(bytes, start, length, StandardCharsets.UTF_8);
	}

	private static int readInt(InputStream input, int length) throws IOException {
		return intFromBytes(input.readNBytes(length));
	}

	private static long readLong(InputStream input, int length) throws IOException {
		return longFromBytes(input.readNBytes(length));
	}

	private static String readUtf8String(InputStream input, int length) throws IOException {
		return utf8StringFromBytes(input.readNBytes(length));
	}

	public static void processLog(InputStream data, Logger logger) throws IOException {
		final var input = data;
		Map<Long, Entry> idToEntry = new HashMap<>();
		// Process header
		// Expect WPILOG
		byte[] wpilogHeaderBytes = data.readNBytes(6);
		var wpilogHeader = new String(wpilogHeaderBytes, StandardCharsets.US_ASCII);
		if (!wpilogHeader.equals("WPILOG")) {
			System.err.println("First 6 bytes " + Arrays.toString(wpilogHeaderBytes) + " (\"" + wpilogHeader + "\") did not match \"WPILOG\"!");
			return;
		}
		// Check version
		int versionMinor = readInt(input, 1);
		int versionMajor = readInt(input, 1);
		System.out.println("Version number " + versionMajor + "." + versionMinor);
		// Handle extra header (just ignore it)
		int extraHeaderLength = readInt(input, 4);
		byte[] extraHeaderBytes = data.readNBytes(extraHeaderLength);
		System.out.println("Extra header: \"" + new String(extraHeaderBytes, StandardCharsets.UTF_8) + "\"");
		// Process data
		while (true) {
			int headerLengthBitfield = data.read();
			if (headerLengthBitfield == -1) {
				System.out.println("<DONE>");
				return;
			}

			int entryIdLength = 1 + (headerLengthBitfield & 0b11);
			int payloadSizeLength = 1 + ((headerLengthBitfield >> 2) & 0b11);
			int timestampLength = 1 + ((headerLengthBitfield >> 4) & 0b111);
			if ((headerLengthBitfield >> 7) != 0) {
				System.out.println("Invalid header length bitfield " + headerLengthBitfield);
				return;
			}

			long entryId = readLong(input, entryIdLength);
			int payloadSize = readInt(input, payloadSizeLength);
			long timestamp = readLong(input, timestampLength);

			if (entryId == 0) {
				// Control record
				int type = data.read();
				if (type == 0) {
					// Start record
					long newEntryId = readLong(input, 4);
					int newEntryNameLength = readInt(input, 4);
					var newEntryName = readUtf8String(input, newEntryNameLength);
					int newEntryTypeLength = readInt(input, 4);
					var newEntryType = readUtf8String(input, newEntryTypeLength);
					int newEntryMetadataLength = readInt(input, 4);
					var newEntryMetadata = readUtf8String(input, newEntryMetadataLength);
					var entry = new Entry(newEntryId, newEntryName, newEntryType, newEntryMetadata);
					logger.logStart(entry, timestamp);
					idToEntry.put(newEntryId, entry);
				} else if (type == 1) {
					// Finish record
					int finishedEntryId = readInt(input, 4);
					if (!idToEntry.containsKey(finishedEntryId)) {
						System.err.println("Could not end entry with non-existent ID " + finishedEntryId + "!");
					} else {
						var entry = idToEntry.remove(finishedEntryId);
						logger.logFinish(entry, timestamp);
					}
				} else if (type == 2) {
					// Set metadata record
					long updateEntryId = readLong(input, 4);
					int updateEntryMetadataLength = readInt(input, 4);
					String updateEntryMetadata = readUtf8String(input, updateEntryMetadataLength);
					if (!idToEntry.containsKey(updateEntryId)) {
						System.err.println("Could not set metadata of entry with non-existent ID " + updateEntryId + "!");
					} else {
						var entry = idToEntry.get(updateEntryId);
						entry.metadata = updateEntryMetadata;
						logger.logSetMetadata(entry, timestamp, updateEntryMetadata);
					}
				} else {
					System.err.println("Unknown control record with type " + type + "! Aborting");
					return;
				}
			} else {
				// Non-control record
				var entry = idToEntry.get(entryId);
				ByteReader payloadSupplier = new ByteReader(input, payloadSize);
				logger.logValue(entry, timestamp, payloadSupplier);
				payloadSupplier.finish();
			}
		}
	}

	private static class ByteReader {
		private boolean wasPolled = false;
		private byte[] value;
		private final InputStream input;
		private final int numBytes;

		public ByteReader(InputStream input, int numBytes) {
			this.input = input;
			this.numBytes = numBytes;
		}

		public void finish() throws IOException {
			if (!wasPolled) {
				input.skip(numBytes);
				wasPolled = true;
			}
		}

		public byte[] get() throws IOException {
			if (!wasPolled) {
				value = input.readNBytes(numBytes);
				wasPolled = true;
			}
			return value;
		}
	}
}
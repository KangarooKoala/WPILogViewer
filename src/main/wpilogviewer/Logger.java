package wpilogviewer;

import java.io.IOException;
import java.util.Arrays;

public class Logger {
	private final String nameFilter;
	private final boolean logControl;
	private final boolean logValue;

	public Logger(String nameFilter, boolean logControl, boolean logValue) {
		this.nameFilter = nameFilter;
		this.logControl = logControl;
		this.logValue = logValue;
	}

	public void logStart(WpiLogEntry entry, long timestamp) {
		if (!logControl) {
			return;
		}
		System.out.println("Got Start record at " + timestamp + " for entry ID " + entry.id + ", name \"" + entry.name + "\", type \"" + entry.type + "\", and metadata \"" + entry.metadata + "\"");
	}

	public void logFinish(WpiLogEntry entry, long timestamp) {
		if (!logControl) {
			return;
		}
		System.out.println("Got Finish record at " + timestamp + " for entry ID " + entry.id + " (name \"" + entry.name + "\")");
	}

	public void logSetMetadata(WpiLogEntry entry, long timestamp, String newMetadata) {
		if (!logControl) {
			return;
		}
		System.out.println("Got Set Metadata record at " + timestamp + " for entry ID " + entry.id + " (name \"" + entry.name + "\") to \"" + newMetadata + "\"");
	}

	public void logValue(WpiLogEntry entry, long timestamp, ByteReader payloadSupplier) throws IOException {
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

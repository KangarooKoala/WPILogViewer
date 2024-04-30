package wpilogviewer;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WpiLogProcessor {
	private static int readInt(InputStream input, int length) throws IOException {
		return Util.intFromBytes(input.readNBytes(length));
	}

	private static long readLong(InputStream input, int length) throws IOException {
		return Util.longFromBytes(input.readNBytes(length));
	}

	private static String readUtf8String(InputStream input, int length) throws IOException {
		return Util.utf8StringFromBytes(input.readNBytes(length));
	}

	public static void process(InputStream data, Logger logger) throws IOException {
		final var input = data;
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
					logger.logStart(newEntryId, newEntryName, newEntryType, newEntryMetadata, timestamp);
				} else if (type == 1) {
					// Finish record
					int finishedEntryId = readInt(input, 4);
					logger.logFinish(finishedEntryId, timestamp);
				} else if (type == 2) {
					// Set metadata record
					long updateEntryId = readLong(input, 4);
					int updateEntryMetadataLength = readInt(input, 4);
					String updateEntryMetadata = readUtf8String(input, updateEntryMetadataLength);
					logger.logSetMetadata(updateEntryId, timestamp, updateEntryMetadata);
				} else {
					System.err.println("Unknown control record with type " + type + "! Aborting");
					return;
				}
			} else {
				// Non-control record
				ByteReader payloadSupplier = new ByteReader(input, payloadSize);
				logger.logValue(entryId, timestamp, payloadSupplier::getUnchecked);
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

		public byte[] getUnchecked() {
			try {
				return get();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}
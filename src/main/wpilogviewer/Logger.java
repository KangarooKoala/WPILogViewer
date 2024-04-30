package wpilogviewer;

import java.util.function.Supplier;

public interface Logger {
	void logStart(long entryId, String entryName, String entryType, String entryMetadata, long timestamp);

	void logFinish(long entryId, long timestamp);

	void logSetMetadata(long entryId, long timestamp, String newMetadata);

	void logValue(long entryId, long timestamp, Supplier<byte[]> payloadSupplier);
}
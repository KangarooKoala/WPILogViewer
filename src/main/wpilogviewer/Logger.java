package wpilogviewer;

import java.util.function.Supplier;

public interface Logger {
	void logStart(WpiLogEntry entry, long timestamp);

	void logFinish(WpiLogEntry entry, long timestamp);

	void logSetMetadata(WpiLogEntry entry, long timestamp, String newMetadata);

	void logValue(WpiLogEntry entry, long timestamp, Supplier<byte[]> payloadSupplier);
}
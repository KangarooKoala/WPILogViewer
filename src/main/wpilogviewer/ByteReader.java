package wpilogviewer;

import java.io.InputStream;
import java.io.IOException;

public class ByteReader {
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
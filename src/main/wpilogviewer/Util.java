package wpilogviewer;

import java.nio.charset.StandardCharsets;

public class Util {
	public static int intFromBytes(byte[] bytes) {
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

	public static int intFromBytes(byte[] bytes, int start, int length) {
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

	public static long longFromBytes(byte[] bytes) {
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

	public static long longFromBytes(byte[] bytes, int start, int length) {
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

	public static String utf8StringFromBytes(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static String utf8StringFromBytes(byte[] bytes, int start, int length) {
		return new String(bytes, start, length, StandardCharsets.UTF_8);
	}
}
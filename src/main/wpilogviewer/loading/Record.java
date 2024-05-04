package wpilogviewer.loading;

public class Record {
	public enum Type {
		RAW,
		BOOLEAN,
		INT64,
		FLOAT,
		DOUBLE,
		STRING,
		BOOLEAN_ARRAY,
		INT64_ARRAY,
		FLOAT_ARRAY,
		DOUBLE_ARRAY,
		STRING_ARRAY,
		UNKNOWN;
	}

	public static Record rawRecord(long timestamp, byte[] value) {
		return new Record(timestamp, Type.RAW, value, false, 0, 0f, 0d, null, null, null, null, null, null);
	}

	public static Record booleanRecord(long timestamp, boolean value) {
		return new Record(timestamp, Type.BOOLEAN, null, value, 0, 0f, 0d, null, null, null, null, null, null);
	}

	public static Record int64Record(long timestamp, long value) {
		return new Record(timestamp, Type.INT64, null, false, value, 0f, 0d, null, null, null, null, null, null);
	}

	public static Record floatRecord(long timestamp, float value) {
		return new Record(timestamp, Type.FLOAT, null, false, 0, value, 0d, null, null, null, null, null, null);
	}

	public static Record doubleRecord(long timestamp, double value) {
		return new Record(timestamp, Type.DOUBLE, null, false, 0, 0f, value, null, null, null, null, null, null);
	}

	public static Record stringRecord(long timestamp, String value) {
		return new Record(timestamp, Type.STRING, null, false, 0, 0f, 0d, value, null, null, null, null, null);
	}

	public static Record booleanArrayRecord(long timestamp, boolean[] value) {
		return new Record(timestamp, Type.BOOLEAN_ARRAY, null, false, 0, 0f, 0d, null, value, null, null, null, null);
	}

	public static Record int64ArrayRecord(long timestamp, long[] value) {
		return new Record(timestamp, Type.INT64_ARRAY, null, false, 0, 0f, 0d, null, null, value, null, null, null);
	}

	public static Record floatArrayRecord(long timestamp, float[] value) {
		return new Record(timestamp, Type.FLOAT_ARRAY, null, false, 0, 0f, 0d, null, null, null, value, null, null);
	}

	public static Record doubleArrayRecord(long timestamp, double[] value) {
		return new Record(timestamp, Type.DOUBLE_ARRAY, null, false, 0, 0f, 0d, null, null, null, null, value, null);
	}

	public static Record stringArrayRecord(long timestamp, String[] value) {
		return new Record(timestamp, Type.STRING_ARRAY, null, false, 0, 0f, 0d, null, null, null, null, null, value);
	}

	public static Record unknownRecord(long timestamp, byte[] value) {
		return new Record(timestamp, Type.UNKNOWN, value, false, 0, 0f, 0d, null, null, null, null, null, null);
	}

	private final long timestamp;
	private final Type type;
	private final byte[] rawValue;
	private final boolean booleanValue;
	private final long int64Value;
	private final float floatValue;
	private final double doubleValue;
	private final String stringValue;
	private final boolean[] booleanArrayValue;
	private final long[] int64ArrayValue;
	private final float[] floatArrayValue;
	private final double[] doubleArrayValue;
	private final String[] stringArrayValue;

	private Record(
			long timestamp, Type type, byte[] rawValue, boolean booleanValue,
			long int64Value, float floatValue, double doubleValue, String stringValue,
			boolean[] booleanArrayValue, long[] int64ArrayValue,
			float[] floatArrayValue, double[] doubleArrayValue,
			String[] stringArrayValue) {
		this.timestamp = timestamp;
		this.type = type;
		this.rawValue = rawValue;
		this.booleanValue = booleanValue;
		this.int64Value = int64Value;
		this.floatValue = floatValue;
		this.doubleValue = doubleValue;
		this.stringValue = stringValue;
		this.booleanArrayValue = booleanArrayValue;
		this.int64ArrayValue = int64ArrayValue;
		this.floatArrayValue = floatArrayValue;
		this.doubleArrayValue = doubleArrayValue;
		this.stringArrayValue = stringArrayValue;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Type getType() {
		return type;
	}
}
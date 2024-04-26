package wpilogviewer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;

public class WpiLogViewer {
	private static final String USAGE = "Usage: ./run [-h] [-topic <topic>] [-control] [-nocontrol] [-value] [-novalue] <file>";

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println(USAGE);
			return;
		}
		String fileName = null;
		String topicFilter = null;
		boolean logControl = true;
		boolean logValue = true;
		boolean argIsTopic = false;
		boolean help = false;
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
			} else if (arg.equals("-h") || arg.equals("--help") || arg.equals("-?")) {
				help = true;
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
		if (help) {
			System.out.println(USAGE);
			return;
		}
		var logger = new Logger(topicFilter, logControl, logValue);
		if (fileName.equals("-")) {
			try {
				WpiLogProcessor.process(System.in, logger);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			try {
				try (var inputStream = new FileInputStream(fileName)) {
					try {
						WpiLogProcessor.process(inputStream, logger);
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
}
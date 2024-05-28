package wpilogviewer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import wpilogviewer.loading.Loader;

public class Main {
	private enum Subcommand {
		PRINT, SHELL;
	}

	private static final String PRINT_USAGE = "wpilogviewer print [-h] [-topic <topic>] [-control] [-nocontrol] [-value] [-novalue] <file>";
	private static final String SHELL_USAGE = "wpilogviewer shell [-h] <file>";
	private static final String USAGE = "Usage:\n\t" + PRINT_USAGE + "\n\t" + SHELL_USAGE;

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println(USAGE);
			return;
		}
		boolean help = false;
		boolean hasError = false;
		Subcommand subcommand = null;
		int start = 0;
		for (; start < args.length; ++start) {
			String arg = args[start];
			if (arg.equals("-h") || arg.equals("--help") || arg.equals("-?")) {
				help = true;
				break;
			} else if (arg.equals("print")) {
				subcommand = Subcommand.PRINT;
				break;
			} else if (arg.equals("shell")) {
				subcommand = Subcommand.SHELL;
				break;
			} else {
				System.err.println("Unknown subcommand " + arg + "!");
				hasError = true;
			}
		}
		if (hasError) {
			return;
		}
		if (help) {
			System.out.println(USAGE);
			return;
		}
		if (subcommand == null) {
			System.err.println("Must specify a subcommand!");
			return;
		}
		switch (subcommand) {
			case PRINT -> printMain(args, start + 1);
			case SHELL -> shellMain(args, start + 1);
		}
	}

	private static void printMain(String[] args, int start) {
		String fileName = null;
		String topicFilter = null;
		boolean logControl = true;
		boolean logValue = true;
		boolean argIsTopic = false;
		boolean help = false;
		for (int i = start; i < args.length; ++i) {
			String arg = args[i];
			if (arg.equals("-h")) {
				help = true;
				break;
			} else if (arg.equals("-topic")) {
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
		if (help) {
			System.out.println(PRINT_USAGE);
			return;
		}
		if (fileName == null) {
			System.err.println("Must specify an input file!");
			return;
		}
		var logger = new PrintLogger(topicFilter, logControl, logValue);
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

	private static void shellMain(String[] args, int start) {
		String fileName = null;
		boolean help = false;
		for (int i = start; i < args.length; ++i) {
			String arg = args[i];
			if (arg.equals("-h")) {
				help = true;
				break;
			} else {
				if (fileName != null) {
					System.err.println("Cannot specify multiple files!");
					return;
				}
				fileName = arg;
			}
		}
		if (help) {
			System.out.println(SHELL_USAGE);
			return;
		}
		if (fileName == null) {
			System.err.println("Must specify an input file!");
			return;
		}
		if (fileName.equals("-")) {
			try {
				shellProcessInputStream(System.in);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			try {
				try (var inputStream = new FileInputStream(fileName)) {
					try {
						shellProcessInputStream(inputStream);
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

	private static void shellProcessInputStream(InputStream inputStream) throws IOException {
		var loader = new Loader(inputStream, Loader.Verbosity.NORMAL);
		System.out.println("Loading input...");
		loader.load();
		System.out.println("Listing " + loader.getIds().size() + " entries:");
		for (long id : loader.getIds()) {
			for (long timestamp : loader.getEntryStartTimestamps(id)) {
				var entry = loader.getEntry(id, timestamp);
				System.out.println("Id: " + id + ", timestamp: " + timestamp + ", entry name: " + entry.getName());
			}
		}
	}
}
# We assume we're in the project root directory

# 0: quiet; 1: normal; 2: verbose
$verbosity = 1

foreach ($arg in $ARGS) {
	if ($arg -eq '-q') {
		if ($verbosity -gt 1) {
			echo 'Cannot specify -q with -v'
			exit 1
		}
		$verbosity = 0
	} elseif ($arg -eq '-v') {
		if ($verbosity -lt 1) {
			echo 'Cannot specify -v with -q'
			exit 1
		}
		$verbosity = 2
	} else {
		echo "Unknown option '$arg'"
		exit 1
	}
}

if ($verbosity -ge 2) {
	echo 'Setting up build directory'
}

mkdir -force build/class | out-null

rm -r build/class/*

if ($verbosity -ge 2) {
	echo 'Loading source files'
}

$srcfiles = ls -r -file src/*.java | % { $_.ToString() }

if ($verbosity -ge 1) {
	echo "Compiling $($srcfiles.Length) files"
}

javac -d build/class $srcfiles

if ($verbosity -ge 1) {
	echo 'Generating jar'
}

jar --create --file build/program.jar --main-class=wpilogviewer.Main -C build/class/ .

if ($verbosity -ge 1) {
	echo 'Done'
}

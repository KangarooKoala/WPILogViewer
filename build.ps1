# We assume we're in the project root directory

# 0: quiet; 1: normal; 2: verbose
[int]$verbosity = 1
[bool]$setverbosity = $false

foreach ($arg in $ARGS) {
	if ($arg -ceq '-q') {
		if ($setverbosity) {
			echo 'Cannot use -q with other verbosity specifiers'
			exit 1
		}
		$verbosity = 0
		$setverbosity = $true
	} elseif ($arg -ceq '-v') {
		if ($setverbosity) {
			echo 'Cannot specify -v with other verbosity specifiers'
			exit 1
		}
		$verbosity = 2
		$setverbosity = $true
	} elseif ($arg -clike '-v=*') {
		if ($setverbosity) {
			echo 'Cannot use -v=<verbosity> with other verbosity specifiers'
			exit 1
		}
		[string]$numbertext = $arg.Remove(0, '-v='.Length);
		if ($numbertext.Length -eq 0) {
			echo "Got empty -v=<verbosity> specifier: '$arg'"
			exit 1
		}
		for ([int]$i = 0; $i -lt $numbertext.Length; ++$i) {
			if ($numbertext[$i] -cnotlike '[0123456789]') {
				echo "Got non-numeric -v=<verbosity> specifier: '$arg'"
				exit 1
			}
		}
		$verbosity = [int]$numbertext
		if ($verbosity -lt 0 -or $verbosity -gt 2) {
			echo "WARNING: Building with nonstandard verbosity $verbosity"
		}
		$setverbosity = $true
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

[string[]]$srcfiles = ls -r -file src/*.java | % { $_.ToString() }

if ($verbosity -ge 1) {
	echo "Compiling $($srcfiles.Length) files"
}

javac -d build/class $srcfiles

if ($verbosity -ge 1) {
	echo 'Generating jar'
}

jar --create --file build/program.jar --main-class=wpilogviewer.Main -C build/class/ .

if ($verbosity -ge 1) {
	echo 'Done building'
}

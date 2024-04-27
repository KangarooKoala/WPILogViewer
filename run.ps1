# We assume we're in the project root directory

[string[]]$programargs = @()

[bool]$build = $true
# 0: quiet; 1: normal; 2: verbose
[int]$verbosity = 1

for ([int]$i = 0; $i -lt $ARGS.Length; ++$i) {
	[string]$arg = $ARGS[$i]
	if ($arg -eq '-Sskipbuild') {
		$build = $false
	} elseif ($arg -eq '-Sq' -or $arg -eq '-Squiet') {
		if ($verbosity -gt 1) {
			echo 'Cannot specify -Sq with -Sv'
			exit 1
		}
		$verbosity = 0
	} elseif ($arg -eq '-Sv' -or $arg -eq '-Sverbose') {
		if ($verbosity -lt 1) {
			echo 'Cannot specify -Sv with -Sq'
			exit 1
		}
		$verbosity = 2
	} elseif ($arg -clike '-S*') {
		echo "Got unknown script argument '$arg'"
		return 1
	} else {
		$programargs = $ARGS[$i..($ARGS.Length-1)]
		break
	}
}

if ($verbosity -notin @(0, 1, 2)) {
	echo "WARNING: Unknown verbosity $verbosity! Running anyways"
}

if ($build) {
	if ($verbosity -ge 1) {
		echo 'Building'
	}
	if ($verbosity -eq 0) {
		./build -q
	} elseif ($verbosity -eq 1) {
		./build
	} elseif ($verbosity -eq 2) {
		./build -v
	} else {
		# We already warned the user
		./build
	}
}

if ($verbosity -ge 2) {
	echo "Running program with arguments '$programargs'"
}

java -jar build/program.jar $programargs
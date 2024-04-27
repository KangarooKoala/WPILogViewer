# We assume we're in the project root directory

[string[]]$programargs = @()

[bool]$build = $true
# 1: normal; 2: verbose
[int]$verbosity = 1

for ([int]$i = 0; $i -lt $ARGS.Length; ++$i) {
	[string]$arg = $ARGS[$i]
	if ($arg -eq '-Sskipbuild') {
		$build = $false
	} elseif ($arg -eq '-Sv' -or $arg -eq '-Sverbose') {
		$verbosity = 2
	} elseif ($arg -clike '-S*') {
		echo "Got unknown script argument '$arg'"
		return 1
	} else {
		$programargs = $ARGS[$i..($ARGS.Length-1)]
		break
	}
}

if ($build) {
	if ($verbosity -ge 1) {
		echo 'Building'
	}
	./build
}

if ($verbosity -ge 2) {
	echo "Running program with arguments '$programargs'"
}

java -jar build/program.jar $programargs
# We assume we're in the project root directory

[string[]]$programargs = @()

[bool]$build = $true
# 0: quiet; 1: normal; 2: verbose
[int]$verbosity = 1
[bool]$setverbosity = $false

for ([int]$i = 0; $i -lt $ARGS.Length; ++$i) {
	[string]$arg = $ARGS[$i]
	if ($arg -ceq '-Sskipbuild') {
		$build = $false
	} elseif ($arg -ceq '-Sq' -or $arg -ceq '-Squiet') {
		if ($setverbosity) {
			echo 'Cannot use -Sq with other verbosity specifiers'
			exit 1
		}
		$verbosity = 0
		$setverbosity = $true
	} elseif ($arg -ceq '-Sv' -or $arg -ceq '-Sverbose') {
		if ($setverbosity) {
			echo 'Cannot specify -Sv with other verbosity specifiers'
			exit 1
		}
		$verbosity = 2
		$setverbosity = $true
	} elseif ($arg -clike '-Sv=*') {
		if ($setverbosity) {
			echo 'Cannot use -Sv=<verbosity> with other verbosity specifiers'
			exit 1
		}
		[string]$numbertext = $arg.Remove(0, '-Sv='.Length);
		if ($numbertext.Length -eq 0) {
			echo "Got empty -Sv=<verbosity> specifier: '$arg'"
			exit 1
		}
		for ([int]$i = 0; $i -lt $numbertext.Length; ++$i) {
			if ($numbertext[$i] -cnotlike '[0123456789]') {
				echo "Got non-numeric -Sv=<verbosity> specifier: '$arg'"
				exit 1
			}
		}
		$verbosity = [int]$numbertext
		if ($verbosity -lt 0 -or $verbosity -gt 2) {
			echo "WARNING: Running with nonstandard verbosity $verbosity"
		}
		$setverbosity = $true
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
	if ($setverbosity) {
		./build "-v=$verbosity"
	} else {
		./build
	}
}

if ($verbosity -ge 2) {
	echo "Running program with arguments '$programargs'"
}

java -jar build/program.jar $programargs
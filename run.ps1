# We assume we're in the project root directory

$programargs = @()

$build = $true

for ([int]$i = 0; $i -lt $ARGS.Length; ++$i) {
	$arg = $ARGS[$i]
	if ($arg -eq '-Sskipbuild') {
		$build = $false
	} elseif ($arg -clike '-S*') {
		echo "Got unknown script argument '$arg'"
		return 1
	} else {
		$programargs = $ARGS[$i..($ARGS.Length-1)]
		break
	}
}

if ($build) {
	./build
}

java -jar build/program.jar $programargs
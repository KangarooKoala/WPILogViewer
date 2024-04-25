# We assume we're in the project root directory

mkdir -force build/class | out-null

$srcfiles = ls -r -file src/*.java | % { $_.ToString() }

echo "Source files"
echo $srcfiles
echo "DONE"

javac -d build/class $srcfiles

jar --create --file build/program.jar --main-class=WpiLogViewer -C build/class/ .

# We assume we're in the project root directory

mkdir -force build/class | out-null

rm -r build/class/*

$srcfiles = ls -r -file src/*.java | % { $_.ToString() }

javac -d build/class $srcfiles

jar --create --file build/program.jar --main-class=wpilogviewer.WpiLogViewer -C build/class/ .

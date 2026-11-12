#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
compiler=(java -m jdk.compiler/com.sun.tools.javac.Main)
if command -v javac >/dev/null 2>&1; then compiler=(javac); fi
"${compiler[@]}" -source 17 -target 17 -Xlint:all,-options -Werror -d "$classes" \
 app/src/main/java/com/rikodev/music/android/dna/genome/*.java \
 app/src/main/java/com/rikodev/music/android/dna/analysis/*.java \
 app/src/main/java/com/rikodev/music/android/dna/render/OrganismCamera.java \
 tools/CameraCheck.java tools/DnaShareCheck.java
java -cp "$classes" CameraCheck
java -cp "$classes" com.rikodev.music.android.dna.analysis.DnaShareCheck

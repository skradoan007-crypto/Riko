#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_classes="$(mktemp -d)"
trap 'rm -rf "$test_classes"' EXIT
compiler=(java -m jdk.compiler/com.sun.tools.javac.Main)
if command -v javac >/dev/null 2>&1; then compiler=(javac); fi
"${compiler[@]}" -source 17 -target 17 -Xlint:all,-options -Werror -d "$test_classes" \
  app/src/main/java/com/rikodev/music/android/audiofx/Mp3PcmTimestampNormalizer.java \
  app/src/main/java/com/rikodev/music/android/dna/genome/*.java \
  app/src/main/java/com/rikodev/music/android/dna/analysis/*.java \
  app/src/test/java/com/rikodev/music/android/dna/analysis/DnaAnalyzerContracts.java
java -Xmx64m -cp "$test_classes:app/src/test/resources" com.rikodev.music.android.dna.analysis.DnaAnalyzerContracts

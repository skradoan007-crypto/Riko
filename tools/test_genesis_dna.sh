#!/usr/bin/env bash
# Actual production JVM sources and shared contract tests; no Android SDK or downloads.
set -euo pipefail
cd "$(dirname "$0")/.."
test_classes="$(mktemp -d)"
trap 'rm -rf "$test_classes"' EXIT
compiler=(java -m jdk.compiler/com.sun.tools.javac.Main)
if command -v javac >/dev/null 2>&1; then compiler=(javac); fi
# Source/target match app/build.gradle.kts. -options only suppresses the host-JDK
# system-module-path warning when running with JDK 21 instead of CI's JDK 17.
"${compiler[@]}" -source 17 -target 17 -Xlint:all,-options -Werror -d "$test_classes" \
  app/src/main/java/com/rikodev/music/android/dna/genome/*.java \
  app/src/test/java/com/rikodev/music/android/dna/genome/DnaCoreContracts.java
java -cp "$test_classes:app/src/test/resources" \
  com.rikodev.music.android.dna.genome.DnaCoreContracts
python3 tools/genesis_dna_reference.py --check

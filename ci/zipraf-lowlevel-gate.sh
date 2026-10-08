#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"
fail() { printf '[ZIPRAF_LOWLEVEL_GATE][FAIL] %s\n' "$*" >&2; exit 1; }

app='app/src/main/java/io/rafaelia/audiostudio'
zip="$app/RfaStoredZip.java"
buffer="$app/RfaBoundedBytes.java"
ordered="$app/RfaOrderedEntries.java"
writer="$app/AssuranceZiprafWriter.java"
test -s "$zip" && test -s "$buffer" && test -s "$ordered" && test -s "$writer" || fail 'AUTHORIAL_SOURCE_MISSING'

# Runtime must no longer link java.util.zip/CRC32 nor ByteArrayOutputStream.
if grep -nE '^[[:space:]]*import[[:space:]]+java\.util\.zip\.|^[[:space:]]*import[[:space:]]+java\.io\.ByteArrayOutputStream;' "$app"/*.java; then
  fail 'HOSTED_ZIP_OR_UNBOUNDED_BYTE_STREAM_REINTRODUCED'
fi
grep -Fq 'new RfaStoredZip(raw)' "$writer" || fail 'AUTHORIAL_ZIP_NOT_WIRED'
grep -Fq 'new RfaOrderedEntries()' "$writer" || fail 'BOUNDED_ENTRY_TABLE_NOT_WIRED'
if grep -nE 'import[[:space:]]+java\.util\.(TreeMap|Map);' "$writer"; then
  fail 'JAVA_COLLECTIONS_REINTRODUCED_IN_ZIPRAF_PATH'
fi
grep -Fq 'new RfaBoundedBytes(RfaStoredZip.MAX_ENTRY_BYTES)' "$writer" || fail 'BOUND_NOT_WIRED'
grep -Fq 'out.append(buffer, 0, n)' "$writer" || fail 'BOUNDED_ACCUMULATOR_NOT_WIRED'
grep -Fq 'zip.finish()' "$writer" || fail 'ZIP_CENTRAL_DIRECTORY_NOT_FINISHED'
grep -Fq 'output.write(tail)' "$zip" || fail 'ZIP_END_OF_CENTRAL_DIRECTORY_MISSING'
grep -Fq 'CRC32_POLY = 0xedb88320' "$zip" || fail 'CRC32_POLYNOMIAL_MISSING'
grep -Fq 'MAX_ZIP_BYTES = 64L * 1024L * 1024L' "$zip" || fail 'OUTPUT_SIZE_BOUNDS_MISSING'

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

javac -encoding UTF-8 -d "$tmp" "$zip" "$buffer" "$ordered" native/tests/RfaStoredZipSmoke.java
java -cp "$tmp" io.rafaelia.audiostudio.RfaStoredZipSmoke

printf 'ZIPRAF_SHIPPING_JDK_ZIP_IMPORTS=0\n'
printf 'ZIPRAF_AUTHORIAL_ORDERED_ENTRY_TABLE=PASS\n'
printf 'ZIPRAF_SHIPPING_BYTEARRAYOUTPUTSTREAM_IMPORTS=0\n'
printf 'ZIPRAF_JNI_NATIVE_DEPENDENCY=0_FOR_ZIP_SERIALIZER\n'
printf 'ZIPRAF_LOWLEVEL_GATE=PASS_EXECUTED_SCOPE\n'
printf 'ZIPRAF_PHYSICAL_ANDROID_RUN=NOT_RUN\n'

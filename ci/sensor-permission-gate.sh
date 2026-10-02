#!/usr/bin/env bash
set -euo pipefail

manifest='app/src/main/AndroidManifest.xml'
main='app/src/main/java/io/rafaelia/audiostudio/MainActivity.java'
snapshot='app/src/main/java/io/rafaelia/audiostudio/SystemAccessSnapshot.java'
vibration='app/src/main/java/io/rafaelia/audiostudio/MicroDeltaVibrationProbe.java'
magnetic='app/src/main/java/io/rafaelia/audiostudio/MicroDeltaMagnetometerProbe.java'
origin='ci/rafaelia-origin-assets.sh'

fail() {
  printf '[SENSOR_PERMISSION_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

for f in "$manifest" "$main" "$snapshot" "$vibration" "$magnetic" "$origin"; do
  test -s "$f" || fail "MISSING=$f"
done

mapfile -t declared < <(
  sed -n 's/.*<uses-permission android:name="\([^"]*\)".*/\1/p' "$manifest" | LC_ALL=C sort
)

expected=(
  android.permission.ACCESS_NETWORK_STATE
  android.permission.RECORD_AUDIO
)

if test "${#declared[@]}" -ne "${#expected[@]}"; then
  printf 'DECLARED_PERMISSION=%s\n' "${declared[@]:-TOKEN_VAZIO}"
  fail "PERMISSION_COUNT=${#declared[@]} expected=${#expected[@]}"
fi

for i in "${!expected[@]}"; do
  test "${declared[$i]}" = "${expected[$i]}" || \
    fail "PERMISSION[$i]=${declared[$i]} expected=${expected[$i]}"
done

for forbidden in \
  android.permission.ACTIVITY_RECOGNITION \
  android.permission.HIGH_SAMPLING_RATE_SENSORS \
  android.permission.ACCESS_FINE_LOCATION \
  android.permission.ACCESS_COARSE_LOCATION \
  android.permission.CAMERA \
  android.permission.BODY_SENSORS \
  android.permission.BLUETOOTH_SCAN \
  android.permission.BLUETOOTH_CONNECT; do
  if grep -Fq "$forbidden" "$manifest"; then
    fail "FORBIDDEN_MANIFEST_PERMISSION=$forbidden"
  fi
done

if grep -Fq 'Manifest.permission.ACTIVITY_RECOGNITION' "$main" "$snapshot"; then
  fail 'DEAD_ACTIVITY_RECOGNITION_RUNTIME_PATH_PRESENT'
fi
if grep -Fq 'REQ_ACTIVITY' "$main"; then
  fail 'DEAD_ACTIVITY_REQUEST_CODE_PRESENT'
fi

if grep -RFnE 'Sensor\.TYPE_STEP_(COUNTER|DETECTOR)|TYPE_SIGNIFICANT_MOTION' app/src/main/java >/dev/null; then
  fail 'STEP_ACTIVITY_SENSOR_API_PRESENT_WITHOUT_REVIEW'
fi

if grep -RFn 'SENSOR_DELAY_FASTEST' app/src/main/java >/dev/null; then
  fail 'UNBOUNDED_FASTEST_SENSOR_REQUEST_PRESENT'
fi

grep -Fq 'SensorManager.SENSOR_DELAY_GAME' "$vibration" || \
  fail 'VIBRATION_RATE_PROFILE_NOT_BOUNDED_GAME'
grep -Fq 'SensorManager.SENSOR_DELAY_GAME' "$magnetic" || \
  fail 'MAGNETOMETER_RATE_PROFILE_NOT_BOUNDED_GAME'

grep -Fq 'SENSOR_MUDELTA=EXPLICIT_PROOF_ACTION' "$snapshot" || \
  fail 'SYSTEM_SENSOR_EVIDENCE_POLICY_MISSING'
grep -Fq 'ação explícita: coletando μ∆ local' "$main" || \
  fail 'EVIDENCE_SENSOR_DISCLOSURE_MISSING'
grep -Fq 'ACTIVITY_RECOGNITION=NOT_DECLARED_NO_STEP_ACTIVITY_FEATURE' "$origin" || \
  fail 'EMBEDDED_PERMISSION_CONTRACT_STALE'

printf 'PERMISSION_SURFACE=RECORD_AUDIO,ACCESS_NETWORK_STATE\n'
printf 'ACTIVITY_RECOGNITION=ABSENT\n'
printf 'HIGH_SAMPLING_RATE_SENSORS=ABSENT\n'
printf 'STEP_ACTIVITY_API=ABSENT\n'
printf 'SENSOR_RATE_PROFILE=SENSOR_DELAY_GAME_BOUNDED_BY_PLATFORM\n'
printf 'SENSOR_EVIDENCE_DISCLOSURE=EXPLICIT_PROOF_ACTION\n'
printf 'SENSOR_PERMISSION_GATE=PASS_EXECUTED_SCOPE\n'
printf 'PHYSICAL_DEVICE_PERMISSION_FLOW=NOT_RUN\n'
printf 'EXTERNAL_STANDARD_AUDIT=NOT_AUDITED\n'
printf 'CLAIM_ALLOWED=false\n'

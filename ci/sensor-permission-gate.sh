#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

manifest='app/src/main/AndroidManifest.xml'
main='app/src/main/java/io/rafaelia/audiostudio/MainActivity.java'
snapshot='app/src/main/java/io/rafaelia/audiostudio/SystemAccessSnapshot.java'
evidence='app/src/main/java/io/rafaelia/audiostudio/EvidenceBundleWriter.java'
vibration='app/src/main/java/io/rafaelia/audiostudio/MicroDeltaVibrationProbe.java'
magnetic='app/src/main/java/io/rafaelia/audiostudio/MicroDeltaMagnetometerProbe.java'
origin='ci/rafaelia-origin-assets.sh'

fail() {
  printf '[SENSOR_PERMISSION_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

for f in "$manifest" "$main" "$snapshot" "$evidence" "$vibration" "$magnetic" "$origin"; do
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

optional_sensor_features=(
  android.hardware.sensor.accelerometer
  android.hardware.sensor.compass
  android.hardware.sensor.gyroscope
  android.hardware.sensor.light
  android.hardware.sensor.proximity
  android.hardware.sensor.barometer
)

for feature in "${optional_sensor_features[@]}"; do
  grep -Fq "<uses-feature android:name=\"$feature\" android:required=\"false\" />" "$manifest" || \
    fail "OPTIONAL_SENSOR_FEATURE_MISSING=$feature"
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

if grep -RFn 'Manifest.permission.ACTIVITY_RECOGNITION' app/src/main/java >/dev/null; then
  fail 'DEAD_ACTIVITY_RECOGNITION_RUNTIME_PATH_PRESENT'
fi
if grep -Fq 'REQ_ACTIVITY' "$main"; then
  fail 'DEAD_ACTIVITY_REQUEST_CODE_PRESENT'
fi

if grep -RnE 'Sensor\.TYPE_STEP_(COUNTER|DETECTOR)|TYPE_SIGNIFICANT_MOTION' app/src/main/java >/dev/null; then
  fail 'STEP_ACTIVITY_SENSOR_API_PRESENT_WITHOUT_REVIEW'
fi

if grep -RFn 'SENSOR_DELAY_FASTEST' app/src/main/java >/dev/null; then
  fail 'UNBOUNDED_FASTEST_SENSOR_REQUEST_PRESENT'
fi

grep -Fq 'SensorManager.SENSOR_DELAY_GAME' "$vibration" || \
  fail 'VIBRATION_RATE_PROFILE_NOT_BOUNDED_GAME'
grep -Fq 'SensorManager.SENSOR_DELAY_GAME' "$magnetic" || \
  fail 'MAGNETOMETER_RATE_PROFILE_NOT_BOUNDED_GAME'

grep -Fq 'SENSOR_ACCESS=TYPE_ALL_INVENTORY+UNPROTECTED_PHYSICAL_DIRECT' "$snapshot" || \
  fail 'SYSTEM_SENSOR_ACCESS_CONTRACT_MISSING'
grep -Fq 'PROTECTED_SENSOR_ACCESS=PERMISSION_SPECIFIC_NOT_AUTO_GRANTED' "$snapshot" || \
  fail 'SYSTEM_PROTECTED_SENSOR_POLICY_MISSING'
grep -Fq 'Sensor.TYPE_GYROSCOPE' "$snapshot" || \
  fail 'SYSTEM_GYROSCOPE_DISCOVERY_MISSING'
grep -Fq 'Sensor.TYPE_PRESSURE' "$snapshot" || \
  fail 'SYSTEM_BAROMETER_DISCOVERY_MISSING'
grep -Fq 'Sensor.TYPE_AMBIENT_TEMPERATURE' "$snapshot" || \
  fail 'SYSTEM_TEMPERATURE_DISCOVERY_MISSING'
grep -Fq 'Sensor.TYPE_RELATIVE_HUMIDITY' "$snapshot" || \
  fail 'SYSTEM_HUMIDITY_DISCOVERY_MISSING'
grep -Fq 'getSensorList(Sensor.TYPE_ALL)' "$snapshot" || \
  fail 'SYSTEM_TYPE_ALL_INVENTORY_MISSING'
grep -Fq 'SENSOR_RATE=PLATFORM_BOUNDED_NO_HIGH_RATE_PERMISSION' "$snapshot" || \
  fail 'SYSTEM_SENSOR_RATE_CONTRACT_MISSING'
grep -Fq 'SENSOR_MUDELTA=EXPLICIT_PROOF_ACTION' "$snapshot" || \
  fail 'SYSTEM_SENSOR_EVIDENCE_POLICY_MISSING'

grep -Fq 'Result.unavailable("SERVICE_UNAVAILABLE")' "$vibration" || \
  fail 'VIBRATION_SERVICE_UNAVAILABLE_STATE_MISSING'
grep -Fq 'Result.unavailable("NOT_PRESENT")' "$vibration" || \
  fail 'VIBRATION_NOT_PRESENT_STATE_MISSING'
grep -Fq 'Result.unavailable("LISTENER_REGISTRATION_FAILED")' "$vibration" || \
  fail 'VIBRATION_REGISTRATION_FAILURE_STATE_MISSING'
grep -Fq 'Result.unavailable("SERVICE_UNAVAILABLE")' "$magnetic" || \
  fail 'MAGNETIC_SERVICE_UNAVAILABLE_STATE_MISSING'
grep -Fq 'Result.unavailable("NOT_PRESENT")' "$magnetic" || \
  fail 'MAGNETIC_NOT_PRESENT_STATE_MISSING'
grep -Fq 'Result.unavailable("LISTENER_REGISTRATION_FAILED")' "$magnetic" || \
  fail 'MAGNETIC_REGISTRATION_FAILURE_STATE_MISSING'

grep -Fq '"activity_recognition_permission",' "$evidence" || \
  fail 'EVIDENCE_ACTIVITY_PERMISSION_FIELD_MISSING'
grep -Fq '"NOT_DECLARED_NO_STEP_ACTIVITY_FEATURE"' "$evidence" || \
  fail 'EVIDENCE_ACTIVITY_PERMISSION_STATE_STALE'
grep -Fq '"sensor_mudelta_policy",' "$evidence" || \
  fail 'EVIDENCE_SENSOR_POLICY_FIELD_MISSING'
grep -Fq 'getSensorList(Sensor.TYPE_ALL)' "$evidence" || \
  fail 'EVIDENCE_TYPE_ALL_SENSOR_INVENTORY_MISSING'
grep -Fq 'ação explícita: coletando μ∆ local' "$main" || \
  fail 'EVIDENCE_SENSOR_DISCLOSURE_MISSING'

grep -Fq 'PHYSICAL_SENSOR_INVENTORY=ANDROID_SENSOR_MANAGER_TYPE_ALL_PLATFORM_REPORTED' "$origin" || \
  fail 'EMBEDDED_ALL_SENSOR_INVENTORY_CONTRACT_STALE'
grep -Fq 'ACCELEROMETER=NO_RUNTIME_PERMISSION_REQUIRED_ANDROID_SENSOR_MANAGER' "$origin" || \
  fail 'EMBEDDED_ACCEL_PERMISSION_CONTRACT_STALE'
grep -Fq 'MAGNETOMETER=NO_RUNTIME_PERMISSION_REQUIRED_ANDROID_SENSOR_MANAGER' "$origin" || \
  fail 'EMBEDDED_MAG_PERMISSION_CONTRACT_STALE'
grep -Fq 'GYROSCOPE=NO_RUNTIME_PERMISSION_REQUIRED_ANDROID_SENSOR_MANAGER' "$origin" || \
  fail 'EMBEDDED_GYRO_PERMISSION_CONTRACT_STALE'
grep -Fq 'BAROMETER=NO_RUNTIME_PERMISSION_REQUIRED_ANDROID_SENSOR_MANAGER' "$origin" || \
  fail 'EMBEDDED_BAROMETER_PERMISSION_CONTRACT_STALE'
grep -Fq 'AMBIENT_TEMPERATURE=NO_RUNTIME_PERMISSION_REQUIRED_ANDROID_SENSOR_MANAGER' "$origin" || \
  fail 'EMBEDDED_TEMP_PERMISSION_CONTRACT_STALE'
grep -Fq 'RELATIVE_HUMIDITY=NO_RUNTIME_PERMISSION_REQUIRED_ANDROID_SENSOR_MANAGER' "$origin" || \
  fail 'EMBEDDED_HUMIDITY_PERMISSION_CONTRACT_STALE'
grep -Fq 'STEP_COUNTER_DETECTOR=PERMISSION_SPECIFIC_ACTIVITY_RECOGNITION_NOT_AUTO_REQUESTED' "$origin" || \
  fail 'EMBEDDED_STEP_PERMISSION_POLICY_STALE'
grep -Fq 'BODY_HEALTH_SENSORS=PERMISSION_SPECIFIC_NOT_AUTO_REQUESTED' "$origin" || \
  fail 'EMBEDDED_BODY_SENSOR_POLICY_STALE'
grep -Fq 'HIGH_SAMPLING_RATE_SENSORS=NOT_DECLARED_CURRENT_PROFILE_LE_200HZ' "$origin" || \
  fail 'EMBEDDED_HIGH_RATE_PERMISSION_CONTRACT_STALE'

printf 'REPO_ROOT=%s\n' "$repo_root"
printf 'PERMISSION_SURFACE=RECORD_AUDIO,ACCESS_NETWORK_STATE\n'
printf 'OPTIONAL_SENSOR_FEATURES=%s\n' "${optional_sensor_features[*]}"
printf 'PHYSICAL_SENSOR_INVENTORY=TYPE_ALL_PLATFORM_REPORTED\n'
printf 'UNPROTECTED_PHYSICAL_SENSOR_RUNTIME_PERMISSION=NOT_REQUIRED\n'
printf 'PROTECTED_SENSOR_POLICY=PERMISSION_SPECIFIC_NOT_AUTO_GRANTED\n'
printf 'ACTIVITY_RECOGNITION=ABSENT\n'
printf 'BODY_SENSORS=ABSENT\n'
printf 'HIGH_SAMPLING_RATE_SENSORS=ABSENT_CURRENT_PROFILE_LE_200HZ\n'
printf 'STEP_ACTIVITY_API=ABSENT\n'
printf 'SENSOR_RATE_PROFILE=SENSOR_DELAY_GAME_BOUNDED_BY_PLATFORM\n'
printf 'SENSOR_UNAVAILABLE_REASONING=SERVICE|NOT_PRESENT|LISTENER_REGISTRATION_FAILED\n'
printf 'SENSOR_EVIDENCE_DISCLOSURE=EXPLICIT_PROOF_ACTION\n'
printf 'SENSOR_PERMISSION_GATE=PASS_EXECUTED_SCOPE\n'
printf 'PHYSICAL_DEVICE_PERMISSION_FLOW=NOT_RUN\n'
printf 'EXTERNAL_STANDARD_AUDIT=NOT_AUDITED\n'
printf 'CLAIM_ALLOWED=false\n'

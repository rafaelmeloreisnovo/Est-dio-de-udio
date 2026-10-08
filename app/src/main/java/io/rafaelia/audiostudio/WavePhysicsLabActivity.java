/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.net.Uri;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Read-only physics lab. Never changes Wi-Fi/Bluetooth/cellular/GNSS settings,
 * frequencies, Tx power, channels, modem settings, or scan policy.
 * Opening the lab registers accelerometer/rotation-vector listeners only.
 */
public final class WavePhysicsLabActivity extends Activity implements SensorEventListener {
    private static final int REQUEST_LOCATION = 640;
    private static final int REQUEST_MIC = 641;
    private SensorManager sensorManager;
    private TextView screen;
    private TextView pose;
    private volatile float ax, ay, az;
    private volatile float yawDegrees = Float.NaN;
    private int accelEvents;
    private int crossingEvents;
    private float oldAx = Float.NaN;
    private double deltaSumSq;
    private String gps = "TOKEN_VAZIO_NOT_OBSERVED";
    private String wifi = "TOKEN_VAZIO_NOT_OBSERVED";
    private String cell = "TOKEN_VAZIO_NOT_OBSERVED";
    private String bluetooth = "TOKEN_VAZIO_NOT_OBSERVED";
    private String audio = "NOT_RUN";
    private String bands = "TOKEN_VAZIO_NO_PCM_WINDOW";
    private String recordTime = "TOKEN_VAZIO";
    private volatile boolean recording;
    private long lastAccelNanos;
    private long gpsFixElapsedMs = -1;
    private String acoustic = "TOKEN_VAZIO_NOT_CALIBRATED_HERE";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 24, 22, 24);
        root.setBackgroundColor(Color.rgb(10, 13, 17));
        TextView title = label("RAFAELIA | LABORATORIO DE ONDAS / P0", 19);
        root.addView(title);
        root.addView(label("READ-ONLY RADIO: no enable/disable, tuning, channel, power, transmitter or modem changes.", 12));
        root.addView(label("Spectro = mono PCM frequency bands. Pose = device orientation. DOA/echolocation distance = TOKEN_VAZIO without a calibrated multi-position experiment.", 12));
        pose = label("POSE=TOKEN_VAZIO", 13);
        screen = label("", 13);
        root.addView(pose);
        Button read = button("Observar GPS, Wi-Fi, celular, Bluetooth (somente leitura)");
        read.setOnClickListener(v -> readSnapshot());
        root.addView(read);
        Button mic = button("Amostrar espectro local (microfone, 4096 amostras)");
        mic.setOnClickListener(v -> sampleMic());
        root.addView(mic);
        Button export = button("Salvar observacoes em ZIPRAF");
        export.setOnClickListener(v -> exportZipraf());
        root.addView(export);
        root.addView(screen);
        scroll.addView(root);
        setContentView(scroll);
        if (getIntent().getBooleanExtra("calibrated", false)) {
            int lag = getIntent().getIntExtra("lag_samples", -1);
            long rt60 = getIntent().getLongExtra("rt60_relative_ms", -1);
            if (lag >= 0) {
                acoustic = "CAL_RELATIVE_OBSERVED lag_samples=" + lag +
                        " latency_us=" + (lag * 1000000L / 48000L) +
                        " RT60_REL_MS=" + (rt60 >= 0 ? rt60 : "TOKEN_VAZIO");
            }
        }
        show();
    }

    private TextView label(String text, int size) {
        TextView t = new TextView(this);
        t.setTextColor(Color.rgb(221, 234, 240));
        t.setTextSize(size);
        t.setText(text);
        t.setPadding(3, 8, 3, 9);
        return t;
    }
    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    @Override protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            Sensor a = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor r = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (a != null) sensorManager.registerListener(this, a, SensorManager.SENSOR_DELAY_NORMAL);
            if (r != null) sensorManager.registerListener(this, r, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    @Override protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax = e.values[0]; ay = e.values[1]; az = e.values[2];
            if (!Float.isNaN(oldAx)) {
                double d = ax - oldAx;
                deltaSumSq += d * d;
                crossingEvents += WaveLabCore.sectionCrossing(oldAx, ax);
            }
            oldAx = ax;
            lastAccelNanos = e.timestamp;
            ++accelEvents;
        } else if (e.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotation = new float[9];
            float[] angles = new float[3];
            SensorManager.getRotationMatrixFromVector(rotation, e.values);
            SensorManager.getOrientation(rotation, angles);
            yawDegrees = (float) Math.toDegrees(angles[0]);
        }
        if (accelEvents % 8 == 0 && pose != null) {
            pose.setText("POSE_SECTOR=" +
                    (Float.isNaN(yawDegrees) ? "TOKEN_VAZIO_ROTATION_VECTOR" :
                            WaveLabCore.poseSector(yawDegrees) + "/8") +
                    " [NOT_ACOUSTIC_DIRECTION]  aXYZ=(" +
                    fmt(ax) + "," + fmt(ay) + "," + fmt(az) + ") m/s2");
        }
    }
    private static String fmt(double d) {
        return Double.isFinite(d) ? String.format(Locale.US, "%.4f", d) : "TOKEN_VAZIO";
    }
    private boolean has(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    private void readSnapshot() {
        if (!has(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        gps = "TOKEN_VAZIO_GPS_NO_FIX";
        gpsFixElapsedMs = -1;
        try {
            LocationManager loc = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location last = loc == null ? null :
                    loc.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) {
                long age = SystemClock.elapsedRealtime() -
                        last.getElapsedRealtimeNanos() / 1000000L;
                gpsFixElapsedMs = age;
                gps = age < 0 || age > 120000
                        ? "TOKEN_VAZIO_GPS_STALE age_ms=" + age :
                        "GPS_LAT=" + fmt(last.getLatitude()) +
                        " GPS_LON=" + fmt(last.getLongitude()) +
                        " accuracy_m=" + (last.hasAccuracy() ? fmt(last.getAccuracy()) : "TOKEN_VAZIO") +
                        " age_ms=" + age;
            }
        } catch (SecurityException | IllegalArgumentException error) {
            gps = "TOKEN_VAZIO_GPS_PERMISSION_OR_PROVIDER";
        }
        wifi = "TOKEN_VAZIO_WIFI_SCAN_CACHE";
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            List<ScanResult> found = wm == null ? null : wm.getScanResults();
            if (found != null) {
                int max = -200;
                int hz = -1;
                for (ScanResult item : found) {
                    if (item.level > max) { max = item.level; hz = item.frequency; }
                }
                wifi = "CACHED_SCAN_COUNT=" + found.size() +
                        " strongest_rssi_dbm=" + (found.isEmpty() ? "TOKEN_VAZIO" : max) +
                        " strongest_channel_MHz=" + (found.isEmpty() ? "TOKEN_VAZIO" : hz) +
                        " cache_age=TOKEN_VAZIO_NOT_INDIVIDUALLY_VALIDATED";
            }
        } catch (SecurityException error) { wifi = "TOKEN_VAZIO_WIFI_PERMISSION"; }
        cell = "TOKEN_VAZIO_CELL_NOT_REPORTED";
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            List<CellInfo> values = tm == null ? null : tm.getAllCellInfo();
            if (values != null) {
                int dbm = -200;
                int reported = 0;
                for (CellInfo item : values) {
                    if (item.getCellSignalStrength() != null) {
                        dbm = Math.max(dbm, item.getCellSignalStrength().getDbm());
                        ++reported;
                    }
                }
                cell = "CELL_COUNT=" + values.size() + " LEVEL_REPORTS=" + reported +
                        " strongest_dbm=" + (reported > 0 ? dbm : "TOKEN_VAZIO") +
                        " freshness=TOKEN_VAZIO_PLATFORM_CACHED";
            }
        } catch (SecurityException | UnsupportedOperationException error) {
            cell = "TOKEN_VAZIO_CELL_PERMISSION_OR_API";
        }
        bluetooth = "TOKEN_VAZIO_BLUETOOTH_CONNECT_PERMISSION";
        if (Build.VERSION.SDK_INT < 31 ||
                has(Manifest.permission.BLUETOOTH_CONNECT)) {
            try {
                BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                BluetoothAdapter adapter = bm == null ? null : bm.getAdapter();
                bluetooth = adapter == null ? "TOKEN_VAZIO_NO_BLUETOOTH_ADAPTER"
                        : "ADAPTER_ENABLED=" + adapter.isEnabled() +
                        " BLE_SCAN=NOT_RUN NO_RADIO_MUTATION";
            } catch (SecurityException e) { bluetooth = "TOKEN_VAZIO_BLUETOOTH_PERMISSION"; }
        }
        show();
    }

    private void sampleMic() {
        if (recording) { audio = "BUSY"; show(); return; }
        if (!has(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        recording = true;
        audio = "CAPTURING_4096_SAMPLES";
        show();
        new Thread(() -> {
            AudioRecord ar = null;
            String result = "TOKEN_VAZIO_AUDIO_CAPTURE_FAILED";
            String spectrum = "TOKEN_VAZIO_AUDIO_CAPTURE_FAILED";
            try {
                int min = AudioRecord.getMinBufferSize(48000,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (min <= 0) throw new IllegalStateException("unsupported_capture_format");
                ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 48000,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(min, 16384));
                if (ar.getState() != AudioRecord.STATE_INITIALIZED)
                    throw new IllegalStateException("AudioRecord_not_initialized");
                short[] input = new short[4096];
                ar.startRecording();
                int count = 0, attempts = 0;
                while (count < input.length && attempts++ < 12) {
                    int got = ar.read(input, count, input.length - count);
                    if (got <= 0) break;
                    count += got;
                }
                if (count == input.length) {
                    recordTime = String.valueOf(System.currentTimeMillis());
                    result = "OBSERVED_PCM16_MONO_48K n=4096 window_ms=85.333" +
                            " pose_sector=" + (Float.isNaN(yawDegrees) ?
                            "TOKEN_VAZIO" : WaveLabCore.poseSector(yawDegrees));
                    StringBuilder b = new StringBuilder();
                    for (int hz : WaveLabCore.BANDS_HZ) {
                        b.append(hz).append("Hz=").append(
                                fmt(WaveLabCore.relativeDbfs(input, count, hz))).append(" dBFS; ");
                    }
                    spectrum = b.toString();
                } else {
                    result = "TOKEN_VAZIO_SHORT_CAPTURE n=" + count;
                }
            } catch (RuntimeException e) {
                result = "TOKEN_VAZIO_AUDIO_CAPTURE_ERROR_" + e.getClass().getSimpleName();
            } finally {
                if (ar != null) {
                    try { if (ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) ar.stop(); }
                    catch (IllegalStateException ignored) { }
                    ar.release();
                }
            }
            final String a = result, b = spectrum;
            runOnUiThread(() -> {
                audio = a; bands = b; recording = false; show();
            });
        }, "rafaelia-wave-lab-once").start();
    }

    @Override public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(req, permissions, results);
        if (results.length == 0 || results[0] != PackageManager.PERMISSION_GRANTED) {
            if (req == REQUEST_LOCATION) gps = "TOKEN_VAZIO_PERMISSION_DENIED";
            if (req == REQUEST_MIC) audio = "TOKEN_VAZIO_PERMISSION_DENIED";
            show(); return;
        }
        if (req == REQUEST_LOCATION) readSnapshot();
        if (req == REQUEST_MIC) sampleMic();
    }

    private String receipt() {
        long now = System.currentTimeMillis();
        double rmsDelta = accelEvents > 1 ? Math.sqrt(deltaSumSq / (accelEvents - 1)) : Double.NaN;
        return "schema=rafaelia.wave-lab/alpha1\n"
                + "source_sha=" + BuildConfig.SOURCE_SHA + "\n"
                + "capture_epoch_ms=" + now + "\n"
                + "capture_monotonic_ns=" + SystemClock.elapsedRealtimeNanos() + "\n"
                + "policy=PASSIVE_READ_ONLY_NO_RADIO_MUTATIONS\n"
                + "gps=" + gps + "\n"
                + "gps_fix_age_ms=" + (gpsFixElapsedMs < 0 ? "TOKEN_VAZIO" : gpsFixElapsedMs) + "\n"
                + "wifi=" + wifi + "\ncell=" + cell + "\nbluetooth=" + bluetooth + "\n"
                + "accelerometer_events=" + accelEvents + "\n"
                + "accelerometer_last_timestamp_ns=" + lastAccelNanos + "\n"
                + "accel_xyz_m_s2=" + fmt(ax) + "," + fmt(ay) + "," + fmt(az) + "\n"
                + "accel_delta_x_rms_m_s2=" + fmt(rmsDelta) + "\n"
                + "poincare_x_zero_up_crossings=" + crossingEvents +
                   " [COUNT_ONLY_NOT_CHAOS_PROOF]\n"
                + "pose_sector_eight=" + (Float.isNaN(yawDegrees) ?
                    "TOKEN_VAZIO_ROTATION_VECTOR" : WaveLabCore.poseSector(yawDegrees)) + "\n"
                + "audio=" + audio + "\n"
                + "spectral_bins_relative_dbfs=" + bands + "\n"
                + "audio_observation_epoch_ms=" + recordTime + "\n"
                + "acoustic=" + acoustic + "\n"
                + "echo_direction=TOKEN_VAZIO_MONO_UNDERDETERMINED\n"
                + "range_m=TOKEN_VAZIO_UNCALIBRATED_IO_LATENCY\n"
                + "gnss_cn0=TOKEN_VAZIO_NOT_MEASURED\n"
                + "ambisonic_foa=TOKEN_VAZIO_NOT_CAPTURED_4_CHANNELS\n"
                + "claim_allowed=false\n";
    }
    private void show() {
        if (screen != null) screen.setText(receipt().replace("=", " = "));
    }
    private void exportZipraf() {
        if (recording) { audio = "EXPORT_BLOCKED_BUSY"; show(); return; }
        Uri uri = null;
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME,
                    "Rafaelia_WaveLab_" + System.currentTimeMillis() + ".zip");
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/RafaeliaAudio/Physics");
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IllegalStateException("MediaStore_insert_null");
            try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                if (stream == null) throw new IllegalStateException("MediaStore_stream_null");
                RfaStoredZip zip = new RfaStoredZip(stream);
                zip.add("00_manifest.txt", ("schema=rafaelia.wave-lab/alpha1\n" +
                        "entry=10_observations.txt\n" +
                        "integrity=ZIP_STORED_CRC32_NOT_CRYPTOGRAPHIC_AUTHENTICITY\n" +
                        "sample_stream=NOT_EMBEDDED_PRIVACY_BY_DEFAULT\n" +
                        "no_radio_mutation=true\nclaim_allowed=false\n")
                        .getBytes(StandardCharsets.UTF_8));
                zip.add("10_observations.txt", receipt().getBytes(StandardCharsets.UTF_8));
                zip.finish();
            }
            v.clear(); v.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update(uri, v, null, null);
            audio = "ZIPRAF_SAVED_IN_DOWNLOADS";
        } catch (Exception error) {
            if (uri != null) getContentResolver().delete(uri, null, null);
            audio = "TOKEN_VAZIO_ZIPRAF_EXPORT_" + error.getClass().getSimpleName();
        }
        show();
    }
}

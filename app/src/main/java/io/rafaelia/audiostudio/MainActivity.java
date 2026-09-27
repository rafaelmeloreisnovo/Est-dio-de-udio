package io.rafaelia.audiostudio;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQ_AUDIO = 100;
    private static final int REQ_PICK = 101;

    private TextView status;
    private AudioRecorderEngine recorder;
    private File recordedPcm;
    private Uri lastOutput;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Rafaelia Audio Studio — Delta 1");
        title.setTextSize(24f);
        box.addView(title);

        status = new TextView(this);
        status.setText(
                "Pipeline: 48 kHz PCM16 → DSP freestanding → Ogg/Opus\n" +
                "Preset ativo: VOICE_WHATSAPP");
        status.setTextSize(16f);
        box.addView(status);

        Button pick = button("Importar áudio WhatsApp / arquivo");
        pick.setOnClickListener(v -> pickAudio());
        box.addView(pick);

        Button start = button("Gravar voz 48 kHz");
        start.setOnClickListener(v -> ensurePermissionAndRecord());
        box.addView(start);

        Button stop = button("Parar e remasterizar");
        stop.setOnClickListener(v -> stopAndMaster());
        box.addView(stop);

        Button share = button("Compartilhar último Ogg/Opus");
        share.setOnClickListener(v -> shareLast());
        box.addView(share);

        TextView note = new TextView(this);
        note.setText(
                "\nPrincípio operacional: não chamar RMS de LUFS. " +
                "BS.1770/R128 permanece PENDING até medição formal.");
        box.addView(note);

        scroll.addView(box);
        return scroll;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private void setStatus(String value) {
        runOnUiThread(() -> status.setText(value));
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQ_PICK);
    }

    private void ensurePermissionAndRecord() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        startRecording();
    }

    private void startRecording() {
        if (recorder != null) {
            setStatus("Já existe gravação ativa.");
            return;
        }
        try {
            recordedPcm = new File(getCacheDir(), "rafaelia_recording_48k.pcm");
            recorder = new AudioRecorderEngine(this, recordedPcm);
            recorder.start();
            setStatus(
                    "GRAVANDO 48 kHz mono PCM16.\n" +
                    "UNPROCESSED é usado quando o aparelho declarar suporte; " +
                    "senão VOICE_RECOGNITION.");
        } catch (Exception e) {
            recorder = null;
            setStatus("Falha ao iniciar gravação: " + e.getMessage());
        }
    }

    private void stopAndMaster() {
        if (recorder == null) {
            setStatus("Nenhuma gravação ativa.");
            return;
        }
        int sampleRate = recorder.getSampleRate();
        int channels = recorder.getChannels();
        recorder.stop();
        recorder = null;
        runPipelineFromPcm(recordedPcm, sampleRate, channels, "microfone");
    }

    private void runPipelineFromPcm(File input, int sampleRate,
                                    int channels, String origin) {
        setStatus("PROCESSANDO " + origin + "…");

        new Thread(() -> {
            File mastered = new File(getCacheDir(), "rafaelia_mastered.pcm");
            Uri outputUri = null;

            try {
                AudioPipeline.masterPcm(
                        input,
                        mastered,
                        channels,
                        NativeDsp.PRESET_WHATSAPP_VOICE);

                outputUri = createPendingOutput();
                if (outputUri == null) {
                    throw new IllegalStateException("MediaStore insert retornou null");
                }

                try (ParcelFileDescriptor pfd =
                             getContentResolver().openFileDescriptor(outputUri, "w")) {
                    if (pfd == null) {
                        throw new IllegalStateException("FD de saída indisponível");
                    }
                    AudioPipeline.encodeOpusOgg(
                            mastered,
                            pfd.getFileDescriptor(),
                            sampleRate,
                            channels);
                }

                publishOutput(outputUri);
                lastOutput = outputUri;
                setStatus(
                        "IMPLEMENTED_UNTESTED concluído.\n" +
                        "Saída Ogg/Opus criada em Music/RafaeliaAudio.\n" +
                        "Próximo gate: ouvir, medir clipping/duração e CI.");
            } catch (Exception e) {
                if (outputUri != null) {
                    getContentResolver().delete(outputUri, null, null);
                }
                setStatus("Falha no pipeline: " + e.getMessage());
            }
        }, "rafaelia-pipeline").start();
    }

    private Uri createPendingOutput() {
        String stamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME,
                "Rafaelia_Master_" + stamp + ".opus");
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/ogg");
        values.put(MediaStore.Audio.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MUSIC + "/RafaeliaAudio");
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);

        return getContentResolver().insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void publishOutput(Uri uri) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.IS_PENDING, 0);
        getContentResolver().update(uri, values, null, null);
    }

    private void shareLast() {
        if (lastOutput == null) {
            setStatus("Ainda não há saída processada.");
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("audio/ogg");
        send.putExtra(Intent.EXTRA_STREAM, lastOutput);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Compartilhar áudio"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            setStatus("DECODIFICANDO arquivo selecionado…");

            new Thread(() -> {
                try {
                    File pcm = new File(getCacheDir(), "rafaelia_imported.pcm");
                    AudioPipeline.DecodedAudio decoded =
                            AudioPipeline.decodeToPcm(this, uri, pcm);
                    runPipelineFromPcm(
                            decoded.pcmFile,
                            decoded.sampleRate,
                            decoded.channels,
                            "arquivo importado");
                } catch (Exception e) {
                    setStatus("Falha ao decodificar: " + e.getMessage());
                }
            }, "rafaelia-decode").start();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else if (requestCode == REQ_AUDIO) {
            setStatus("Permissão de microfone negada.");
        }
    }

    @Override
    protected void onDestroy() {
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
        super.onDestroy();
    }
}

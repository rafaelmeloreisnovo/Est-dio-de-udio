package io.rafaelia.audiostudio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;

final class StudioWizard {
    private static final String PREFS = "rafaelia_audio_setup";
    private static final String DONE = "wizard_done_v2";

    private StudioWizard() {}

    static boolean shouldShow(Activity activity) {
        return !activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(DONE, false);
    }

    static void show(Activity activity, Runnable onFinish) {
        stepIntro(activity, onFinish);
    }

    private static void stepIntro(Activity a, Runnable finish) {
        new AlertDialog.Builder(a)
                .setTitle("Wizard — Estúdio de Narração")
                .setMessage(
                        "O wizard verifica microfone, rota de captura e o pipeline " +
                        "48 kHz. O áudio bruto é preservado antes da remasterização.")
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Continuar", (d, w) -> stepCapabilities(a, finish))
                .show();
    }

    private static void stepCapabilities(Activity a, Runnable finish) {
        AudioManager am = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
        String sampleRate = am == null ? "UNAVAILABLE_SERVICE"
                : am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        String frames = am == null ? "UNAVAILABLE_SERVICE"
                : am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        boolean raw = am != null && "true".equals(am.getProperty(
                AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
        boolean mic = a.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        String report =
                "Android: " + Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT +
                "\nMicrofone autorizado: " + mic +
                "\nUNPROCESSED declarado: " + raw +
                "\nSample-rate de saída reportado: " + value(sampleRate) +
                "\nFrames/buffer reportado: " + value(frames) +
                "\n\nCaptura: 48 kHz PCM16 mono. Se UNPROCESSED não existir, " +
                "VOICE_RECOGNITION é o fallback documentado.";

        new AlertDialog.Builder(a)
                .setTitle("Pré-voo de áudio")
                .setMessage(report)
                .setNegativeButton("Voltar", (d, w) -> stepIntro(a, finish))
                .setPositiveButton("Próximo", (d, w) -> stepWorkflow(a, finish))
                .show();
    }

    private static void stepWorkflow(Activity a, Runnable finish) {
        new AlertDialog.Builder(a)
                .setTitle("Fluxo recomendado")
                .setMessage(
                        "1. Cole ou escreva o roteiro.\n" +
                        "2. Faça um teste curto de microfone.\n" +
                        "3. Grave acompanhando o teleprompter.\n" +
                        "4. Remasterize.\n" +
                        "5. Confira espectro e reprodução.\n" +
                        "6. Exporte Ogg/Opus.\n\n" +
                        "Targets: -23 LUFS = EBU R128; -18/-16 LUFS = " +
                        "targets de workflow, não normas.")
                .setNegativeButton("Voltar", (d, w) -> stepCapabilities(a, finish))
                .setPositiveButton("Concluir", (d, w) -> {
                    SharedPreferences prefs =
                            a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                    prefs.edit().putBoolean(DONE, true).apply();
                    finish.run();
                })
                .show();
    }

    private static String value(String value) {
        return value == null || value.length() == 0 ? "UNAVAILABLE_NOT_REPORTED" : value;
    }
}

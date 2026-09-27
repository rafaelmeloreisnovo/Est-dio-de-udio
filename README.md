# Rafaelia Audio Studio

APK Android 10+ para captura, tratamento e remasterização de voz com foco em Ogg/Opus e fluxos de mensageria.

## Estado

- SOURCE: este repositório
- EXECUTION_TARGET: Android API 29+
- DSP_CORE: PASS (host smoke + ARMv7 ELF32 compile + 0 undefined symbols)
- APK_BUILD: ROUTE_STATE_BLOCKED (GitHub Actions ainda sem runs)
- WHATSAPP_OGG_OPUS_IMPORT: IMPLEMENTED_UNTESTED
- OGG_OPUS_EXPORT: IMPLEMENTED_UNTESTED
- ITU_BS1770_5_LOUDNESS: PENDING
- EBU_R128_METERING: PENDING
- claim_allowed: false até CI + teste físico com áudio real

## Arquitetura

O projeto separa três camadas:

1. Android I/O: AudioRecord, MediaExtractor, MediaCodec e MediaMuxer.
2. JNI bridge: adaptação de buffers Java para o kernel.
3. DSP core: C freestanding, sem malloc, sem libm, sem I/O e sem dependências externas.

O APK inteiro não é bare metal: ele roda sobre Android/Linux e usa APIs do sistema. O núcleo dsp_core.c é deliberadamente isolado para poder ser compilado e auditado como componente freestanding.

## Fluxo Delta 1

Importar Ogg/Opus ou gravar microfone em PCM 48 kHz
-> remasterizar em blocos PCM16
-> exportar Ogg/Opus
-> compartilhar pelo seletor do Android.

Preset inicial: VOICE_WHATSAPP.

Chain:
- high-pass/DC cleanup em ponto fixo Q31;
- gate/expansão suave por envelope;
- leveler de fala;
- limiter final abaixo de 0 dBFS.

Nenhum estágio é chamado de LUFS, true-peak, de-esser, EQ paramétrico ou VST enquanto não houver implementação e teste correspondentes.

## Fontes técnicas

- RFC 6716: Definition of the Opus Audio Codec.
- RFC 7845: Ogg Encapsulation for the Opus Audio Codec.
- Android 10: Opus encoding support.
- Android MediaMuxer API 29: MUXER_OUTPUT_OGG.
- Android MediaRecorder/AudioSource: UNPROCESSED com fallback recomendado.
- ITU-R BS.1770-5 (2023): loudness e true peak.
- EBU R 128 v5 (2023): loudness normalisation.

## Build

CI usa JDK 17, Gradle 8.11.1, Android SDK 35, NDK 27.2 e CMake 3.22.1.

Comando local:
gradle :app:assembleDebug

APK esperado:
app/build/outputs/apk/debug/app-debug.apk

## Próximo gate

1. CI verde.
2. Instalação física API 29 armeabi-v7a.
3. Importar amostra Ogg/Opus real.
4. Conferir ausência de clipping, duração e inteligibilidade.
5. Só depois promover BUILD/WHATSAPP_PIPELINE para PASS.

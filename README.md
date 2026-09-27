# Rafaelia Audio Studio

Android 10+ audio workstation para captura, narração guiada, tratamento, medição, normalização e export Ogg/Opus.

## Estado operacional

- MAIN_DELTA1: MERGED
- DELTA2_BRANCH: feature/pro-audio-delta2-narration-metering
- EXECUTION_TARGET: Android API 29+
- DSP_CORE_DELTA1: PASS no gate previamente registrado
- DSP_METER_DELTA2: IMPLEMENTED_UNTESTED
- APK_BUILD_DELTA2: PENDING_CI
- PHYSICAL_ANDROID10: NOT_RUN
- WHATSAPP_REAL_ROUNDTRIP: NOT_RUN
- claim_allowed: false para conformidade/qualidade end-to-end até CI + vetores + teste físico

## Delta 2

### Gravação
- PCM16 mono 48 kHz;
- UNPROCESSED quando declarado pelo dispositivo;
- VOICE_RECOGNITION como fallback;
- tentativa best-effort de desativar AGC, NoiseSuppressor e AEC da sessão;
- raw PCM preservado antes da remasterização;
- telemetria de sample peak, RMS bruto e samples clipados.

### Wizard
- pré-voo de permissão;
- API Android;
- suporte UNPROCESSED;
- sample rate / frames-per-buffer reportados;
- explicação dos targets e fluxo.

### Narração
- editor de roteiro;
- teleprompter;
- velocidade aproximada em WPM;
- start/stop manual;
- gravação com countdown;
- acompanhamento do roteiro durante a captura.

### DSP rack interno
- high-pass / DC cleanup;
- gate / expansão suave;
- speech leveler;
- limiter;
- ganho de normalização fixed-point.

É um rack DSP interno. Não é declarado VST2/VST3 enquanto a ABI VST não existir.

### Medição
- K-weighting de duas etapas, coeficientes BS.1770-5 para 48 kHz quantizados Q29;
- blocos de 400 ms;
- overlap 75%;
- gate absoluto -70 LKFS;
- gate relativo -10 LU em potência;
- true-peak 4x com FIR 48-tap / 4-phase do Annex 2;
- espectrometria relativa de 16 centros via Goertzel fixed-point;
- ceiling de normalização: -1 dBTP.

A implementação ainda precisa de vetores de conformidade antes de receber PASS normativo.

### Targets
- Broadcast: -23 LUFS — EBU R128.
- Narração: -18 LUFS — target de workflow.
- WhatsApp/mobile: -16 LUFS — target de workflow.
- Música clean: -18 LUFS conservador — não é mastering musical certificado.

## Arquitetura

1. Android I/O/UI:
   AudioRecord, AudioTrack, MediaExtractor, MediaCodec, MediaMuxer, MediaStore.
2. JNI:
   ponte interna de buffers.
3. dsp_core.c:
   transformação fixed-point.
4. meter_core.c:
   K-weighting, gating, true-peak, spectrum e cálculo de ganho.

O APK inteiro não pode ser freestanding/bare-metal porque depende do runtime Android para microfone, tela, armazenamento e codec. Os núcleos DSP/meter são os artefatos freestanding auditáveis.

## Invariantes do núcleo

- sem malloc/calloc/realloc/free;
- sem libm;
- sem stdio;
- sem filesystem;
- sem rede;
- sem threads;
- sem bibliotecas DSP de terceiros;
- sem VST SDK;
- buffers fornecidos pelo chamador;
- fixed-point.

## Build gate

CI exige:
1. DSP host smoke;
2. meter host smoke;
3. source dependency gate;
4. ARMv7 zero undefined symbols;
5. AArch64 zero undefined symbols;
6. assembleDebug;
7. APK artifact.

## F_next

- CI do Delta 2;
- corrigir qualquer helper/compile error;
- vetores BS.1770/EBU;
- APK no Android 10 armeabi-v7a;
- gravação física de narração;
- round-trip Ogg/Opus real;
- inspeção auditiva e clipping.

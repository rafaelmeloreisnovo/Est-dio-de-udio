# Rafaelia Audio Studio

Android 10+ audio workstation para captura, narração guiada, tratamento, medição, normalização e export Ogg/Opus.

## Estado operacional

- MAIN_BASE: 058a3a5a8f79d1103a224e484a4797d51d151966
- MAIN_CI: PASS
- DELTA3_FREESTANDING_CONTEXTS: IMPLEMENTED_UNTESTED
- EXECUTION_TARGET: Android API 29+
- DSP_HOST_SMOKE_MAIN: PASS
- METER_HOST_SMOKE_MAIN: PASS
- ARMV7_ZERO_UNDEFINED_MAIN: PASS
- AARCH64_ZERO_UNDEFINED_MAIN: PASS
- APK_BUILD_MAIN: PASS
- PHYSICAL_ANDROID10: NOT_RUN
- WHATSAPP_REAL_ROUNDTRIP: NOT_RUN
- BS1770_CONFORMANCE: PENDING
- claim_allowed: false para conformidade/qualidade end-to-end até vetores + teste físico

## Delta 3 — core autoral freestanding

O núcleo DSP/meter está sendo endurecido para uma fronteira explicitamente independente de plataforma:

- tipos escalares próprios em `rfa_core_types.h`;
- nenhum header de sistema no core;
- compilação com `-nostdinc -ffreestanding -fno-builtin`;
- sem heap/libc/libm/filesystem/network/threads;
- estado DSP e meter fornecido pelo chamador;
- nenhuma variável global mutável no core;
- JNI possui o estado apenas na camada Android;
- gate de zero símbolos indefinidos em ARMv7/AArch64;
- allowlist da superfície ABI externa.

Isso permite portar o mesmo core para Android, Termux, Linux, firmware ou bare-metal sem reescrever os algoritmos.

## Gravação

- PCM16 mono 48 kHz;
- UNPROCESSED quando declarado pelo dispositivo;
- VOICE_RECOGNITION como fallback;
- tentativa best-effort de desativar AGC, NoiseSuppressor e AEC;
- raw PCM preservado antes da remasterização;
- telemetria de sample peak, RMS bruto e clipping.

## Narração

- editor de roteiro;
- teleprompter;
- velocidade em WPM;
- countdown;
- acompanhamento do roteiro durante a captura.

## DSP rack

- high-pass / DC cleanup;
- gate / expansão suave;
- speech leveler;
- limiter;
- normalização fixed-point.

## Medição

- K-weighting 48 kHz Q29;
- blocos 400 ms / overlap 75%;
- gate absoluto e relativo;
- true-peak 4x;
- espectro de 16 bandas via Goertzel fixed-point;
- ceiling -1 dBTP.

A implementação precisa de vetores formais antes de qualquer claim normativo BS.1770/EBU.

## Arquitetura

```text
Android I/O/UI/codecs
        |
      JNI
        |
  -----------------
  |               |
DSP core       Meter core
freestanding   freestanding
caller-state   caller-state
fixed-point    fixed-point
```

O APK inteiro não é freestanding: microfone, tela, armazenamento e codec dependem da plataforma Android. O core é a unidade portátil e auditável.


## Delta 4 — Audio Manifold / ZRF-CFR

Branch de implementação: `feature/audio-manifold-zrf-cfr-v1`.

### Micromódulos freestanding

- `rfa_wave_core`: banco de até 16 senoides Q15 por acumulador de fase, sem `libm`;
- `rfa_matrix_core`: matriz Q15 caller-owned até 16×16;
- `rfa_block_core`: ring buffer caller-owned e perfis 128/512/4096;
- `rfa_container_core`: cabeçalhos ZRF/CFR, chunks tipados e checksum bounded;
- `rfa_fir_core`: FIR/convolution caller-owned para IR/reverb/correction;
- `rfa_time_core`: time-map/resampling linear Q16 (não pitch-preserving);
- `rfa_lms_core`: cancelamento adaptativo LMS com referência;
- `rfa_biquad_core`: banco de até 16 seções Q30 para EQ/filtros com coeficientes explícitos.

Todos entram no mesmo gate `-nostdinc -ffreestanding -fno-builtin` dos núcleos existentes.

### Studio surface

A interface ganhou uma superfície profissional navegável:

```text
REC | EDIT | CAL | SPEC | ROOM | VOICE | MASTER | EXPORT
```

O workspace recebe waveform vivo limitado, telemetria da captura e espectro/master já produzidos pelo pipeline. Curvas de calibração/room não são simuladas: permanecem `TOKEN_VAZIO` até medição.

### ZRF / CFR

- o PCM bruto continua preservado;
- antes do mastering, a origem processada também recebe um sidecar `ZRF1`;
- ZRF v1 já contém cabeçalho + chunk PCM real;
- WAVE/MATR/CAL/IR/SPEC/PHON/ROOM/RCPT estão registrados como tipos de chunk;
- CFR possui codec de cabeçalho/chunk, mas captura de calibração permanece `PENDING`;
- FIR/LMS/time-map/biquad são kernels de referência e ainda não estão todos ligados ao workflow visual.

Ver `docs/ZRF_CFR_FORMAT_V1.md`.

### Evidência

```text
DELTA4_SOURCE = IMPLEMENTED
DELTA4_HOST_SMOKE = PENDING_CI
DELTA4_ARMV7_ZERO_UNDEFINED = PENDING_CI
DELTA4_AARCH64_ZERO_UNDEFINED = PENDING_CI
DELTA4_APK_BUILD = PENDING_CI
DELTA4_PHYSICAL_ANDROID = NOT_RUN
CFR_CALIBRATION_CAPTURE = PENDING
ABSOLUTE_SPL = TOKEN_VAZIO
claim_allowed = false
```

### Research boundary

Poincaré/7D/14-axis, fonética, neurociência e hipóteses quânticas podem entrar como representações/experimentos com schema próprio. Nenhuma dessas camadas é promovida a mecanismo físico pelo simples fato de existir código ou matriz.

Arquitetura: `docs/AUDIO_MANIFOLD_CONTRACT_V1.md`.

### Licenciamento

Novos módulos usam:

```text
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
```

Pesquisa/avaliação não comercial é permitida nos termos de `LICENSE_RESEARCH_COMMERCIAL.md`; uso comercial requer acordo escrito separado.

## F_next

1. executar CI do Delta 3;
2. corrigir qualquer regressão de compilação/símbolo;
3. promover apenas gates comprovados para PASS;
4. instalar APK no Android 10 armeabi-v7a;
5. captura física + Ogg/Opus round-trip;
6. vetores BS.1770/EBU;
7. inspeção de clipping/true-peak.


## Delta 4.2 — CFR relative physical capture

Implemented on `feature/cfr-relative-calibration-v1`:

- deterministic Q15 sync sequence for bounded latency alignment;
- fixed-point exponential sweep already produced by `rfa_measure_core`;
- Android `AudioTrack -> room -> AudioRecord` I/O edge at 48 kHz mono;
- `UNPROCESSED` input with `MIC` fallback;
- conservative excitation gain and output volume cap;
- CFR recording with `WAVE + CAL + PCM(reference) + PCM(response)`;
- CAL workspace action wired to the measurement path.

Current state:

```text
CFR_RELATIVE_SOURCE = IMPLEMENTED
CFR_RELATIVE_CI = PENDING
CFR_RELATIVE_PHYSICAL_ANDROID = NOT_RUN
ABSOLUTE_SPL = TOKEN_VAZIO
FREQUENCY_TRANSFER_DECONVOLUTION = PENDING
RT60 = PENDING
ROOM_CORRECTION = PENDING
```

The captured CFR is deliberately sufficient for later re-analysis: the original excitation and microphone response are preserved rather than only storing a derived curve.

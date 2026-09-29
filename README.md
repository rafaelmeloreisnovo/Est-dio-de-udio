# Rafaelia Audio Studio

Android 10+ audio workstation para captura, narração guiada, tratamento, medição, normalização e export Ogg/Opus.

## Estado operacional

- MAIN_HEAD_AFTER_PR7: 452de8d1276137b093b05fc2d956f565757620db
- LAST_VALIDATED_SOURCE_HEAD: 356bafe81f148e972ece53274dde86208864b90b
- LAST_VALIDATED_PR_GATE: PASS (run 36528458788 / #84)
- EXECUTION_TARGET: Android API 29+
- DSP_HOST_SMOKE_VALIDATED: PASS
- METER_HOST_SMOKE_VALIDATED: PASS
- MANIFOLD_HOST_SMOKE_VALIDATED: PASS
- ARMV7_ZERO_UNDEFINED_VALIDATED: PASS
- ARMV7_ABI_MANIFEST_VALIDATED: PASS
- AARCH64_ZERO_UNDEFINED_VALIDATED: PASS
- APK_BUILD_VALIDATED: PASS
- CFR_RELATIVE_CAPTURE_SOURCE: PASS_CI
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
ABSOLUTE_SPL = PENDING_PHYSICAL_REFERENCE
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
CFR_RELATIVE_CI = PASS
CFR_RELATIVE_PHYSICAL_ANDROID = NOT_RUN
ABSOLUTE_SPL = PENDING_PHYSICAL_REFERENCE
FREQUENCY_TRANSFER_DECONVOLUTION = PENDING
RT60 = PENDING
ROOM_CORRECTION = PENDING
```

The captured CFR is deliberately sufficient for later re-analysis: the original excitation and microphone response are preserved rather than only storing a derived curve.


## Documentation & evidence product layer

Canonical route: `docs/START_HERE.md`.

The project now ships a publication-grade documentation family:

- product/public: `PRODUCT_OVERVIEW.md`, `PUBLIC_PRODUCT_BRIEF.md`, `USER_GUIDE.md`;
- technical: `DEVELOPER_GUIDE.md`, `AUDIO_MANIFOLD_CONTRACT_V1.md`, `ZRF_CFR_FORMAT_V1.md`;
- assurance: `VERIFICATION_AND_EVIDENCE.md`, `INSTALLATION_VALIDATION.md`, `RELEASE_READINESS.md`;
- hardware: `HARDWARE_TEST_PROTOCOL.md`, `MICRO_DELTA_VIBRATION_V1.md`;
- navigation: `PUBLICATION_INDEX.md`, `DOCUMENT_PRODUCTS.md`, `CONCEPT_FAMILY_ATLAS.md`, `GLOSSARY.md`, `FAQ.md`;
- governance: scoped `AGENTS.md` files at repository, app, C core, Java platform, docs and CI levels.

### In-app proof bundle

The app exposes **Gerar provas + teste μ∆**. It generates a shareable evidence file with:

- installed APK SHA-256;
- package/version/install/update metadata;
- embedded source SHA / CI coordinates when built by canonical CI;
- Android/ABI/audio capability data;
- sensor inventory;
- bounded accelerometer μ∆ vibration observation;
- hashes of available ZRF/CFR/master artifacts.

`μ∆` is a temporal delta operator. Vibration observation is not promoted to hardware-fault diagnosis without a validated reference/model.


## Delta 4.4 — Adaptive low UI + dependency boundary

The primary studio surface is now auto-adaptive:

- compact phone: top workspace tabs + central console + persistent transport;
- wide/landscape: left workspace rail + central console + right inspector;
- persistent actions: `REC | STOP | PLAY | CAL | PROOF`;
- detailed legacy controls remain available below the console.

Low-dependency state:

```text
JAVA_THIRD_PARTY_DEPS = 0
ANDROIDX = 0
KOTLIN = 0
R8_SHRINK = 0
JNI_IMPLEMENTATION_FILES = 1
PHONE_ABIS = armeabi-v7a + arm64-v8a
EVIDENCE_SHA256_PROVIDER = project-local LowSha256
```

Android SDK/DEX/platform APIs remain an irreducible shell for an Android application. They are not DSP/runtime-library dependencies of the freestanding core.

Resolvable empty tokens now become explicit states such as `INPUT_IDLE`, `UNAVAILABLE_NOT_REPORTED`, `NOT_RUN` and `PENDING_PHYSICAL_REFERENCE`. Undefined semantics such as an unresolved NIU hardware definition remain `TOKEN_VAZIO`.

See `docs/ADAPTIVE_STUDIO_UI_V1.md`, `docs/LOW_LEVEL_BOUNDARY_V1.md` and `docs/TOKEN_GAP_RECONCILIATION_V1.md`.

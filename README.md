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

## F_next

1. executar CI do Delta 3;
2. corrigir qualquer regressão de compilação/símbolo;
3. promover apenas gates comprovados para PASS;
4. instalar APK no Android 10 armeabi-v7a;
5. captura física + Ogg/Opus round-trip;
6. vetores BS.1770/EBU;
7. inspeção de clipping/true-peak.

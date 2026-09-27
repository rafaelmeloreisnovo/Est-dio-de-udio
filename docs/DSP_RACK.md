# DSP Rack interno — Delta 2

## Regra de nomenclatura

O projeto implementa um **rack DSP interno** com arquitetura semelhante a uma cadeia de plugins.
Ele não se declara VST2/VST3, porque não incorpora nem implementa a ABI/SDK VST.

VST3_HOST = PENDING
VST3_PLUGIN_ABI = PENDING
INTERNAL_DSP_RACK = IMPLEMENTED_UNTESTED

## Cadeia atual

1. DC/high-pass cleanup
2. noise gate / downward expansion suave
3. speech leveler
4. sample-domain soft limiter
5. medição K-weighted separada
6. true-peak FIR 4x separado
7. normalização gated em múltiplos passes

A medição fica fora da cadeia de efeito para não confundir transformação com evidência.

## Presets

### VOICE_WHATSAPP
- high-pass mais agressivo;
- gate ativo;
- leveler de fala;
- limiter;
- target padrão do produto: -16 LUFS (workflow, não norma);
- ceiling: -1 dBTP.

### VOICE_NATURAL / NARRATION
- high-pass mais conservador;
- gate mais suave;
- leveler menos agressivo;
- target sugerido: -18 LUFS (workflow).

### BROADCAST
Usa o preset de voz natural, mas a normalização aponta para -23 LUFS conforme EBU R128.
Conformidade formal depende de vetores EBU/ITU e permanece PENDING.

### MUSIC_CLEAN
Scaffold conservador; não é um mastering musical completo.
Não há claim de de-esser, multiband compressor, stereo imager ou EQ paramétrico enquanto esses módulos não forem implementados e testados.

## Freestanding boundary

dsp_core.c e meter_core.c:
- sem malloc/calloc/realloc/free;
- sem libm;
- sem stdio;
- sem filesystem;
- sem rede;
- sem threads;
- sem dependência externa de DSP;
- fixed-point;
- buffers fornecidos pelo chamador.

jni_bridge.c é somente a fronteira Android/JVM. O APK inteiro não é bare metal.

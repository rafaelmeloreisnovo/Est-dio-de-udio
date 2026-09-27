# Arquitetura operacional

## Invariantes

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

O Android fornece captura, codecs, contêiner e armazenamento. O kernel DSP não faz chamadas Android.

## Fronteiras

### Camada Android

Responsável por:
- permissão de microfone;
- AudioRecord;
- Storage Access Framework;
- MediaExtractor/MediaCodec;
- MediaMuxer Ogg;
- MediaStore;
- share intent.

Essa camada não é freestanding e não deve ser descrita como bare metal.

### JNI

Somente ponte de buffers.

### dsp_core.c

Contrato:
- C11;
- sem malloc/free;
- sem libm;
- sem stdio;
- sem filesystem;
- sem rede;
- sem threads;
- sem syscalls explícitas;
- estado fixo para até dois canais;
- PCM16 in-place;
- aritmética principal Q31.

O workflow compila o objeto ARMv7 com -ffreestanding e exige zero símbolos indefinidos no objeto do núcleo.

## Tuning

Preset VOICE_WHATSAPP:
- entrada e saída alvo: 48 kHz PCM16;
- high-pass de primeira ordem aproximado ~70 Hz;
- gate/expansão suave abaixo de aproximadamente -52 dBFS;
- leveler conservador em três regiões;
- limiter com joelho simples acima de aproximadamente -1 dBFS.

Isso é um chain de voz determinístico. Não substitui medição BS.1770 nem um mastering musical completo.

## Perfis futuros

VOICE_NATURAL: IMPLEMENTED_UNTESTED.
MUSIC_CLEAN: IMPLEMENTED_UNTESTED.
PODCAST_R128: PENDING.
MUSIC_MASTER_STEREO: PENDING.
RESTORATION_DEESSER: PENDING.
PARAMETRIC_EQ: PENDING.
TRUE_PEAK_OVERSAMPLING: PENDING.

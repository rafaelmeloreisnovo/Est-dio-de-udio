# Arquitetura operacional

## Invariantes

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

A arquitetura separa rigidamente duas zonas:

1. **CORE_AUTHORIAL_FREESTANDING** — DSP e medição sem runtime hospedado.
2. **PLATFORM_ADAPTER** — Android/JNI, necessário apenas para microfone, UI, storage e codecs do sistema.

Não se declara o APK inteiro como bare-metal. A propriedade freestanding pertence ao núcleo C auditado.

## CORE_AUTHORIAL_FREESTANDING

Arquivos:
- `rfa_core_types.h`
- `dsp_core.h/.c`
- `meter_core.h/.c`

Contrato verificável:
- C11;
- `-ffreestanding`;
- `-fno-builtin`;
- `-nostdinc`;
- sem headers de sistema;
- sem libc/libm;
- sem malloc/calloc/realloc/free;
- sem stdio;
- sem filesystem;
- sem rede;
- sem threads;
- sem syscalls explícitas;
- sem VST SDK ou biblioteca DSP de terceiros;
- sem estado mutável global no core;
- estado fornecido explicitamente pelo chamador;
- buffers fornecidos pelo chamador;
- fixed-point;
- ARMv7 e AArch64 com zero símbolos indefinidos;
- superfície ABI externa limitada por allowlist do CI.

O core não conhece Android, Java, JNI, arquivos, sockets, relógio ou allocator.

## PLATFORM_ADAPTER

### JNI

`jni_bridge.c` é a primeira camada hospedada. Ela possui as instâncias de `dsp_state` e `meter_state` usadas pelo aplicativo e somente converte buffers/tipos JNI para a API do core.

Estado global de plataforma pode existir aqui porque esta camada não reivindica freestanding. Ele não atravessa para o core.

### Android

Responsável por:
- permissão de microfone;
- AudioRecord / AudioTrack;
- Storage Access Framework;
- MediaExtractor / MediaCodec;
- MediaMuxer Ogg;
- MediaStore;
- share intent.

Essa camada é deliberadamente substituível. Um futuro adaptador Termux, Linux, firmware ou bare-metal pode chamar o mesmo core sem portar Android.

## Estado explícito

O DSP usa `dsp_state` e o meter usa `meter_state`. Isso elimina dependência de estado oculto entre execuções e permite:
- múltiplas instâncias independentes;
- testes determinísticos;
- uso em firmware;
- reentrância controlada pelo chamador;
- memória estática, stack ou região fornecida externamente, sem heap.

## Gate

O PR gate deve falhar quando:
- um header `<...>` entra no core;
- aparece chamada para função hospedada proibida;
- aparece variável `static` mutável em file-scope no core;
- ARMv7/AArch64 geram símbolo indefinido;
- a ABI exporta símbolo fora da allowlist;
- smoke tests divergirem;
- o APK deixar de compilar.

## Limite de autoria

Algoritmos, fixed-point, estados, filtros, medição e transformação podem ser autorais e vivem no core.

Captura física do microfone, composição de tela, sandbox Android e acesso ao codec hardware/software do sistema são serviços da plataforma. Reimplementá-los dentro do APK sem Android não removeria a dependência do kernel/driver; apenas deslocaria a fronteira. Por isso a fronteira é explícita e auditável.

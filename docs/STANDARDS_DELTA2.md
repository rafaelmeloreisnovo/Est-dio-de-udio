# Standards map — Delta 2

## ITU-R BS.1770-5

Implementado no núcleo, ainda não promovido:
- K-weighting de duas etapas em 48 kHz, coeficientes quantizados Q29;
- blocos de 400 ms;
- overlap de 75%;
- gate absoluto equivalente a -70 LKFS;
- gate relativo -10 LU em domínio de potência;
- FIR true-peak de ordem 48, 4 fases, 4x para 48 -> 192 kHz.

Estado: IMPLEMENTED_UNTESTED.
A promoção exige vetores de conformidade.

## EBU R128 v5 / Tech 3341

- target -23 LUFS disponível como perfil Broadcast;
- -18 e -16 LUFS são targets autorais de workflow e não são apresentados como EBU;
- true peak é usado como ceiling no ganho final;
- LRA, Momentary e Short-term display permanecem PENDING.

## Android

A camada Android usa apenas APIs de plataforma para:
- AudioRecord;
- AudioTrack;
- MediaCodec;
- MediaExtractor;
- MediaMuxer;
- MediaStore;
- permissões e UI.

Não há biblioteca DSP de terceiros.
AAudio/Oboe não foram adicionados no Delta 2: Oboe seria dependência externa e AAudio exigiria uma segunda fronteira nativa de I/O. O modo atual mantém o I/O em Java e o processamento no núcleo freestanding.

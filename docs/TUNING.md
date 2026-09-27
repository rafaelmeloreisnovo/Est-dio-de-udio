# Tuning profissional sem rotular o que não foi medido

## Voz curta / mensageria

Prioridades:
1. inteligibilidade;
2. ruído de fundo controlado;
3. picos controlados;
4. pouca latência de processamento offline;
5. arquivo pequeno e interoperável.

Por isso o Delta 1 usa Ogg/Opus e um chain conservador de voz.

## Podcast / narração

Precisa, além do chain de voz:
- loudness integrado real;
- short-term loudness;
- true peak;
- controle de sibilância;
- equalização por voz/ambiente.

Estado: PENDING para os itens não implementados.

## Música

Mastering musical não deve reutilizar cegamente preset de voz.
Necessita preservar estéreo, transientes e balanço tonal, além de medições por programa.
O preset MUSIC_CLEAN atual é somente um scaffold conservador, não um master musical certificado.

## Gravação

Quando o aparelho declarar suporte a UNPROCESSED, ele é preferido para evitar AGC/NS do fabricante.
Quando não houver suporte, VOICE_RECOGNITION é usado como fallback.
Em uma fase posterior, AAudio poderá fornecer monitoramento de baixa latência; o callback deverá evitar alocação, I/O e locks.

## WhatsApp / Ogg Opus

O app usa:
- input: áudio que MediaExtractor/MediaCodec consiga decodificar para PCM16; o alvo principal do Delta 1 é Ogg/Opus 48 kHz;
- output: Opus dentro de Ogg via MediaCodec + MediaMuxer;
- MIME: audio/ogg;
- extensão: .opus.

Resampler para fontes diferentes de 48 kHz: PENDING.

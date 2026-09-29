# Procedimento de gravação e narração

## 1. Wizard / pré-voo

O primeiro uso apresenta:
- versão/API Android;
- permissão de microfone;
- disponibilidade declarada de UNPROCESSED;
- sample-rate de saída informado pelo AudioManager;
- frames por buffer quando informados pelo aparelho;
- distinção entre target normativo e target de workflow.

Ausência de propriedade reportada = `UNAVAILABLE_NOT_REPORTED`; `TOKEN_VAZIO` is reserved for unresolved semantics/source.

## 2. Captura

Formato de trabalho:
- 48 kHz;
- PCM16;
- mono para narração;
- arquivo bruto preservado no cache da sessão.

Fonte:
- UNPROCESSED quando o hardware declara suporte;
- VOICE_RECOGNITION caso contrário.

O app tenta desabilitar AGC, NS e AEC da sessão quando há implementação e controle disponíveis.
Isso é best-effort; o hardware/firmware pode manter processamento próprio.

## 3. Teleprompter

O narrador pode:
- escrever ou colar o roteiro;
- carregar o texto em uma área de leitura grande;
- ajustar velocidade aproximada em WPM;
- iniciar/parar rolagem;
- iniciar gravação com contagem regressiva de 3 segundos;
- gravar enquanto o texto avança.

WPM controla velocidade de rolagem, não mede velocidade real de fala.

## 4. Telemetria durante captura

Exibe:
- source escolhida;
- sample peak em %FS;
- RMS bruto em %FS;
- contagem de samples clipados.

Esses números são diagnóstico de captura. RMS bruto não é LUFS.

## 5. Remasterização

Passo A: DSP e primeiro gate.
Passo B: segundo gate relativo.
Passo C: ganho de normalização limitado por true peak.
Passo D: Ogg/Opus.

O master PCM pode ser ouvido antes/independentemente do compartilhamento.

## 6. Espectrometria

16 centros:
80, 125, 200, 315, 500, 800, 1250, 2000, 3150, 5000,
8000, 10000, 12500, 16000, 20000 e 22000 Hz.

Implementação: banco Goertzel fixed-point em 48 kHz.
A visualização é relativa; ainda não é um analisador FFT calibrado em dB SPL.

## 7. Gate de qualidade

Antes de promover para PASS:
- build APK;
- teste físico do microfone;
- arquivo WhatsApp real;
- reprodução sem underruns audíveis;
- vetores BS.1770/EBU para loudness;
- vetores true-peak;
- comparação antes/depois sem clipping;
- teste de fala feminina/masculina, silêncio, ruído estacionário e música.

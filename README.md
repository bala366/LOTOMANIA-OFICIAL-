# Lotomania Engrossando o Talo V4 — Paridade Pydroid

Esta versão corrige a divergência do V3.

## Mudança principal

O cálculo NÃO foi reescrito em Java.

O aplicativo Android carrega e executa o arquivo:

`app/src/main/python/motor_lotomania.py`

Esse arquivo contém o motor Python do Pydroid V3:
- mesma semente `20260829`;
- mesmos pesos;
- mesmas 10 reinicializações;
- mesmas 7000 tentativas;
- mesma lógica de `random`;
- mesma busca por 50 dezenas;
- mesma distribuição 10x10;
- mesmo tratamento de repetidas;
- último concurso retirado do cálculo das forças, como no Pydroid.

Java é usado somente para:
- dashboard;
- selecionar TXT;
- mostrar resultado;
- gerar PDF.

Objetivo: mesmo TXT + motor Python = jogo do Pydroid.

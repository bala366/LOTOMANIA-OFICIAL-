import re
import math
import random
import json

JANELA_TALO = 18
JANELA_CURTA = 6
JANELA_PERSISTENCIA = 30

TAMANHO_JOGO = 50

ALVO_REPETIDAS = 10
MIN_REPETIDAS = 8
MAX_REPETIDAS = 13

REINICIOS = 10
TENTATIVAS_POR_REINICIO = 7000
TEMPERATURA_INICIAL = 5.0

SEMENTE = 20260829


def limitar(v, minimo, maximo):
    return max(minimo, min(maximo, v))


def formatar_dezena(n):
    if n == 100:
        return "00"
    return f"{n:02d}"


def formatar_grupo(grupo):
    return " ".join(formatar_dezena(n) for n in sorted(grupo))


def slope(valores):
    n = len(valores)

    if n < 2:
        return 0.0

    mx = (n - 1) / 2.0
    my = sum(valores) / n

    num = 0.0
    den = 0.0

    for i, y in enumerate(valores):
        dx = i - mx
        num += dx * (y - my)
        den += dx * dx

    if den == 0:
        return 0.0

    return num / den


def ler_historico_texto(texto):
    historico = []

    for linha_txt in texto.splitlines():

        numeros_txt = re.findall(r"\d+", linha_txt)

        if len(numeros_txt) < 20:
            continue

        valores = [int(x) for x in numeros_txt]

        if len(valores) >= 21:
            concurso = valores[0]
            candidatos = valores[1:]
        else:
            concurso = len(historico) + 1
            candidatos = valores

        dezenas = []
        vistos = set()

        for n in candidatos:

            if n == 0:
                n = 100

            if 1 <= n <= 100 and n not in vistos:
                vistos.add(n)
                dezenas.append(n)

                if len(dezenas) == 20:
                    break

        if len(dezenas) == 20:
            historico.append({
                "concurso": concurso,
                "dezenas": tuple(sorted(dezenas))
            })

    if not historico:
        raise ValueError("Nenhum concurso válido encontrado.")

    historico.sort(key=lambda x: x["concurso"])

    return historico


def calcular_forcas(historico):
    total = len(historico)

    janela18 = historico[-min(JANELA_TALO, total):]
    janela30 = historico[-min(JANELA_PERSISTENCIA, total):]

    frequencia_total = [0] * 101

    for concurso in historico:
        for n in concurso["dezenas"]:
            frequencia_total[n] += 1

    forca = {}

    for n in range(1, 101):

        serie18 = [
            1 if n in c["dezenas"] else 0
            for c in janela18
        ]

        serie6 = serie18[-6:]

        freq_hist = frequencia_total[n] / total * 100
        freq18 = sum(serie18) / len(serie18) * 100
        freq6 = sum(serie6) / len(serie6) * 100

        persist30 = (
            sum(1 for c in janela30 if n in c["dezenas"])
            / len(janela30)
            * 100
        )

        sl18 = slope(serie18)
        sl6 = slope(serie6)

        metade = max(1, len(serie18) // 2)

        antiga = serie18[:metade]
        recente = serie18[metade:]

        media_antiga = sum(antiga) / len(antiga)

        media_recente = (
            sum(recente) / len(recente)
            if recente
            else 0
        )

        crescimento = media_recente - media_antiga

        score = (
            freq_hist * 0.12
            + freq18 * 0.24
            + freq6 * 0.28
            + persist30 * 0.12
            + limitar(sl18 * 110, -12, 12)
            + limitar(sl6 * 140, -16, 16)
            + limitar(crescimento * 36, -12, 12)
        )

        forca[n] = {
            "score": score,
            "serie18": serie18,
            "freq_hist": freq_hist,
            "freq18": freq18,
            "freq6": freq6,
            "slope18": sl18,
            "slope6": sl6,
            "crescimento": crescimento
        }

    return forca


def linha_numero(n):
    if n == 100:
        return 9
    return (n - 1) // 10


def coluna_numero(n):
    if n == 100:
        return 9
    return (n - 1) % 10


def score_distribuicao(jogo):
    s = set(jogo)
    score = 0.0

    linhas = [0] * 10
    colunas = [0] * 10

    for n in s:
        linhas[linha_numero(n)] += 1
        colunas[coluna_numero(n)] += 1

    for qtd in linhas:
        if qtd == 5:
            score += 6
        elif qtd in (4, 6):
            score += 4
        elif qtd in (3, 7):
            score += 1
        elif qtd <= 2:
            score -= 8
        elif qtd >= 8:
            score -= 8

    for qtd in colunas:
        if qtd == 5:
            score += 5
        elif qtd in (4, 6):
            score += 3.5
        elif qtd in (3, 7):
            score += 1
        elif qtd <= 2:
            score -= 7
        elif qtd >= 8:
            score -= 7

    for lin in range(10):

        sequencia = 0

        for col in range(10):

            n = lin * 10 + col + 1

            if n in s:
                sequencia += 1
            else:

                if sequencia == 2:
                    score += 1
                elif sequencia == 3:
                    score += 4
                elif sequencia == 4:
                    score += 7
                elif sequencia == 5:
                    score += 6
                elif sequencia == 6:
                    score += 2
                elif sequencia >= 7:
                    score -= (sequencia - 6) * 4

                sequencia = 0

        if sequencia == 2:
            score += 1
        elif sequencia == 3:
            score += 4
        elif sequencia == 4:
            score += 7
        elif sequencia == 5:
            score += 6
        elif sequencia == 6:
            score += 2
        elif sequencia >= 7:
            score -= (sequencia - 6) * 4

    for lin in range(10):

        buraco = 0

        for col in range(10):

            n = lin * 10 + col + 1

            if n not in s:
                buraco += 1
            else:

                if buraco >= 4:
                    score -= (buraco - 3) * 5
                elif buraco in (1, 2):
                    score += 0.6

                buraco = 0

        if buraco >= 4:
            score -= (buraco - 3) * 5
        elif buraco in (1, 2):
            score += 0.6

    pares = sum(1 for n in s if n % 2 == 0)
    score -= abs(pares - 25) * 1.2

    baixas = sum(1 for n in s if n <= 50)
    score -= abs(baixas - 25) * 1.0

    return score


def score_talo_jogo(jogo, historico):
    s = set(jogo)

    janela18 = historico[-min(JANELA_TALO, len(historico)):]

    serie = []

    for concurso in janela18:

        acertos = sum(
            1
            for n in concurso["dezenas"]
            if n in s
        )

        serie.append(acertos)

    serie6 = serie[-6:]

    pesos18 = list(range(1, len(serie) + 1))

    media18 = (
        sum(v * p for v, p in zip(serie, pesos18))
        / sum(pesos18)
    )

    pesos6 = list(range(1, len(serie6) + 1))

    media6 = (
        sum(v * p for v, p in zip(serie6, pesos6))
        / sum(pesos6)
    )

    sl18 = slope(serie)
    sl6 = slope(serie6)

    metade = max(1, len(serie) // 2)

    antiga = serie[:metade]
    recente = serie[metade:]

    crescimento = (
        sum(recente) / len(recente)
        -
        sum(antiga) / len(antiga)
    )

    qtd10 = sum(1 for x in serie if x >= 10)
    qtd11 = sum(1 for x in serie if x >= 11)
    qtd12 = sum(1 for x in serie if x >= 12)
    qtd13 = sum(1 for x in serie if x >= 13)
    qtd14 = sum(1 for x in serie if x >= 14)

    subidas = sum(
        1
        for a, b in zip(serie, serie[1:])
        if b > a
    )

    percentual_subidas = (
        subidas
        /
        max(1, len(serie) - 1)
        *
        100
    )

    score = (
        media18 * 2.3
        + media6 * 3.2
        + limitar(sl18 * 20, -12, 12)
        + limitar(sl6 * 24, -16, 16)
        + limitar(crescimento * 6, -12, 12)
        + percentual_subidas * 0.10
        + qtd10 * 0.25
        + qtd11 * 0.45
        + qtd12 * 0.80
        + qtd13 * 1.30
        + qtd14 * 2.00
    )

    return score, {
        "serie18": serie,
        "serie6": serie6,
        "media18": media18,
        "media6": media6,
        "slope18": sl18,
        "slope6": sl6,
        "crescimento": crescimento,
        "qtd10": qtd10,
        "qtd11": qtd11,
        "qtd12": qtd12,
        "qtd13": qtd13,
        "qtd14": qtd14
    }


def avaliar_jogo(jogo, forca, historico_analise, ultimo_set):
    s = set(jogo)

    media_forca = (
        sum(forca[n]["score"] for n in s)
        / len(s)
    )

    repetidas = len(s & ultimo_set)

    if MIN_REPETIDAS <= repetidas <= MAX_REPETIDAS:

        score_rep = 12.0

        score_rep -= (
            abs(repetidas - ALVO_REPETIDAS)
            * 1.2
        )

    else:

        distancia = min(
            abs(repetidas - MIN_REPETIDAS),
            abs(repetidas - MAX_REPETIDAS)
        )

        score_rep = -8 - distancia * 4

    talo_score, talo_dados = score_talo_jogo(
        s,
        historico_analise
    )

    distribuicao = score_distribuicao(s)

    score_final = (
        media_forca * 0.72
        + talo_score * 1.20
        + distribuicao * 0.80
        + score_rep
    )

    return score_final, {
        "media_forca": media_forca,
        "repetidas": repetidas,
        "score_repetidas": score_rep,
        "score_distribuicao": distribuicao,
        "score_talo": talo_score,
        "talo": talo_dados
    }


def criar_jogo_inicial(forca, ultimo_set):

    ranking = sorted(
        range(1, 101),
        key=lambda n: (
            forca[n]["score"],
            forca[n]["slope6"],
            forca[n]["slope18"],
            n
        ),
        reverse=True
    )

    repetidas = [
        n
        for n in ranking
        if n in ultimo_set
    ][:ALVO_REPETIDAS]

    jogo = set(repetidas)

    for n in ranking:

        if len(jogo) >= TAMANHO_JOGO:
            break

        jogo.add(n)

    return jogo


def buscar_melhor_jogo(historico, forca, ultimo_set):

    historico_analise = historico[:-1]

    melhor_global = None
    melhor_score_global = float("-inf")
    melhor_dados_global = None

    base = criar_jogo_inicial(forca, ultimo_set)

    for reinicio in range(1, REINICIOS + 1):

        if reinicio == 1:

            atual = set(base)

        else:

            atual = set(base)

            qtd_trocas = random.randint(6, 16)

            for _ in range(qtd_trocas):

                sai = random.choice(tuple(atual))

                fora = list(
                    set(range(1, 101))
                    -
                    atual
                )

                entra = random.choice(fora)

                atual.remove(sai)
                atual.add(entra)

        score_atual, dados_atual = avaliar_jogo(
            atual,
            forca,
            historico_analise,
            ultimo_set
        )

        melhor_local = set(atual)
        melhor_score_local = score_atual
        melhor_dados_local = dados_atual

        for tentativa in range(
            1,
            TENTATIVAS_POR_REINICIO + 1
        ):

            temperatura = (
                TEMPERATURA_INICIAL
                *
                (
                    1
                    -
                    tentativa
                    /
                    TENTATIVAS_POR_REINICIO
                )
            )

            sai = random.choice(
                tuple(atual)
            )

            while True:

                entra = random.randint(
                    1,
                    100
                )

                if entra not in atual:
                    break

            candidato = set(atual)

            candidato.remove(sai)
            candidato.add(entra)

            novo_score, novos_dados = avaliar_jogo(
                candidato,
                forca,
                historico_analise,
                ultimo_set
            )

            diferenca = novo_score - score_atual

            aceitar = False

            if diferenca >= 0:

                aceitar = True

            elif temperatura > 0:

                prob = math.exp(
                    diferenca
                    /
                    temperatura
                )

                if random.random() < prob:
                    aceitar = True

            if aceitar:

                atual = candidato
                score_atual = novo_score
                dados_atual = novos_dados

            if score_atual > melhor_score_local:

                melhor_local = set(atual)
                melhor_score_local = score_atual
                melhor_dados_local = dados_atual

        if melhor_score_local > melhor_score_global:

            melhor_global = set(melhor_local)
            melhor_score_global = melhor_score_local
            melhor_dados_global = melhor_dados_local

    return (
        sorted(melhor_global),
        melhor_score_global,
        melhor_dados_global
    )


def executar_texto(texto):

    random.seed(SEMENTE)

    historico = ler_historico_texto(texto)

    if len(historico) < 30:
        raise ValueError(
            f"Histórico insuficiente: {len(historico)} concursos."
        )

    ultimo = historico[-1]

    # IDÊNTICO AO PYDROID:
    # força calculada sem o último concurso.
    historico_analise = historico[:-1]

    forca = calcular_forcas(historico_analise)

    ultimo_set = set(ultimo["dezenas"])

    jogo, score, dados = buscar_melhor_jogo(
        historico,
        forca,
        ultimo_set
    )

    repetidas = sorted(set(jogo) & ultimo_set)
    novas = sorted(set(jogo) - ultimo_set)
    espelho = sorted(set(range(1, 101)) - set(jogo))

    linhas = [0] * 10
    colunas = [0] * 10

    for n in jogo:
        linhas[linha_numero(n)] += 1
        colunas[coluna_numero(n)] += 1

    pares = sum(1 for n in jogo if n % 2 == 0)
    baixas = sum(1 for n in jogo if n <= 50)

    retorno = {
        "ultimo_concurso": ultimo["concurso"],
        "ultimo_resultado": list(ultimo["dezenas"]),
        "jogo": jogo,
        "repetidas": repetidas,
        "novas": novas,
        "espelho": espelho,
        "score_final": score,
        "score_talo": dados["score_talo"],
        "score_distribuicao": dados["score_distribuicao"],
        "media_forca": dados["media_forca"],
        "serie18": dados["talo"]["serie18"],
        "serie6": dados["talo"]["serie6"],
        "slope18": dados["talo"]["slope18"],
        "slope6": dados["talo"]["slope6"],
        "crescimento": dados["talo"]["crescimento"],
        "linhas": linhas,
        "colunas": colunas,
        "pares": pares,
        "impares": 50 - pares,
        "baixas": baixas,
        "altas": 50 - baixas,
        "motor": "PYTHON_PYDROID_V3"
    }

    return json.dumps(
        retorno,
        ensure_ascii=False
    )

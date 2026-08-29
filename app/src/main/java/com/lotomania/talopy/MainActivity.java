package com.lotomania.talopy;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.widget.*;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    static final int PICK_TXT = 401;
    static final int SAVE_PDF = 402;

    final ExecutorService executor = Executors.newSingleThreadExecutor();

    Button importar, gerar, pdf;
    TextView status, saida;
    ProgressBar progresso;

    String textoHistorico = null;
    Resultado resultado = null;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        montarTela();
    }

    void montarTela() {
        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 22, 22, 50);
        root.setBackgroundColor(Color.WHITE);

        scroll.addView(root);

        TextView titulo = new TextView(this);
        titulo.setText("☘ LOTOMANIA\nENGROSSANDO O TALO");
        titulo.setTextSize(25);
        titulo.setTextColor(Color.WHITE);
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(18, 28, 18, 28);
        titulo.setBackgroundColor(Color.rgb(244, 123, 32));
        root.addView(titulo, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText("V4 • MOTOR PYTHON DO PYDROID • PARIDADE");
        sub.setTextSize(15);
        sub.setPadding(0, 18, 0, 12);
        root.addView(sub);

        importar = botao("IMPORTAR HISTÓRICO TXT");
        gerar = botao("GERAR O MESMO JOGO DO PYDROID");
        pdf = botao("GERAR PDF");

        gerar.setEnabled(false);
        pdf.setEnabled(false);

        root.addView(importar);
        root.addView(gerar);

        progresso = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progresso.setMax(100);
        root.addView(progresso, new LinearLayout.LayoutParams(-1, 20));

        status = new TextView(this);
        status.setText("Aguardando histórico.");
        status.setTextSize(15);
        status.setPadding(0, 12, 0, 12);
        root.addView(status);

        root.addView(pdf);

        saida = new TextView(this);
        saida.setTextSize(13);
        saida.setTextIsSelectable(true);
        saida.setPadding(0, 18, 0, 40);
        root.addView(saida);

        importar.setOnClickListener(v -> abrirTxt());
        gerar.setOnClickListener(v -> gerar());
        pdf.setOnClickListener(v -> abrirPdf());

        setContentView(scroll);
    }

    Button botao(String t) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextSize(16);
        return b;
    }

    void abrirTxt() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        startActivityForResult(i, PICK_TXT);
    }

    void abrirPdf() {
        if (resultado == null) return;

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, "LOTOMANIA_TALO_V4_PARIDADE_PYDROID.pdf");
        startActivityForResult(i, SAVE_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        if (requestCode == PICK_TXT) carregar(data.getData());
        if (requestCode == SAVE_PDF) salvarPdf(data.getData());
    }

    void carregar(Uri uri) {
        bloquear(true);
        status.setText("Lendo arquivo...");

        executor.execute(() -> {
            try {
                textoHistorico = lerTexto(uri);

                runOnUiThread(() -> {
                    bloquear(false);
                    gerar.setEnabled(true);
                    progresso.setProgress(100);
                    status.setText("Histórico carregado. Motor Python pronto.");
                    saida.setText("Agora clique em GERAR O MESMO JOGO DO PYDROID.");
                });

            } catch (Throwable t) {
                runOnUiThread(() -> erro(t));
            }
        });
    }

    String lerTexto(Uri uri) throws Exception {
        try (
            InputStream in = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            byte[] buf = new byte[8192];
            int n;

            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }

            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    void gerar() {
        if (textoHistorico == null) return;

        bloquear(true);
        progresso.setProgress(5);
        status.setText("Executando o mesmo motor Python do Pydroid...");
        saida.setText("");

        executor.execute(() -> {
            try {
                Python py = Python.getInstance();
                PyObject modulo = py.getModule("motor_lotomania");

                runOnUiThread(() -> {
                    progresso.setProgress(15);
                    status.setText("Motor Python: Talo + repetidas + distribuição 10×10...");
                });

                String json = modulo.callAttr("executar_texto", textoHistorico).toString();

                Resultado r = Resultado.fromJson(json);
                resultado = r;

                runOnUiThread(() -> {
                    bloquear(false);
                    pdf.setEnabled(true);
                    progresso.setProgress(100);
                    status.setText("Concluído pelo motor Python.");
                    saida.setText(r.resumo());
                });

            } catch (Throwable t) {
                runOnUiThread(() -> erro(t));
            }
        });
    }

    void salvarPdf(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {

            PdfDocument doc = new PdfDocument();

            PdfDocument.Page page =
                doc.startPage(
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create()
                );

            Canvas c = page.getCanvas();
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            p.setColor(Color.rgb(244, 123, 32));
            p.setTextSize(21);
            p.setFakeBoldText(true);

            c.drawText(
                "LOTOMANIA — ENGROSSANDO O TALO V4",
                28,
                36,
                p
            );

            p.setTextSize(9);
            p.setFakeBoldText(false);
            p.setColor(Color.DKGRAY);

            c.drawText(
                "VERDE = JOGO DO PYDROID | LARANJA = FORA | * = REPETIDA",
                28,
                54,
                p
            );

            HashSet<Integer> jogoSet = new HashSet<>();
            HashSet<Integer> repSet = new HashSet<>();

            for (int n : resultado.jogo) jogoSet.add(n);
            for (int n : resultado.repetidas) repSet.add(n);

            int sx = 47;
            int sy = 91;
            int dx = 51;
            int dy = 35;
            float raio = 14;

            for (int n = 1; n <= 100; n++) {
                int idx = n - 1;
                int row = idx / 10;
                int col = idx % 10;

                float x = sx + col * dx;
                float y = sy + row * dy;

                boolean escolhido = jogoSet.contains(n);

                p.setColor(
                    escolhido
                        ? Color.rgb(50, 150, 79)
                        : Color.rgb(239, 169, 76)
                );

                c.drawCircle(x, y, raio, p);

                p.setColor(
                    escolhido
                        ? Color.WHITE
                        : Color.rgb(90, 50, 20)
                );

                p.setTextSize(8.2f);
                p.setFakeBoldText(true);

                String txt = Resultado.fmt(n);

                if (repSet.contains(n)) {
                    txt += "*";
                }

                c.drawText(
                    txt,
                    x - p.measureText(txt) / 2,
                    y + 3,
                    p
                );
            }

            p.setFakeBoldText(false);
            p.setColor(Color.BLACK);
            p.setTextSize(8.4f);

            int y = 475;

            String[] linhas = resultado.resumo().split("\n");

            for (String linha : linhas) {
                if (y > 817) break;
                c.drawText(linha, 28, y, p);
                y += 11;
            }

            doc.finishPage(page);
            doc.writeTo(os);
            doc.close();

            status.setText("PDF gerado com o jogo calculado pelo Python.");

        } catch (Throwable t) {
            erro(t);
        }
    }

    void bloquear(boolean sim) {
        importar.setEnabled(!sim);
        gerar.setEnabled(!sim && textoHistorico != null);

        if (sim) {
            pdf.setEnabled(false);
            progresso.setProgress(0);
        }
    }

    void erro(Throwable t) {
        bloquear(false);
        status.setText("Erro: " + t.getMessage());

        new AlertDialog.Builder(this)
            .setTitle("Erro")
            .setMessage(String.valueOf(t))
            .setPositiveButton("OK", null)
            .show();
    }
}

class Resultado {

    int ultimoConcurso;

    int[] ultimoResultado;
    int[] jogo;
    int[] repetidas;
    int[] novas;
    int[] espelho;

    double scoreFinal;
    double scoreTalo;
    double scoreDistribuicao;
    double mediaForca;
    double slope18;
    double slope6;
    double crescimento;

    int[] serie18;
    int[] serie6;
    int[] linhas;
    int[] colunas;

    int pares;
    int impares;
    int baixas;
    int altas;

    String motor;

    static Resultado fromJson(String texto) throws Exception {
        JSONObject o = new JSONObject(texto);

        Resultado r = new Resultado();

        r.ultimoConcurso = o.getInt("ultimo_concurso");

        r.ultimoResultado = ints(o.getJSONArray("ultimo_resultado"));
        r.jogo = ints(o.getJSONArray("jogo"));
        r.repetidas = ints(o.getJSONArray("repetidas"));
        r.novas = ints(o.getJSONArray("novas"));
        r.espelho = ints(o.getJSONArray("espelho"));

        r.scoreFinal = o.getDouble("score_final");
        r.scoreTalo = o.getDouble("score_talo");
        r.scoreDistribuicao = o.getDouble("score_distribuicao");
        r.mediaForca = o.getDouble("media_forca");

        r.serie18 = ints(o.getJSONArray("serie18"));
        r.serie6 = ints(o.getJSONArray("serie6"));

        r.slope18 = o.getDouble("slope18");
        r.slope6 = o.getDouble("slope6");
        r.crescimento = o.getDouble("crescimento");

        r.linhas = ints(o.getJSONArray("linhas"));
        r.colunas = ints(o.getJSONArray("colunas"));

        r.pares = o.getInt("pares");
        r.impares = o.getInt("impares");
        r.baixas = o.getInt("baixas");
        r.altas = o.getInt("altas");

        r.motor = o.getString("motor");

        return r;
    }

    static int[] ints(JSONArray a) throws Exception {
        int[] r = new int[a.length()];

        for (int i = 0; i < a.length(); i++) {
            r[i] = a.getInt(i);
        }

        return r;
    }

    static String fmt(int n) {
        return n == 100
            ? "00"
            : String.format(Locale.US, "%02d", n);
    }

    static String grupo(int[] a) {
        int[] b = a.clone();
        Arrays.sort(b);

        StringBuilder s = new StringBuilder();

        for (int n : b) {
            if (s.length() > 0) s.append(" ");
            s.append(fmt(n));
        }

        return s.toString();
    }

    String resumo() {
        StringBuilder s = new StringBuilder();

        s.append("MOTOR: PYTHON DO PYDROID V3\n");
        s.append("Último concurso: ").append(ultimoConcurso).append("\n");
        s.append("Último resultado:\n").append(grupo(ultimoResultado)).append("\n\n");

        s.append("MELHOR JOGO DE 50 — COLCHETES DO PYDROID:\n");
        s.append(grupo(jogo)).append("\n\n");

        s.append("Repetidas do último: ").append(repetidas.length).append("\n");
        s.append(grupo(repetidas)).append("\n\n");

        s.append("Não repetidas: ").append(novas.length).append("\n");
        s.append(grupo(novas)).append("\n\n");

        s.append("Score final: ").append(String.format(Locale.US, "%.2f", scoreFinal)).append("\n");
        s.append("Score talo: ").append(String.format(Locale.US, "%.2f", scoreTalo)).append("\n");
        s.append("Score distribuição: ").append(String.format(Locale.US, "%.2f", scoreDistribuicao)).append("\n");
        s.append("Força média: ").append(String.format(Locale.US, "%.2f", mediaForca)).append("\n");

        s.append("Série18: ").append(Arrays.toString(serie18)).append("\n");
        s.append("Últimos6: ").append(Arrays.toString(serie6)).append("\n");
        s.append("Slope18: ").append(String.format(Locale.US, "%+.4f", slope18)).append("\n");
        s.append("Slope6: ").append(String.format(Locale.US, "%+.4f", slope6)).append("\n");
        s.append("Crescimento: ").append(String.format(Locale.US, "%+.3f", crescimento)).append("\n\n");

        s.append("CARTELA — [XX] = JOGO\n");

        HashSet<Integer> set = new HashSet<>();
        HashSet<Integer> rep = new HashSet<>();

        for (int n : jogo) set.add(n);
        for (int n : repetidas) rep.add(n);

        for (int lin = 0; lin < 10; lin++) {
            for (int col = 0; col < 10; col++) {
                int n = lin * 10 + col + 1;
                String t = fmt(n);

                if (set.contains(n)) {
                    if (rep.contains(n)) {
                        s.append("[").append(t).append("*]");
                    } else {
                        s.append("[").append(t).append("] ");
                    }
                } else {
                    s.append(" ").append(t).append("  ");
                }

                if (col < 9) s.append(" ");
            }

            s.append("\n");
        }

        s.append("\nLinhas: ").append(Arrays.toString(linhas)).append("\n");
        s.append("Colunas: ").append(Arrays.toString(colunas)).append("\n");
        s.append("Pares: ").append(pares).append(" | Ímpares: ").append(impares).append("\n");
        s.append("01-50: ").append(baixas).append(" | 51-00: ").append(altas).append("\n");

        return s.toString();
    }
}

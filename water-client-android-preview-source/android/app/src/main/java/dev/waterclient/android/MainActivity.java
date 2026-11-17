package dev.waterclient.android;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Primeira base Android: navegação landscape e perfis Offline persistentes. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 15, 25);
    private static final int PANEL = Color.rgb(17, 29, 43);
    private static final int PANEL_2 = Color.rgb(25, 41, 57);
    private static final int AQUA = Color.rgb(64, 221, 232);
    private static final int TEXT = Color.rgb(238, 247, 250);
    private static final int MUTED = Color.rgb(160, 180, 192);
    private static final String PREFS = "water-client";
    private final List<String> pages = java.util.Arrays.asList("Início", "Versões", "Mods", "Servidores", "Downloads", "Contas", "Configurações");
    private LinearLayout root;
    private LinearLayout content;
    private String activePage = "Início";
    private String activeProfile = "";
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private int renderGeneration;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        activeProfile = getPreferences(MODE_PRIVATE).getString("active-profile", "");
        List<String> savedProfiles = readProfiles();
        if (activeProfile.trim().isEmpty() && !savedProfiles.isEmpty()) {
            activeProfile = savedProfiles.get(0);
            getPreferences(MODE_PRIVATE).edit().putString("active-profile", activeProfile).apply();
        }
        activePage = savedProfiles.isEmpty() ? "Boas-vindas" : "Início";
        render();
    }

    private void render() {
        renderGeneration++;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(18) + insets.getSystemWindowInsetLeft(),
                    dp(12) + insets.getSystemWindowInsetTop(),
                    dp(18) + insets.getSystemWindowInsetRight(),
                    dp(12) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("WATER  CLIENT", 20, AQUA, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView profile = label(activeProfile.trim().isEmpty() ? "SEM PERFIL" : activeProfile + "  ·  OFFLINE", 13, TEXT, true);
        profile.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        header.addView(profile, new LinearLayout.LayoutParams(-2, dp(44)));
        root.addView(header);
        root.addView(divider());

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, 0, 1);
        bodyParams.topMargin = dp(12);
        root.addView(body, bodyParams);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setPadding(0, 0, dp(12), 0);
        body.addView(nav, new LinearLayout.LayoutParams(dp(142), -1));
        nav.setVisibility("Boas-vindas".equals(activePage) ? View.GONE : View.VISIBLE);
        for (String page : pages) {
            Button item = button(page, page.equals(activePage));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 0, 1f);
            p.bottomMargin = dp(3);
            nav.addView(item, p);
            item.setOnClickListener(v -> { activePage = page; render(); });
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        body.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG);
        content.setPadding(dp(8), dp(5), dp(4), dp(8));
        scroll.addView(content, new android.widget.FrameLayout.LayoutParams(-1, -2));
        root.addView(label("O client mais limpo para celulares.", 11, MUTED, false));
        setContentView(root);
        animateEntry(root);
        showPage();
    }

    private void showPage() {
        content.removeAllViews();
        switch (activePage) {
            case "Boas-vindas" -> welcomePage();
            case "Início" -> homePage();
            case "Contas" -> accountsPage();
            case "Configurações" -> settingsPage();
            case "Mods" -> modsPage();
            case "Servidores" -> serversPage();
            case "Versões" -> infoPage("Versões e instâncias", "A instalação de Vanilla, Fabric, Forge, NeoForge e Quilt depende do runtime Android. Compatibilidade deve ser validada por versão.");
            case "Downloads" -> infoPage("Downloads", "Não há downloads ativos. O app não baixa runtime nem arquivos de jogo nesta versão.");
            default -> infoPage("WATER CLIENT", "O client mais limpo para celulares.");
        }
    }

    private void modsPage() {
        final int requestGeneration = renderGeneration;
        addText("Biblioteca de mods", 26, TEXT, true);
        addText("Busca real no catálogo público Modrinth. A instalação exige runtime e instância configurados.", 13, MUTED, false);
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Pesquisar no Modrinth");
        searchRow.addView(query, new LinearLayout.LayoutParams(0, dp(52), 1f));
        Button search = button("Pesquisar", true);
        searchRow.addView(search, new LinearLayout.LayoutParams(dp(130), dp(52)));
        content.addView(searchRow);
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results, new LinearLayout.LayoutParams(-1, -2));
        TextView status = label("Pesquise um projeto para consultar a API oficial.", 12, MUTED, false);
        results.addView(status);
        search.setOnClickListener(v -> {
            String term = query.getText().toString().trim();
            if (term.trim().isEmpty()) { status.setText("Digite o nome de um mod."); return; }
            search.setEnabled(false);
            status.setText("Buscando no Modrinth…");
            network.execute(() -> {
                try {
                    List<ModrinthApi.Project> found = ModrinthApi.searchMods(term);
                    runOnUiThread(() -> {
                        if (isFinishing() || !"Mods".equals(activePage) || requestGeneration != renderGeneration) return;
                        results.removeAllViews();
                        if (found.isEmpty()) results.addView(label("Nenhum mod encontrado.", 13, MUTED, false));
                        for (ModrinthApi.Project project : found) {
                            LinearLayout item = card();
                            item.addView(label(project.title, 15, TEXT, true));
                            item.addView(label(project.description, 12, MUTED, false));
                            item.addView(label(String.format(java.util.Locale.US, "%,d downloads · %s", project.downloads, project.slug), 10, AQUA, false));
                            Button open = button("Abrir página do projeto", false);
                            open.setOnClickListener(x -> startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://modrinth.com/mod/" + Uri.encode(project.slug)))));
                            item.addView(open);
                            addCardTo(results, item);
                        }
                        search.setEnabled(true);
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        if (isFinishing() || !"Mods".equals(activePage) || requestGeneration != renderGeneration) return;
                        results.removeAllViews();
                        results.addView(label("Falha ao consultar Modrinth: " + error.getMessage(), 12, MUTED, false));
                        search.setEnabled(true);
                    });
                }
            });
        });
    }

    private void serversPage() {
        addText("Servidores", 26, TEXT, true);
        addText("Endereços são salvos localmente. A conexão de jogo ainda depende do runtime.", 13, MUTED, false);
        Button add = button("＋  Adicionar servidor", true);
        add.setOnClickListener(v -> addServerDialog());
        content.addView(add);
        JSONArray servers = getServerList();
        for (int i = 0; i < servers.length(); i++) {
            JSONObject server = servers.optJSONObject(i);
            if (server == null) continue;
            LinearLayout row = card();
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.addView(label(server.optString("name"), 14, TEXT, true));
            details.addView(label(server.optString("address"), 11, MUTED, false));
            row.addView(details, new LinearLayout.LayoutParams(0, dp(48), 1));
            Button edit = button("Editar", false);
            final int index = i;
            edit.setOnClickListener(v -> editServerDialog(index, server));
            row.addView(edit);
            Button remove = button("Excluir", false);
            remove.setOnClickListener(v -> {
                JSONArray current = getServerList();
                JSONArray updated = new JSONArray();
                for (int j = 0; j < current.length(); j++) if (j != index) updated.put(current.opt(j));
                saveServerList(updated);
                render();
            });
            row.addView(remove);
            addCard(row);
        }
        addText("Perfis Offline só conectam a servidores que aceitam autenticação offline. Nenhuma exigência de autenticação é contornada.", 11, MUTED, false);
    }

    private JSONArray getServerList() {
        try { return new JSONArray(getPreferences(MODE_PRIVATE).getString("servers", "[]")); }
        catch (JSONException ignored) { return new JSONArray(); }
    }

    private void saveServerList(JSONArray servers) {
        getPreferences(MODE_PRIVATE).edit().putString("servers", servers.toString()).apply();
    }

    private void addServerDialog() {
        editServerDialog(-1, null);
    }

    private void editServerDialog(int index, JSONObject existing) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("Nome personalizado");
        if (existing != null) name.setText(existing.optString("name"));
        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("endereco.exemplo:25565");
        if (existing != null) address.setText(existing.optString("address"));
        fields.addView(name);
        fields.addView(address);
        new AlertDialog.Builder(this).setTitle("Adicionar servidor").setView(fields)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String serverName = name.getText().toString().trim();
                    String serverAddress = address.getText().toString().trim();
                    if (serverName.trim().isEmpty() || serverAddress.trim().isEmpty()) {
                        Toast.makeText(this, "Informe nome e endereço.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    JSONArray servers = getServerList();
                    JSONObject server = new JSONObject();
                    try {
                        server.put("name", serverName);
                        server.put("address", serverAddress);
                        if (index >= 0 && index < servers.length()) servers.put(index, server);
                        else servers.put(server);
                        saveServerList(servers);
                        render();
                    } catch (JSONException ignored) { }
                }).setNegativeButton("Cancelar", null).show();
    }

    private void welcomePage() {
        addText("WATER CLIENT", 13, AQUA, true);
        addText("O client mais limpo para celulares.", 27, TEXT, true);
        addText("Escolha como quer entrar. Esta primeira versão cria perfis locais; o runtime Java e o OAuth oficial ainda estão em integração.", 14, MUTED, false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(row, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout premiumCard = card();
        premiumCard.addView(label("✦  MINECRAFT JAVA ORIGINAL", 15, AQUA, true));
        premiumCard.addView(label("Conta Microsoft que possui o jogo", 12, TEXT, true));
        premiumCard.addView(label("OAuth ainda não configurado; nenhum login é simulado.", 11, MUTED, false));
        Button premiumButton = button("Entrar com Microsoft", false);
        premiumButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Login Microsoft não configurado")
                .setMessage("A autenticação oficial exige registrar o aplicativo e configurar o fluxo OAuth. Esta versão não coleta credenciais.")
                .setPositiveButton("Entendi", null).show());
        premiumCard.addView(premiumButton);
        LinearLayout offlineCard = card();
        offlineCard.addView(label("◉  CONTA OFFLINE", 15, AQUA, true));
        offlineCard.addView(label("Um perfil local sem senha", 12, TEXT, true));
        offlineCard.addView(label("Só conecta a servidores que aceitam autenticação offline.", 11, MUTED, false));
        Button offlineButton = button("Criar conta Offline", true);
        offlineButton.setOnClickListener(v -> promptOffline());
        offlineCard.addView(offlineButton);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1f);
        p1.rightMargin = dp(8);
        row.addView(premiumCard, p1);
        row.addView(offlineCard, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private void homePage() {
        addText("BEM-VINDO AO WATER CLIENT", 12, MUTED, true);
        addText("Pronto para jogar?", 27, TEXT, true);
        LinearLayout card = card();
        card.setBackgroundColor(Color.rgb(22, 52, 59));
        card.addView(label("PERFIL ATIVO  ·  " + (activeProfile.trim().isEmpty() ? "nenhum" : activeProfile), 12, AQUA, true));
        card.addView(label("Minecraft Java Edition", 20, TEXT, true));
        card.addView(label("Runtime Android ainda não integrado", 13, MUTED, false));
        Button play = button("JOGAR  ·  INDISPONÍVEL NESTA VERSÃO", false);
        play.setEnabled(false);
        card.addView(play);
        card.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Execução ainda não disponível")
                .setMessage("Este protótipo ainda não inclui runtime Java para Android, LWJGL nem renderer. Ele não pode iniciar Minecraft.")
                .setPositiveButton("Entendi", null).show());
        addCard(card);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addSummary(row, "MODS", "Integração pendente");
        addSummary(row, "INSTÂNCIAS", "Runtime pendente");
        content.addView(row);
    }

    private void accountsPage() {
        addText("Contas", 26, TEXT, true);
        addText("Perfis Offline ficam salvos neste aparelho. Eles não são identidades Microsoft verificadas.", 13, MUTED, false);
        Button add = button("＋  Criar perfil Offline", true);
        add.setOnClickListener(v -> promptOffline());
        content.addView(add);
        for (String name : readProfiles()) {
            LinearLayout row = card();
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView item = label(name + (name.equals(activeProfile) ? "  ·  ATIVO" : "  ·  OFFLINE"), 15, TEXT, true);
            row.addView(item, new LinearLayout.LayoutParams(0, dp(44), 1));
            Button select = button(name.equals(activeProfile) ? "Selecionado" : "Usar", name.equals(activeProfile));
            select.setOnClickListener(v -> selectProfile(name));
            row.addView(select);
            Button delete = button("Excluir", false);
            delete.setOnClickListener(v -> removeProfile(name));
            row.addView(delete);
            addCard(row);
        }
        Button microsoft = button("Adicionar conta Microsoft", false);
        microsoft.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Login Microsoft não configurado")
                .setMessage("A autenticação oficial exige registrar o aplicativo e configurar o fluxo OAuth. Nenhuma senha será solicitada nem simulada por esta versão.")
                .setPositiveButton("Fechar", null).show());
        content.addView(microsoft);
    }

    private void settingsPage() {
        addText("Configurações", 26, TEXT, true);
        addText("Animações", 15, AQUA, true);
        String[] modes = {"Completa", "Reduzida", "Mínima"};
        for (String mode : modes) {
            Button choice = button(mode + (mode.equals(currentMotion()) ? "  ✓" : ""), mode.equals(currentMotion()));
            choice.setOnClickListener(v -> {
                getPreferences(MODE_PRIVATE).edit().putString("motion", mode).apply();
                render();
            });
            content.addView(choice);
        }
        boolean reduced = android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled();
        addText(reduced ? "Animações limitadas pela preferência de movimento do Android." : "As transições respeitam a opção de animação escolhida.", 13, MUTED, false);
        addText("Memória, renderer e gráficos serão configuráveis quando o runtime de jogo estiver integrado.", 13, MUTED, false);
    }

    private void infoPage(String title, String message) {
        addText(title, 26, TEXT, true);
        LinearLayout panel = card();
        panel.addView(label(message, 15, MUTED, false));
        addCard(panel);
    }

    private void promptOffline() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("3–16 letras, números ou _");
        new AlertDialog.Builder(this).setTitle("Novo perfil Offline").setView(input)
                .setMessage("Sem senha. Esse nome não é uma identidade Microsoft verificada.")
                .setPositiveButton("Criar", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.matches("[A-Za-z0-9_]{3,16}")) {
                        Toast.makeText(this, "Use 3–16 letras, números ou _", Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<String> profiles = readProfiles();
                    if (!profiles.contains(name)) profiles.add(name);
                    writeProfiles(profiles);
                    selectProfile(name);
                }).setNegativeButton("Cancelar", null).show();
    }

    private List<String> readProfiles() {
        List<String> profiles = new ArrayList<>();
        String raw = getPreferences(MODE_PRIVATE).getString("offline-profiles", "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) profiles.add(array.getString(i));
        } catch (JSONException ignored) { }
        return profiles;
    }

    private void writeProfiles(List<String> profiles) {
        getPreferences(MODE_PRIVATE).edit().putString("offline-profiles", new JSONArray(profiles).toString()).apply();
    }

    private void selectProfile(String name) {
        activeProfile = name;
        getPreferences(MODE_PRIVATE).edit().putString("active-profile", name).apply();
        activePage = "Início";
        render();
    }

    private void removeProfile(String name) {
        List<String> profiles = readProfiles();
        profiles.remove(name);
        writeProfiles(profiles);
        if (name.equals(activeProfile)) activeProfile = profiles.isEmpty() ? "" : profiles.get(0);
        getPreferences(MODE_PRIVATE).edit().putString("active-profile", activeProfile).apply();
        render();
    }

    private String currentMotion() {
        String saved = getPreferences(MODE_PRIVATE).getString("motion", null);
        if (saved != null) return saved;
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean reducedBySystem = android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled();
        return reducedBySystem || (manager != null && manager.isLowRamDevice()) ? "Mínima" : "Reduzida";
    }

    private void animateEntry(View view) {
        if ("Mínima".equals(currentMotion())) return;
        if (android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) return;
        view.setAlpha(0f);
        view.setTranslationX(dp(8));
        view.animate().alpha(1f).translationX(0f)
                .setDuration("Completa".equals(currentMotion()) ? 240 : 150)
                .start();
    }

    private void addText(String text, int size, int color, boolean bold) {
        TextView view = label(text, size, color, bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(9);
        content.addView(view, p);
    }

    private void addSummary(LinearLayout row, String title, String value) {
        LinearLayout panel = card();
        panel.addView(label(title, 12, AQUA, true));
        panel.addView(label(value, 13, MUTED, false));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.rightMargin = dp(8);
        row.addView(panel, p);
    }

    private void addCard(View view) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(10);
        content.addView(view, p);
    }

    private void addCardTo(LinearLayout parent, View view) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(9);
        parent.addView(view, p);
    }

    private LinearLayout card() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable();
        background.setColor(PANEL);
        background.setCornerRadius(dp(12));
        panel.setBackground(background);
        panel.setPadding(dp(16), dp(13), dp(16), dp(13));
        return panel;
    }

    private Button button(String text, boolean selected) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(selected ? Color.rgb(5, 30, 37) : TEXT);
        b.setTextSize(13);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(5), dp(3), dp(5), dp(3));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected ? AQUA : PANEL_2));
        return b;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.rgb(39, 58, 73));
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        network.shutdownNow();
        super.onDestroy();
    }
}

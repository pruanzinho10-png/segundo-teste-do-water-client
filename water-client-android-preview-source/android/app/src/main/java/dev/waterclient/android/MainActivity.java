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
import android.util.Log;
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
    private static final String TAG = "WaterClientDiag";
    private static final String DIAGNOSTIC_BUILD = "mod-installer-feature-1";
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
    private ScrollView pageScroll;
    private String activePage = "Início";
    private String activeProfile = "";
    private String selectedGameVersion = "1.21.1";
    private String selectedModLoader = "fabric";
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private int renderGeneration;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        activeProfile = getPreferences(MODE_PRIVATE).getString("active-profile", "");
        selectedGameVersion = getPreferences(MODE_PRIVATE).getString("mod-game-version", "1.21.1");
        selectedModLoader = getPreferences(MODE_PRIVATE).getString("mod-loader", "fabric");
        List<String> savedProfiles = readProfiles();
        if (activeProfile.trim().isEmpty() && !savedProfiles.isEmpty()) {
            activeProfile = savedProfiles.get(0);
            getPreferences(MODE_PRIVATE).edit().putString("active-profile", activeProfile).apply();
        }
        activePage = savedProfiles.isEmpty() ? "Boas-vindas" : "Início";
        Log.i(TAG, "DIAG " + DIAGNOSTIC_BUILD + " onCreate activePage=" + activePage
                + " activeProfileEmpty=" + activeProfile.trim().isEmpty()
                + " savedProfiles=" + savedProfiles.size());
        render();
    }

    private void render() {
        renderGeneration++;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int maxInset = dp(64);
            int rawLeft = insets.getSystemWindowInsetLeft();
            int rawTop = insets.getSystemWindowInsetTop();
            int rawRight = insets.getSystemWindowInsetRight();
            int rawBottom = insets.getSystemWindowInsetBottom();
            int left = Math.min(Math.max(rawLeft, 0), maxInset);
            int top = Math.min(Math.max(rawTop, 0), maxInset);
            int right = Math.min(Math.max(rawRight, 0), maxInset);
            int bottom = Math.min(Math.max(rawBottom, 0), maxInset);
            Log.i(TAG, "DIAG windowInsets raw=" + rawLeft + ',' + rawTop + ',' + rawRight + ',' + rawBottom
                    + " applied=" + left + ',' + top + ',' + right + ',' + bottom + " cap=" + maxInset);
            view.setPadding(dp(18) + left, dp(12) + top, dp(18) + right, dp(12) + bottom);
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
        View separator = divider();
        root.addView(separator, new LinearLayout.LayoutParams(-1, dp(1)));

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
            if ("Configurações".equals(page)) continue;
            Button item = button(navIcon(page) + "  " + page, page.equals(activePage));
            item.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            item.setPadding(dp(12), 0, dp(8), 0);
            item.setTextSize(12);
            item.setTextColor(page.equals(activePage) ? AQUA : TEXT);
            item.setBackgroundTintList(android.content.res.ColorStateList.valueOf(page.equals(activePage) ? Color.rgb(23, 60, 67) : PANEL_2));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(42));
            p.bottomMargin = dp(5);
            nav.addView(item, p);
            item.setOnClickListener(v -> { activePage = page; render(); });
        }
        View navSpacer = new View(this);
        nav.addView(navSpacer, new LinearLayout.LayoutParams(1, 0, 1f));
        Button settingsItem = button("⚙  Configurações", "Configurações".equals(activePage));
        settingsItem.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        settingsItem.setPadding(dp(12), 0, dp(8), 0);
        settingsItem.setTextSize(12);
        settingsItem.setTextColor("Configurações".equals(activePage) ? AQUA : TEXT);
        settingsItem.setBackgroundTintList(android.content.res.ColorStateList.valueOf("Configurações".equals(activePage) ? Color.rgb(23, 60, 67) : PANEL_2));
        nav.addView(settingsItem, new LinearLayout.LayoutParams(-1, dp(42)));
        settingsItem.setOnClickListener(v -> { activePage = "Configurações"; render(); });

        ScrollView scroll = new ScrollView(this);
        pageScroll = scroll;
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        body.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG);
        content.setPadding(dp(8), dp(5), dp(4), dp(8));
        scroll.addView(content, new android.widget.FrameLayout.LayoutParams(-1, -2));
        TextView footer = label("O client mais limpo para celulares.", 11, MUTED, false);
        root.addView(footer);
        setContentView(root);
        animateEntry(root);
        root.post(() -> ensureBodyHeight(root, header, separator, body, footer, bodyParams));
        showPage();
    }

    private void ensureBodyHeight(LinearLayout root, View header, View separator, LinearLayout body,
                                  View footer, LinearLayout.LayoutParams bodyParams) {
        int available = root.getHeight() - root.getPaddingTop() - root.getPaddingBottom()
                - header.getHeight() - separator.getHeight() - footer.getHeight() - bodyParams.topMargin;
        Log.i(TAG, "DIAG containers build=" + DIAGNOSTIC_BUILD
                + " root=" + root.getWidth() + "x" + root.getHeight()
                + " padding=" + root.getPaddingLeft() + ',' + root.getPaddingTop() + ','
                + root.getPaddingRight() + ',' + root.getPaddingBottom()
                + " header=" + header.getHeight() + " separator=" + separator.getHeight()
                + " footer=" + footer.getHeight() + " topMargin=" + bodyParams.topMargin
                + " body=" + body.getWidth() + "x" + body.getHeight()
                + " availableBodyHeight=" + available);
        if (body.getHeight() <= 0 && available > 0) {
            bodyParams.height = available;
            bodyParams.weight = 0f;
            body.setLayoutParams(bodyParams);
            Log.w(TAG, "DIAG appliedBodyHeightFallback=" + available);
        }
    }

    private void showPage() {
        content.removeAllViews();
        Log.i(TAG, "DIAG showPage begin generation=" + renderGeneration + " page=" + activePage
                + " contentParent=" + (content.getParent() == null ? "null" : content.getParent().getClass().getSimpleName()));
        try {
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
        } catch (RuntimeException error) {
            Log.e(TAG, "DIAG showPage failed generation=" + renderGeneration + " page=" + activePage, error);
            throw error;
        }
        logContentState("after-page-build");
        content.postDelayed(() -> logContentState("after-layout-stable"), 120);
    }

    private void logContentState(String stage) {
        String scrollParent = pageScroll == null || pageScroll.getParent() == null
                ? "null" : pageScroll.getParent().getClass().getSimpleName();
        String contentParent = content == null || content.getParent() == null
                ? "null" : content.getParent().getClass().getSimpleName();
        StringBuilder childSummary = new StringBuilder();
        if (content != null) {
            for (int i = 0; i < content.getChildCount(); i++) {
                View child = content.getChildAt(i);
                if (i > 0) childSummary.append(" | ");
                childSummary.append(i).append(':').append(child.getClass().getSimpleName())
                        .append(" vis=").append(child.getVisibility())
                        .append(" bounds=").append(child.getLeft()).append(',').append(child.getTop())
                        .append('-').append(child.getRight()).append(',').append(child.getBottom());
                if (child instanceof TextView textView) {
                    String text = textView.getText().toString().replace('\n', ' ');
                    if (text.length() > 70) text = text.substring(0, 70);
                    childSummary.append(" text=\"").append(text).append('"');
                }
            }
        }
        Log.i(TAG, "DIAG " + stage + " build=" + DIAGNOSTIC_BUILD
                + " page=" + activePage
                + " generation=" + renderGeneration
                + " children=" + (content == null ? -1 : content.getChildCount())
                + " contentVisibility=" + (content == null ? -1 : content.getVisibility())
                + " contentSize=" + (content == null ? "null" : content.getWidth() + "x" + content.getHeight())
                + " scrollSize=" + (pageScroll == null ? "null" : pageScroll.getWidth() + "x" + pageScroll.getHeight())
                + " scrollVisibility=" + (pageScroll == null ? -1 : pageScroll.getVisibility())
                + " contentParent=" + contentParent + " scrollParent=" + scrollParent
                + " childSummary=[" + childSummary + ']');
    }

    private void modsPage() {
        final int requestGeneration = renderGeneration;
        addText("Biblioteca de mods", 26, TEXT, true);
        addText("Pesquise no Modrinth e instale o JAR na instância padrão do Water Client.", 13, MUTED, false);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        Button versionFilter = button("Minecraft " + selectedGameVersion + "  ⌄", false);
        Button loaderFilter = button(loaderLabel(selectedModLoader) + "  ⌄", false);
        filters.addView(versionFilter, new LinearLayout.LayoutParams(dp(190), dp(44)));
        LinearLayout.LayoutParams loaderParams = new LinearLayout.LayoutParams(dp(160), dp(44));
        loaderParams.leftMargin = dp(8);
        filters.addView(loaderFilter, loaderParams);
        LinearLayout.LayoutParams filtersParams = new LinearLayout.LayoutParams(-1, -2);
        filtersParams.bottomMargin = dp(8);
        content.addView(filters, filtersParams);
        versionFilter.setOnClickListener(v -> chooseModTarget(true));
        loaderFilter.setOnClickListener(v -> chooseModTarget(false));

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
        TextView status = label("Os mods serão guardados na instância escolhida.", 12, MUTED, false);
        results.addView(status);
        try {
            addText("Destino: " + GameInstanceManager.getModsDirectory(this, selectedGameVersion, selectedModLoader).getAbsolutePath(), 10, MUTED, false);
        } catch (Exception error) {
            addText("Não foi possível preparar a pasta da instância: " + error.getMessage(), 10, MUTED, false);
        }
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
                        if (found.isEmpty()) {
                            status.setText("Nenhum mod encontrado para essa pesquisa.");
                            results.addView(status);
                        } else {
                            status.setText("Toque em Instalar para escolher a versão compatível e baixar o JAR.");
                            results.addView(status);
                        }
                        for (ModrinthApi.Project project : found) {
                            LinearLayout item = card();
                            item.addView(label(project.title, 15, TEXT, true));
                            TextView description = label(project.description, 12, MUTED, false);
                            description.setMaxLines(2);
                            item.addView(description);
                            item.addView(label(String.format(java.util.Locale.US, "%,d downloads · %s", project.downloads, project.slug), 10, AQUA, false));
                            Button install = button("Instalar", true);
                            install.setOnClickListener(x -> installMod(project, install, status, requestGeneration));
                            item.addView(install);
                            Button open = button("Ver no Modrinth", false);
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
                        status.setText("Falha ao consultar Modrinth: " + error.getMessage());
                        results.addView(status);
                        search.setEnabled(true);
                    });
                }
            });
        });
        addText("Compatibilidade é filtrada por versão do Minecraft e loader. Alguns mods também exigem dependências.", 11, MUTED, false);
    }

    private void chooseModTarget(boolean chooseVersion) {
        String[] choices = chooseVersion
                ? new String[]{"1.21.1", "1.20.1", "1.19.2"}
                : new String[]{"Fabric", "Forge", "NeoForge", "Quilt"};
        String current = chooseVersion ? selectedGameVersion : loaderLabel(selectedModLoader);
        int selected = 0;
        for (int i = 0; i < choices.length; i++) if (choices[i].equalsIgnoreCase(current)) selected = i;
        new AlertDialog.Builder(this)
                .setTitle(chooseVersion ? "Versão do Minecraft" : "Mod loader")
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    if (chooseVersion) selectedGameVersion = choices[which];
                    else selectedModLoader = choices[which].toLowerCase(java.util.Locale.ROOT);
                    getPreferences(MODE_PRIVATE).edit()
                            .putString("mod-game-version", selectedGameVersion)
                            .putString("mod-loader", selectedModLoader).apply();
                    dialog.dismiss();
                    activePage = "Mods";
                    render();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String loaderLabel(String loader) {
        if (loader == null || loader.isEmpty()) return "Fabric";
        return switch (loader.toLowerCase(java.util.Locale.ROOT)) {
            case "neoforge" -> "NeoForge";
            case "forge" -> "Forge";
            case "quilt" -> "Quilt";
            default -> "Fabric";
        };
    }

    private void installMod(ModrinthApi.Project project, Button install, TextView status, int requestGeneration) {
        final String targetVersion = selectedGameVersion;
        final String targetLoader = selectedModLoader;
        install.setEnabled(false);
        status.setText("Procurando uma versão compatível de " + project.title + "…");
        network.execute(() -> {
            try {
                ModrinthApi.ModFile mod = ModrinthApi.findCompatibleFile(project, targetVersion, targetLoader);
                if (mod == null) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && requestGeneration == renderGeneration) {
                            status.setText("Não há arquivo .jar para " + targetVersion + " com " + loaderLabel(targetLoader) + ".");
                            install.setEnabled(true);
                        }
                    });
                    return;
                }
                runOnUiThread(() -> {
                    if (!isFinishing() && requestGeneration == renderGeneration) {
                        status.setText("Baixando " + mod.filename + "…");
                    }
                });
                java.io.File modsDirectory = GameInstanceManager.getModsDirectory(this, targetVersion, targetLoader);
                java.io.File installed = ModInstaller.install(mod, modsDirectory);
                runOnUiThread(() -> {
                    if (!isFinishing() && requestGeneration == renderGeneration) {
                        status.setText("Instalado: " + installed.getName() + " · instância " + targetVersion + " / " + loaderLabel(targetLoader) + ".");
                        install.setText("Instalado");
                        install.setEnabled(true);
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!isFinishing() && requestGeneration == renderGeneration) {
                        status.setText("Falha ao instalar: " + error.getMessage());
                        install.setEnabled(true);
                    }
                });
            }
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
        Log.i(TAG, "DIAG welcomePage entered generation=" + renderGeneration);
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
        LinearLayout pageHeading = new LinearLayout(this);
        pageHeading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout headingCopy = new LinearLayout(this);
        headingCopy.setOrientation(LinearLayout.VERTICAL);
        headingCopy.addView(label("Pronto para jogar?", 25, TEXT, true));
        headingCopy.addView(label("Olá, " + activeProfile + ". Sua jornada começa aqui.", 12, MUTED, false));
        pageHeading.addView(headingCopy, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView offlineTag = label("PERFIL OFFLINE", 10, AQUA, true);
        offlineTag.setPadding(dp(10), dp(5), dp(10), dp(5));
        offlineTag.setBackground(rounded(Color.rgb(16, 53, 60), 24));
        pageHeading.addView(offlineTag);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.bottomMargin = dp(14);
        content.addView(pageHeading, headingParams);

        LinearLayout hero = card();
        hero.setBackground(rounded(Color.rgb(23, 62, 67), 12));
        hero.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView localStatus = label("●  PERFIL LOCAL", 10, Color.rgb(167, 241, 214), true);
        localStatus.setPadding(dp(9), dp(5), dp(9), dp(5));
        localStatus.setBackground(rounded(Color.rgb(22, 76, 67), 24));
        heroTop.addView(localStatus, new LinearLayout.LayoutParams(-2, -2));
        heroTop.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        heroTop.addView(label("Minecraft Java · protótipo", 11, TEXT, false));
        hero.addView(heroTop);
        LinearLayout.LayoutParams heroTitleParams = new LinearLayout.LayoutParams(-1, -2);
        heroTitleParams.topMargin = dp(10);
        hero.addView(label("WATER CLIENT", 20, TEXT, true), heroTitleParams);
        hero.addView(label("O client mais limpo para celulares.", 12, Color.rgb(189, 209, 211), false));

        LinearLayout launchRow = new LinearLayout(this);
        launchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams launchRowParams = new LinearLayout.LayoutParams(-1, -2);
        launchRowParams.topMargin = dp(12);
        hero.addView(launchRow, launchRowParams);
        Button play = button("▷  JOGAR", true);
        play.setOnClickListener(v -> Toast.makeText(this,
                "O runtime de Minecraft Java para Android ainda não está integrado.", Toast.LENGTH_LONG).show());
        launchRow.addView(play, new LinearLayout.LayoutParams(0, dp(42), 1f));
        Button version = button("Vanilla 1.21.1 ⌄", false);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(dp(150), dp(42));
        versionParams.leftMargin = dp(8);
        launchRow.addView(version, versionParams);
        version.setOnClickListener(v -> { activePage = "Versões"; render(); });

        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.bottomMargin = dp(10);
        content.addView(hero, heroParams);

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.addView(dashboardCard("✣", "Biblioteca de mods", "Explore projetos do Modrinth", "Mods"),
                new LinearLayout.LayoutParams(0, dp(72), 1f));
        LinearLayout.LayoutParams serverCardParams = new LinearLayout.LayoutParams(0, dp(72), 1f);
        serverCardParams.leftMargin = dp(8);
        shortcuts.addView(dashboardCard("▣", "Servidores", "Contas Premium e Offline", "Servidores"), serverCardParams);
        content.addView(shortcuts);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(dp(2), dp(11), 0, 0);
        statusRow.addView(label("●  Modo de demonstração", 10, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1f));
        statusRow.addView(label("Runtime de jogo não conectado", 10, MUTED, false));
        content.addView(statusRow);
    }

    private String navIcon(String page) {
        return switch (page) {
            case "Início" -> "⌂";
            case "Versões" -> "▤";
            case "Mods" -> "✣";
            case "Servidores" -> "▣";
            case "Downloads" -> "↓";
            case "Contas" -> "♙";
            default -> "•";
        };
    }

    private View dashboardCard(String icon, String title, String subtitle, String page) {
        LinearLayout panel = card();
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(12), dp(9), dp(10), dp(9));
        panel.setBackground(rounded(PANEL_2, 10));
        TextView iconView = label(icon, 22, AQUA, true);
        iconView.setGravity(Gravity.CENTER);
        panel.addView(iconView, new LinearLayout.LayoutParams(dp(31), -1));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -1, 1f);
        copyParams.leftMargin = dp(7);
        panel.addView(copy, copyParams);
        copy.addView(label(title, 13, TEXT, true));
        copy.addView(label(subtitle, 10, MUTED, false));
        panel.setOnClickListener(v -> { activePage = page; render(); });
        return panel;
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

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radiusDp));
        return background;
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


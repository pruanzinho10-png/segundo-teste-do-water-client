# WATER CLIENT

**O client mais limpo para celulares.**

Projeto Android em desenvolvimento para Minecraft: Java Edition, com interface horizontal e uma prévia web navegável em `preview/`.

## Estado honesto do projeto

- `android/`: aplicativo Android nativo com navegação horizontal, perfis Offline persistentes, servidores locais editáveis, busca pública do Modrinth e preferências de animação.
- `preview/`: demonstração visual interativa. Os dados, login, instalações e downloads mostrados nessa página são explicitamente simulados.
- A execução real de Minecraft Java ainda não está integrada. O aplicativo Android não finge iniciar o jogo; a ação de jogar explica que falta o runtime.
- Login Microsoft ainda depende de registrar o aplicativo e configurar o fluxo oficial de OAuth. Nenhuma chave ou token fictício está incluído.
- Modrinth: a busca no app consulta a API pública e abre os projetos oficiais; instalar arquivos em instâncias ainda depende do runtime.
- CurseForge exige chave autorizada e uma arquitetura segura de backend; não há chave inventada nem embutida.

## Prévia web

Abra `preview/index.html` diretamente no navegador. Os controles de navegação, conta Offline de demonstração, seleção de versão e configuração de animações funcionam no navegador, sem realizar autenticação ou instalar arquivos.

## Compilar o Android

Requisitos: JDK 17+, Android SDK Platform 36 e Android Build Tools. A pasta `android` contém um projeto Gradle Android. Em ambiente com Gradle 8.11.1 instalado:

```powershell
cd android
gradle assembleDebug
```

O APK de desenvolvimento será criado em `app/build/outputs/apk/debug/app-debug.apk`.

## Diagnóstico do MuMu

O build atual registra a página e as dimensões/filhos da interface na tag Logcat `WaterClientDiag`. Veja [docs/mumu-diagnostics.md](docs/mumu-diagnostics.md) para gerar o APK e capturar o log no MuMu.

## Por que o runtime é uma etapa separada

Executar Minecraft Java no Android exige mais que baixar versões: são necessários runtimes Java compatíveis com ARM, adaptações Android para LWJGL/GLFW, renderizadores nativos e tratamento de entradas. A análise técnica de referência e a licença estão em `docs/ENGINE-RESEARCH.md`.

## Estrutura

- `android/`: app Android e gerenciamento de perfis Offline no dispositivo.
- `preview/`: experiência conceitual navegável, marcada como demonstração.
- `docs/`: pesquisa de arquitetura e plano de integração do runtime.
- `.github/workflows/android.yml`: compilação de APK em GitHub Actions.


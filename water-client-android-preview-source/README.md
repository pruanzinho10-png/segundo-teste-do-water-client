# WATER CLIENT

**O client mais limpo para celulares.**

Launcher Android para Minecraft: Java Edition, com interface própria, instalação de mods pelo Modrinth e runtime Java Android integrado ao APK.

## O que já funciona

- A interface Android horizontal abre a configuração do runtime ao tocar em **Jogar**.
- O runtime Amethyst/Pojav cuida de baixar os arquivos oficiais do Minecraft, preparar Java/LWJGL, autenticar a conta e iniciar o jogo. Na primeira execução, escolha a versão e configure uma conta dentro do launcher.
- O perfil local selecionado no Water Client é sincronizado com o runtime depois que uma conta online licenciada já foi adicionada. O app mantém a verificação de licença do Amethyst; perfis locais não substituem uma conta que possui Minecraft Java.
- A aba **Créditos e licenças** identifica AngelAuraMC/Amethyst, PojavLauncher e Boardwalk, e oferece os links do código-fonte e da LGPL-3.0.
- A Biblioteca de mods consulta o Modrinth e instala o JAR verificado em `.minecraft/mods`, a pasta compartilhada com o runtime. Selecione aqui a mesma versão e loader que usar no runtime. Mods com dependências obrigatórias ainda precisam delas instaladas.
- O workflow busca o código do runtime em uma revisão fixa e prepara os componentes necessários durante a compilação; não precisa enviar o código nativo pesado dentro deste repositório.
- `preview/` é uma demonstração web; suas contas e downloads são simulados.

O perfil Offline criado na tela Water Client organiza essa interface; ele não autentica no Minecraft. A conta e a licença do jogo são verificadas pelo runtime oficial. Não há senha ou token fictício no app.

## Gerar o APK

Envie o repositório ao GitHub e abra **Actions → Build Water Client with Java runtime**. Baixe o artefato `water-client-android-debug` quando a execução terminar. Esse APK inclui o runtime Android. O build usa JDK 21, Android NDK e baixa os componentes Java necessários automaticamente.

O projeto Android pequeno em `android/` contém a interface Water e o instalador de mods. O workflow compõe esses arquivos com o motor Android externo fixado no script de build; por isso, `gradle assembleDebug` executado apenas em `android/` gera somente a interface e não é a forma de obter o APK com runtime.

## Prévia web

Abra `preview/index.html` no navegador. Ela representa a identidade e navegação do launcher, mas não baixa o jogo nem instala mods.

## Licenças

O runtime integrado é baseado no [AngelAuraMC/Amethyst-Android](https://github.com/AngelAuraMC/Amethyst-Android), sob LGPL-3.0, e inclui componentes com licenças próprias. Consulte `docs/THIRD-PARTY-LICENSES/AMETHYST-LICENSE.txt` e as licenças listadas no repositório upstream. A revisão de runtime usada no build está fixada em `scripts/prepare-runtime.py`/`.github/workflows/android.yml`.

## Pastas

- `android/`: interface Water Client e integração com Modrinth.
- `scripts/prepare-runtime.py`: aplica a interface e identidade Water sobre o runtime no build.
- `.github/workflows/android.yml`: monta e publica o APK de desenvolvimento.
- `preview/`: demonstração web.
- `docs/`: diagnóstico, pesquisa e avisos de licença.

# Runtime Android do Water Client

## Integração atual

O botão **Jogar** inicia a atividade de preparação do runtime Amethyst/Pojav no mesmo APK. Ele fornece a JVM móvel, a ponte JNI, LWJGL/GLFW, renderização, controles e o ciclo de vida do Minecraft; o runtime não usa o Java de sistema do Android.

O workflow `.github/workflows/android.yml` baixa uma revisão fixa do projeto [AngelAuraMC/Amethyst-Android](https://github.com/AngelAuraMC/Amethyst-Android), inicializa os submódulos, obtém o pacote JRE Android e executa `scripts/prepare-runtime.py`. O script copia a interface Water Client, define sua tela inicial e aplica o ícone/nome. O restante do runtime é mantido no upstream para evitar duplicar mais de 160 MB de código e recursos nativos no repositório Water.

Na primeira abertura do runtime, o usuário precisa escolher/instalar uma versão, configurar o carregador quando necessário e entrar com a conta Minecraft. O botão Water Client entrega o controle ao launcher integrado; a seleção real de versão, autenticação e configurações de renderização acontecem nele.

## Diretório de mods

Em Android 10+, o runtime resolve a pasta do jogo em `Android/data/<pacote instalado>/files/.minecraft`. `GameInstanceManager` usa esse mesmo diretório e instala os JARs em `.minecraft/mods`. O jogo só carregará os mods se a versão e o loader usados no runtime forem os mesmos escolhidos no filtro do Modrinth. O instalador valida origem, HTTPS, tamanho, cabeçalho JAR e SHA-1. Dependências do mod ainda precisam ser instaladas separadamente.

## Testes e limitações

Uma compilação bem-sucedida comprova que Java/runtime e binários nativos foram empacotados, mas não garante compatibilidade com toda GPU, versão ou modloader. O APK deve ser aberto no MuMu ou em um telefone para confirmar download inicial, autenticação, renderer e início de uma versão. Emuladores podem não reproduzir os mesmos renderizadores de aparelhos físicos.

O suporte de login Microsoft/Minecraft é fornecido pelo runtime upstream. O perfil Offline local do Water Client é apenas um perfil da interface e não substitui autenticação válida nem licença do jogo.

## Licenciamento

Amethyst-Android é LGPL-3.0; seus submódulos e bibliotecas nativas têm licenças próprias. O código upstream permanece no repositório original, referenciado por SHA fixo no workflow. Este repositório inclui a licença principal e o script de integração Water. Antes de redistribuir builds fora do artefato de desenvolvimento, publique também a revisão/fonte correspondente do runtime e preserve os avisos de todas as dependências.

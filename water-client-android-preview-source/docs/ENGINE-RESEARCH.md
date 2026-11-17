# Pesquisa do runtime Android

## Estado analisado

O diretório atual não continha o projeto Water Client anterior. A nova base foi iniciada como Android nativo; ela não incorpora o launcher Fabric/Electron anterior.

## Referência: Amethyst Android

O repositório [AngelAuraMC/Amethyst-Android](https://github.com/AngelAuraMC/Amethyst-Android) é um launcher Android/iOS baseado em Boardwalk/PojavLauncher e tem milhares de commits de engenharia de runtime. O README upstream informa suporte amplo a versões e carregadores, mas aponta uma compilação composta por módulos e componentes nativos, runtimes Java pré-compilados e LWJGL personalizado. Não é uma dependência Android simples que possa ser adicionada a `build.gradle`.

Elementos relevantes observados na fonte upstream:

- JREs móveis separados por versão e arquitetura; o projeto documenta ARM32, ARM64, x86 e x86_64.
- LWJGL/GLFW precisa de stubs e integração nativa com a `ANativeWindow`.
- A ponte EGL encaminha chamadas OpenGL; Vulkan e renderizadores alternativos seguem caminhos nativos próprios.
- Os argumentos da JVM, bibliotecas nativas, ambiente do renderer, teclado/mouse/controle e processo de jogo são tratados em componentes específicos do runtime.
- A compilação upstream requer submódulos recursivos e, para alguns caminhos, JRE/LWJGL preparados. O README também alerta sobre caminhos de build com espaços.

O código da referência é LGPL-3.0. Qualquer integração futura deve manter avisos, atribuições, licenças e disponibilizar modificações cobertas pela LGPL. Esta base não copia nem redistribui arquivos do Amethyst.

## Arquitetura recomendada

1. Escolher um commit/release upstream específico e revisar todas as licenças dos submódulos e bibliotecas nativas.
2. Integrar o runtime como módulo isolado, preservando avisos e limites de licença; a UI não deve iniciar JVM nem manipular bibliotecas nativas diretamente.
3. Implementar uma interface de runtime para instalação de versão, carregador, autenticação, parâmetros de memória, renderer, entrada e ciclo de vida do processo.
4. Testar primeiro ARM64 e GPU/renderizadores reais. Compatibilidade de um carregador não pode ser inferida só pela existência de um seletor de versão.
5. Configurar OAuth Microsoft com aplicativo registrado e fluxo oficial; nunca persistir senha, nem criar tokens offline falsos.

## Serviços e autenticação

- A busca Android usa `GET /v2/search` do Modrinth, filtrando tipo `mod`, com `User-Agent` identificável. Resultados abrem a página oficial; a instalação ainda não é apresentada como pronta.
- A API CurseForge usa `x-api-key`. A chave precisa ser autorizada e não deve ser embutida no APK público; será necessário backend ou configuração segura de distribuição.
- Para um app móvel Microsoft, usar biblioteca oficial e Authorization Code com PKCE/OIDC. Isso depende de registro, redirect URI, assinatura Android e validação de propriedade Minecraft; token Microsoft por si só não equivale à verificação de licença Minecraft.

## Orientação e versões recentes do Android

O Manifest solicita `landscape`. O projeto também mantém conteúdo principal com rolagem horizontalmente organizada. Android 16 pode ignorar a orientação solicitada em janelas de largura mínima de 600 dp quando o app tem target SDK 36 ou superior; nesses dispositivos a interface precisa continuar adaptável. Em telefones compactos, o bloqueio de landscape permanece solicitado.

## Bloqueios do ambiente nesta etapa

Este ambiente tem JDK 21, mas não tem Gradle, Android SDK/ADB, dispositivo Android nem configuração OAuth Microsoft. Portanto, não foi possível compilar APK nem validar landscape em aparelho.

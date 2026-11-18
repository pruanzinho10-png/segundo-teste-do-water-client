# Teste do Water Client no MuMu

O APK com runtime Java é criado pelo workflow do GitHub porque o build busca o engine Android, os submódulos nativos e os runtimes Java necessários.

## Gerar e instalar

1. Envie o repositório para o GitHub e abra **Actions → Build Water Client with Java runtime**.
2. Baixe `water-client-android-debug` nos artefatos da execução concluída.
3. Instale o APK no MuMu. Abra WATER CLIENT e toque em **Jogar** para entrar no launcher integrado.
4. Na primeira execução, configure uma conta e instale uma versão do Minecraft.

## Logs

Com o MuMu aberto e o APK instalado, no PowerShell:

```powershell
$adb = "$env:ProgramFiles\Netease\MuMuPlayer\nx_main\adb.exe"
& $adb devices -l
& $adb logcat -c
```

Abra o app e reproduza o problema. Depois capture os logs:

```powershell
& $adb logcat -d -s WaterClientDiag:I AndroidRuntime:E PojavLauncher:I
```

Se o Android recusar instalar por assinatura diferente, desinstale o APK anterior no MuMu e instale o novo artefato. A desinstalação apaga os perfis locais e os arquivos do jogo guardados pelo app.

# Diagnóstico da tela no MuMu

Este build registra a montagem da interface com a tag `WaterClientDiag`.

## Gerar o APK

Na raiz do repositório, execute:

```powershell
gradle -p android :app:assembleDebug --console=plain
```

O APK fica em `android/app/build/outputs/apk/debug/app-debug.apk`. O build usa `versionCode` 2 e `versionName` `0.1.0-diagnostic`.

## Capturar o log

Com o MuMu aberto e o APK de diagnóstico instalado:

```powershell
$adb = "$env:ProgramFiles\Netease\MuMuPlayer\nx_main\adb.exe"
& $adb devices -l
& $adb logcat -c
```

Abra o WATER CLIENT no MuMu e então rode:

```powershell
& $adb logcat -d -s WaterClientDiag:I AndroidRuntime:E
```

As linhas `WaterClientDiag` informam a página escolhida, a quantidade de componentes criados, o tamanho/visibilidade do `ScrollView` e os limites dos filhos após o layout. `AndroidRuntime` mostrará uma exceção caso a tela falhe durante a montagem.

Se o Android recusar instalar o APK por assinatura diferente, desinstale a versão anterior no MuMu e instale o novo artefato. A desinstalação apaga os perfis locais salvos naquele emulador.


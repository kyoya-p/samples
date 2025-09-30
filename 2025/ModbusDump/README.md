# Usage
```sh:GUI
moddump
```

# Debug run
```sh
sh gradlew run # GUI
```

# Build
```sh: Windows Installer作成
sh gradlew packageMsi
```
成果物: `composeApp\build\compose\binaries\main\msi\moddump-x.x.x.msi`
実行: `moddump-x.x.x.msi`

```sh:実行ファイル作成
sh gradlew createDistributable
```
成果物: `composeApp/build/compose/binaries/main/app/moddump` フォルダ以下  
実行: `moddump.exe`

# Refer

- https://www.modbustools.com/


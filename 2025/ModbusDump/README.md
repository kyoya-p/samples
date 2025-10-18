# Usage
```sh
moddump
```

# Debug run
```sh
sh gradlew run
sh gradlew hotRunJvm --auto # Hot Reload
```

# Build
```sh: Windows Installer作成
sh gradlew packageMsi
```
成果物: `composeApp\build\compose\binaries\main\msi\moddump-x.x.x.msi`
インストール実行: `moddump-x.x.x.msi`

```sh:実行ファイル作成
sh gradlew createDistributable
```
成果物: `composeApp/build/compose/binaries/main/app/moddump` フォルダ以下  
実行: `moddump.exe`

# TODO
- id:2510 リアルタイムスキャン

# Refer

- https://www.modbustools.com/


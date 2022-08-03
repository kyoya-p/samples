
プロジェクト履歴
---
IntelliJ 2022.2

File ➔ 新規 ➔ プロジェクト ➔ Compose Multiplatform 
➔ SinglePlatform / Desktop

ビルド/実行
---
## Windows 実行ファイル.exe
```
gradlew createDistributable
```
#### ターゲットファイル
`.\build\compose\binaries\main\app\KtComposeDesktopWin`

#### 実行
`.\build\compose\binaries\main\app\KtComposeDesktopWin\KtComposeDesktopWin.exe`

## Windows インストーラ.msi
```
gradlew packageMsi
```

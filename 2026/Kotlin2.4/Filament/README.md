# Practical Filament Demo (Kotlin 2.4 / Gradle 9.6 / mise)

Nicole Terc 氏の講演「Practical Filament - Reshape your UI!」に基づく Filament コアアーキテクチャおよび PBR レンダリングのデモ実装。

## 情報ソース
- 講演動画: [Practical Filament - Reshape your UI! | Nicole Terc (YouTube)](https://www.youtube.com/watch?v=J4lM9TnlPxk)
- 公式リポジトリ: [nicole-terc/PracticalFilament (GitHub)](https://github.com/nicole-terc/PracticalFilament)
- Google Filament 公式: [google/filament (GitHub)](https://github.com/google/filament)

## 講演における重要コンセプトと実装対応

| 講演トピック | 実装クラス / ファイル | 説明 |
|---|---|---|
| **Engine & Scene Graph** | [`PracticalFilamentEngine`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/core/FilamentEngine.kt), [`Scene`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/core/Scene.kt) | リソース管理、エンティティ登録、カメラ・ライト制御 |
| **Directional Light Gotcha** | [`Light`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/light/Light.kt) | Filament のディレクショナルライトは「光源がある方向」ではなく**「光が進む方向（Emission Vector）」**を指定。正面からの光は `-Z` 方向 |
| **Tangents Gotcha** | [`Quaternion.fromNormalAndTangent`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/core/Math3D.kt), [`Mesh.computeTangents`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/geometry/Geometry.kt) | `VertexAttribute.TANGENTS` には通常の法線ではなく、接空間クォータニオンを渡す仕様に準拠 |
| **PBR Material System** | [`Material`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/material/Material.kt), [`PbrParameters`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/material/Material.kt) | Metallic-Roughness モデル（baseColor, metallic, roughness, reflectance, clearCoat） |
| **MaterialBuilder** | [`MaterialBuilder`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/material/MaterialBuilder.kt) | Filament の `.mat` 構文の動的生成 |
| **GUI Desktop App** | [`DesktopApp`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/demo/DesktopApp.kt) | GUI ウィンドウを起動しマウスドラッグによる3D回転・モデル切替・マテリアル切替を表示 |
| **Console Rasterizer** | [`ConsoleRasterizer`](file:///C:/Users/kyoya/home26/works/samples/2026/Kotlin2.4/Filament/src/main/kotlin/practicalfilament/demo/ConsoleRasterizer.kt) | ターミナル上での ANSI TrueColor 3D レンダリング |

## 実行コマンド (mise)

```bash
# 1. デスクトップ GUI ウィンドウアプリ起動 (マウス回転・Spaceで形状変更・Mで質感変更)
mise run desktop
# または
mise run gui

# 2. コンソールでの 3D PBR デモ実行
mise run demo
# または
mise run run

# 3. 単体テスト実行
mise run test-all
```

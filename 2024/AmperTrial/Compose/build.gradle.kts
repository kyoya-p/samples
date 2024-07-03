import org.jetbrains.compose.desktop.application.dsl.TargetFormat

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "AmperTrial"
            packageVersion = "0.0.1"
            windows {
                shortcut = true
                menu = true
                upgradeUuid = "836f0fc9-0000-0000-0000-4e0b7513cd72"
            }
        }
    }
}


import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(libs.voyager.navigator)
            implementation(libs.composeImageLoader)
            implementation(libs.napier)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kstore)
            implementation(libs.kstore.file)

            api(compose.foundation)
            api(compose.animation)
            api("moe.tlaster:precompose:1.5.10") // https://mvnrepository.com/artifact/moe.tlaster/precompose

            implementation("org.snmp4j:snmp4j:3.7.8") // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
            implementation("com.squareup.okio:okio:3.8.0")  // https://mvnrepository.com/artifact/com.squareup.okio/okio
            implementation("com.charleskorn.kaml:kaml:0.57.0")  // https://mvnrepository.com/artifact/com.charleskorn.kaml/kaml
            implementation("androidx.datastore:datastore-preferences-core:1.1.1") // https://mvnrepository.com/artifact/androidx.datastore/datastore
//            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha08")
//            implementation("androidx.navigation:navigation-compose:2.8.0-alpha08")
            implementation("jp.wjg.shokkaa:snmp4jutils:1.9.0")  // local private library
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.common)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
        }

    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SNMP Desktop"
            packageVersion = "1.8.3"
            windows{
                shortcut=true
                menu=true
                upgradeUuid = "836f0fc9-1179-4d4b-9eda-4e0b7513cd72"            }
        }
    }

}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
}

import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "app.grapheneos.AppCompatConfig"

    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 34
        targetSdk = 37
        versionCode = 5
        versionName = versionCode.toString()
    }

    sourceSets.getByName("main") {
        manifest.srcFile("AndroidManifest.xml")
        res.directories.add("res")
        resources.directories.add("../app_compat_configs.pb")
    }

    tasks.preBuild {
        dependsOn(":generator:run")
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val useKeystoreProperties = keystorePropertiesFile.canRead()

    if (useKeystoreProperties) {
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))

        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"]!!)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            if (useKeystoreProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            isMinifyEnabled = true
        }
    }
}

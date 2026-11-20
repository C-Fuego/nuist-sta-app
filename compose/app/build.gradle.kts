import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import java.util.Properties

plugins {
    alias(libs.plugins.nuist.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.baselineprofile)
}

android {
    splits {
        abi {
            isEnable = true
            reset()
            // 只出两种架构的包：arm64 真机、x86_64 模拟器
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    namespace = "dev.duohuo.nuist.sta.compose"

    val properties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        try {
            properties.load(keystorePropertiesFile.inputStream())
        } catch (e: Exception) {
            println("Warning: Could not load keystore.properties file: ${e.message}")
        }
    }
    val storeFile = properties.getProperty("storeFile") ?: System.getenv("KEYSTORE_FILE")
    val storePassword = properties.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")
    val keyAlias = properties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
    val keyPassword = properties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")
    val hasCustomSigning = storeFile != null && storePassword != null && keyAlias != null && keyPassword != null

    defaultConfig {
        // Please change the applicationId to one that does not conflict with any official release.
        applicationId = project.findProperty("APP_ID") as String? ?: "dev.duohuo.nuist_sta_app.compose"

        // Version control retrieved from git, with a build-plugin fallback when git is unavailable.
        versionCode = project.getGitCommitCount()
        versionName = project.getBaseVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasCustomSigning) {
            register("releaseCustom") {
                this.storeFile = rootProject.file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = if (hasCustomSigning) {
                println("Applying custom signing to debug build.")
                signingConfigs.getByName("releaseCustom")
            } else {
                println("No custom signing info. Debug build will use the default debug keystore.")
                signingConfigs.getByName("debug")
            }
            optimization.enable = false
        }
        getByName("release") {
            signingConfig = if (hasCustomSigning) {
                println("Applying custom signing to release build.")
                signingConfigs.getByName("releaseCustom")
            } else {
                println("No custom signing info. Release build will use the default debug keystore.")
                signingConfigs.getByName("debug")
            }
            vcsInfo.include = false
            optimization.enable = true
        }
        create("nonMinifiedRelease") {
            signingConfig = if (hasCustomSigning) {
                signingConfigs.getByName("releaseCustom")
            } else {
                signingConfigs.getByName("debug")
            }
        }

        create("benchmarkRelease") {
            isDebuggable = true
            signingConfig = if (hasCustomSigning) {
                signingConfigs.getByName("releaseCustom")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    flavorDimensions.add("level")

    productFlavors {
        create("Unstable") {
            dimension = "level"
            isDefault = true
            versionNameSuffix = ".${project.getGitHash()}"
            buildConfigField("int", "BUILD_LEVEL", "0")
        }

        create("Preview") {
            dimension = "level"
            versionNameSuffix = ".${project.getGitHash()}"
            buildConfigField("int", "BUILD_LEVEL", "1")
        }

        create("Stable") {
            dimension = "level"
            buildConfigField("int", "BUILD_LEVEL", "2")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }

    packaging {
        jniLibs {
            // Module-specific exclusions
            excludes += setOf(
                "lib/*/libandroidx.graphics.path.so",
                "lib/*/libdatastore_shared_counter.so",
            )
        }
    }

    androidResources {
        generateLocaleConfig = true
    }
}

/*configurations.all {
    exclude(group = "androidx.navigationevent", module = "navigationevent-compose")
}*/

aboutLibraries {
    library {
        // Enable the duplication mode, allows to merge, or link dependencies which relate
        duplicationMode = DuplicateMode.MERGE
        // Configure the duplication rule, to match "duplicates" with
        duplicationRule = DuplicateRule.SIMPLE
    }
}

dependencies {

    "baselineProfile"(project(":baselineprofile"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)

    // implementation(libs.androidx.navigation3.runtime)
    /*    implementation(libs.androidx.navigationevent) {
            exclude(group = "androidx.navigation", module = "navigationevent-compose")
        }*/

    implementation(libs.compose.materialIcons)
    // Preview support only for debug builds


    implementation(libs.ktx.serializationJson)

    // 校园网 Dart 核心：无界面 Flutter 引擎 + AOT 快照
    debugImplementation("dev.duohuo.nuist_bridge:flutter_release:1.0")
    releaseImplementation("dev.duohuo.nuist_bridge:flutter_release:1.0")

    // QR 扫描 / 生成
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)


    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)





    // aboutlibraries

    // log
    implementation(libs.timber)

    // miuix
    implementation(libs.miuix.core)
    implementation(libs.miuix.material)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.navigation)

    // okhttp

    // monetcompat
    implementation(libs.monetcompat)


    implementation(libs.materialKolor)
}

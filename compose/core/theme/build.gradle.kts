plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.duohuo.nuist.sta.compose.core.theme"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.materialKolor)
    implementation(libs.materialIconsCore)
    implementation(libs.materialIconsExtended)
    implementation(libs.miuix.nav)
    implementation(libs.androidx.activity.compose)
}

// Miuix（及其依赖）以 JVM 21 字节码发布，本模块必须同为目标版本才能内联其 inline 函数
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

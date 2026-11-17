plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.duohuo.nuist.sta.compose.core.auth"
    compileSdk = 37
    defaultConfig { minSdk = 33 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Miuix（及其依赖）以 JVM 21 字节码发布，本模块必须同为目标版本才能内联其 inline 函数
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

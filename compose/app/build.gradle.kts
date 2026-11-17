plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.duohuo.nuist.sta.compose"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.duohuo.nuist_sta_app.compose"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-compose"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
    implementation(project(":core:theme"))
    implementation(project(":core:storage"))
    implementation(project(":features:free_classroom"))
    implementation(project(":features:electricity"))
    implementation(project(":features:score"))
    implementation(project(":features:study"))
    implementation(project(":features:credits"))
    implementation(project(":features:bulletin"))
    implementation(project(":features:timetable"))
    implementation(project(":features:campus_map"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.miuix.nav)
}

// Miuix（及其依赖）以 JVM 21 字节码发布，本模块必须同为目标版本才能内联其 inline 函数
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// NUIST++ Compose 版（Android 专用）—— 影子入口工程
// 只服务 Android；不保留 iOS/Web/Desktop target。
pluginManagement {
    repositories {
        // 阿里云镜像优先（国内加速），随后退回官方源
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}

rootProject.name = "nuist-sta-compose"
include(":app")
include(":core:theme")
include(":core:auth")
include(":core:network")
include(":core:logging")
include(":core:storage")
include(":features:free_classroom")
include(":features:electricity")
include(":features:score")
include(":features:study")
include(":features:credits")
include(":features:bulletin")
include(":features:timetable")
include(":features:campus_map")

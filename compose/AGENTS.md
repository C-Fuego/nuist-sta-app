# AGENTS.md

## Purpose

本文件是本仓库（`nuist-sta-app/compose`）对 AI 编程助手的仓库级约束。它描述**当前真实状态**，不是历史文档：陈旧条款比没有条款更危险，改代码时请先读完相关小节再动手。

适用范围：本目录及以下所有文件。工作区根目录的 `AGENTS.md` 同样有效，两者冲突时以更具体、更近的一条为准。

---

## 项目定位

- 产品：**NUIST++**，南京信息工程大学的校园服务 App（课表、空教室、电费、成绩、公告、校园地图、学习统计等）。
- 本模块：**Jetpack Compose + Material 3 Expressive** 的 Android 客户端，正在逐步取代仓库根目录的 Flutter 实现（`nuist-sta-app/lib`）。
- 迁移策略是「绞杀」：Compose 侧先行覆盖，功能完整后再考虑下线 Flutter 侧。**不要在 Flutter 侧做 UI 改造**，除非用户明确要求。
- 当前阶段：底座已接通（主题、导航、毛玻璃、底栏框架），校园页面正在逐个灌入，数据暂时是 mock。

### 本项目不做什么

- 不引入 Zig 或其他系统级语言。
- 不做任何数据采集、埋点、遥测、设备指纹；不接入第三方统计 SDK。本地日志可以保留，用于开发排查。
- 不恢复被剥离的安装器业务（APK 安装/卸载、Root/Shizuku/Dhizuku、包管理、安装配置档等）。这些代码已整体删除，不要"顺手"加回来。

---

## 硬规则（违反即返工）

1. **命名洁净**：代码、注释、文档、提交信息、面向用户的文案中都不得出现上游参考实现的项目名称。移植文件的版权头可以保留 SPDX 许可标识与中性署名（"原作作者与贡献者"）。项目名只允许出现在用户最终撰写的鸣谢里。
2. **不得自行提交**：没有用户明确指示时，不创建 commit、不 push、不改 git 配置、不加 AI 署名/`Co-authored-by`。需要暂存时按路径 `git add <path>`，禁止 `git add -A` / `git add .`。
   **不得在用户没有提示的情况下改动 `AGENTS.md`**（根目录与本目录的都算）；认为条款需要更新时先向用户提出，由用户决定是否落笔。
3. **不得私自删动代码块**：删除或大改既有模块前，先列清单与影响面并取得批准；删除后必须能通过构建。
4. **工具链留在工作区内**：JDK、Android SDK、Gradle 分发与缓存、Flutter、Rust 等一律放在工作区 `toolchain/` 下，禁止在家目录产生开发目录或依赖缓存。
5. **依赖下载优先镜像**：Gradle 分发包用腾讯云/华为云镜像；Maven 用 `maven.aliyun.com`（public / google / gradle-plugin）且已置顶。新增仓库前先看 `settings.gradle.kts`。
6. **不猜包名、不猜 API**：不确定的类、资源、依赖版本先检索现有代码与版本目录，不要凭印象写。

---

## 构建与工具链

### 环境

所有构建命令前必须先 source 工具链环境，它会把 `JAVA_HOME`、`ANDROID_HOME`、`GRADLE_USER_HOME`、`FLUTTER_ROOT` 等全部指向工作区：

```bash
source ../../toolchain/env.sh    # 在 nuist-sta-app/compose 下
```

- JDK：源码要求的 `BuildConfig.JDK_VERSION = 25`，由 foojay toolchain resolver 自动下载到 `GRADLE_USER_HOME/jdks`，**不要改成 17/21**。`env.sh` 里的 `JAVA_HOME` 只是启动 Gradle 的 JVM。
- Android SDK：需要 platform `android-37.2`（`COMPILE_SDK = 37`、`COMPILE_SDK_MINOR = 2`、`TARGET_SDK = 37`、`MIN_SDK = 26`）。
- Gradle：固定使用 `./gradlew`（9.9.1，分发包指向工作区 `toolchain/dists` 的本地 zip，不要改成在线 URL）。
- 依赖：Miuix 走 Maven Central 的已发布版本；`settings.gradle.kts` 里的 GitHub Packages 仓库仅用于快照，缺少 `gpr.user` / `gpr.key` 时不要启用它。

### 常用命令

```bash
./gradlew :app:assembleStableDebug    # 日常验证：单变体，最快
./gradlew :app:assemblePreviewDebug   # 变更涉及资源/依赖/变体逻辑时
./gradlew :app:assembleDebug          # 三变体全量（Stable/Preview/Unstable）
```

产物在 `app/build/outputs/apk/{Stable,Preview,Unstable}/debug/app-*-debug.apk`，**不是** `apk/debug/`。

安装与联调：

```bash
adb -s <serial> install -r app/build/outputs/apk/Stable/debug/app-Stable-debug.apk
adb -s <serial> shell am start -n <applicationId>/<入口 Activity>
```

### 变体

只有一个 flavor 维度 `level`：`Unstable` / `Preview` / `Stable`。不要扁平化、改名或绕过 flavor 逻辑；`VERSION_CODE`、git hash 版本后缀都在 `build-plugins/src/main/kotlin/BuildConfig.kt` 与 `app/build.gradle.kts` 里集中控制，与任务无关时不要动。

签名：本地无签名材料时回退 debug keystore，这是预期行为，不要"修"。

---

## 仓库结构

```
compose/
├── app/                # 唯一应用模块
├── baselineprofile/    # baseline profile 生成
├── build-plugins/      # 约定插件：nuist.android.{application,library,test}.gradle.kts、nuist.spotless
├── gradle/libs.versions.toml   # 依赖与插件版本唯一来源
└── settings.gradle.kts         # 仓库与模块声明，FAIL_ON_PROJECT_REPOS
```

已被移除的模块：`app-process/`、`hidden-api/`。不要重新 include。

### 包结构（`app/src/main/java/dev/duohuo/nuist/sta/compose/`）

- `core/` —— 应用基础设施：主题、动画、图标、通用 widget、崩溃处理、环境配置。
- `data/` —— 持久化与具体实现：DataStore/Room、provider、repository、mapper。
- `domain/` —— 模型、契约（repository/provider 接口）、use case。
- `di/` —— Koin 模块与初始化接线。
- `ui/` —— 页面、组件、导航、主题；`ui/page/` 下按设计家族分 `main/`（Material 3 Expressive）与 `miuix/`（Miuix）。
- `util/` —— 无状态工具函数。

跨层依赖方向固定：`ui → domain ← data`，`di` 负责装配。不要把业务规则塞进 UI，也不要在 data 层做展示逻辑。

---

## 架构约定

### 依赖注入

Koin 统一在 `di/` 下按主题分模块（`coreModule`、`settingsModule`、`viewModelModule`、`initModule` 等），聚合在 `di/init/app_modules.kt`。新增可注入依赖时放进最相关的既有模块，不要造全局单例。

### 应用初始化

`App.kt` 的顺序是敏感的：CrashHandler → Monet（仅 SDK < 31）→ 条件日志 → Koin bootstrap。除非任务要求，不要重排、不要删步骤。

### 主题

- 入口：`ui/theme/InstallerTheme.kt` 的 `InstallerTheme(...)`，参数覆盖 `useMiuix`、`themeMode`、`paletteStyle`、`colorSpec`、`useDynamicColor`、`useMiuixMonet`、`seedColor`、`useAmoled`、`useMiuixOs4`。
- 配色由 materialkolor 生成，支持动态取色（Android 12+ 系统壁纸、更低版本 MonetCompat）。
- 主题状态用 `ThemeStateProvider` + `AppSettingsRepository`，持久化走 DataStore（键在 `data/settings/local/datastore/AppDataStore.kt`）。
- 品牌默认值：seed `#335BBC`（蓝）。

### 设置类改动清单

改一个设置项时按顺序确认：domain 模型 → provider/repository 契约 → data 存储与 mapper → DI 注册 → UI state/action/viewModel → **两套 UI 家族都要改** → 中英文文案。

### 双 UI 家族

`ui/page/main/` 是 Material 3 Expressive，`ui/page/miuix/` 是 Miuix。两者共享领域逻辑，但实现各自原生：

- 不要把 Miuix 组件漏进 Material 3 页面（反之亦然），除非有意为之。
- 改共用逻辑时不能只验证一个家族。
- 通用组件放在能容纳差异的层级，不要把某个页面的产品决策硬编码进通用 widget。

---

## UI 约定（Material 3 Expressive）

- 遵循 MD3E：`MaterialExpressiveTheme` + `MotionScheme.expressive()`，优先用 expressive 组件（`LargeFlexibleTopAppBar`、`ShortNavigationBar`、`WideNavigationRail`、`ButtonGroup`、`HorizontalFloatingToolbar`、`ToggleFloatingActionButton`、wavy 进度条等）与 `*Emphasized` 字体。
- 图标用 Material Symbols（`ui/…/SymbolIcons.kt` 中的码位映射）；填充/描边由 FILL 轴控制，不要混用 Material Icons 补齐形状。
- 间距、圆角、字体一律走 `MaterialTheme` 与项目内的尺寸常量，不写魔法值。
- 悬浮底栏/毛玻璃等框架组件已封装好，优先复用 `ui/library/`、`ui/animation/` 下的实现，不要自造轮子。
- 页面骨架：顶栏（大标题可折叠）+ 内容区 + 底部净空；窄屏用底栏、宽屏（≥ 840dp）用侧栏。

---

## 文案与本地化

- 开发者维护 **英文 + 简体中文**；其他语言走 Weblate，不接受直接翻译 PR。
- 改用户可见文案时中英文一起改，保持既有术语（如"学期""周次""空教室"）一致。
- 面向用户的措辞要准确，不要为了简洁删掉必要的说明（权限、数据来源、失败原因）。

## 隐私与权限

- 权限只允许：`INTERNET`、`ACCESS_NETWORK_STATE`、`POST_NOTIFICATIONS`。新增任何权限都要先说明理由并取得批准。
- 不采集设备标识、不上报日志与使用数据。新增依赖前确认它不引入统计/推送/Firebase 之类 SDK。
- 日志只写本地，经 `AppConfig.isLogEnabled` 控制。

---

## 验证与交付

- 至少跑 `./gradlew :app:assembleStableDebug`，涉及资源、依赖或变体逻辑时跑 `assemblePreviewDebug`。
- 声称"已通过"必须是真跑过；没跑或没跑通要明说。
- UI 改动优先在真机/模拟器上验证（`android_*` 工具或 `adb`），能用 UI 树/截图确认就不要只靠读代码。
- Android / MD3E 相关任务优先加载 `md3e`、`android-cli`、`android-ui-automation` 技能。

## 提交规范

仅在用户明确要求时提交。Conventional Commits，主题 ≤ 72 字符、小写、祈使语气：

`fix:` `feat:` `refactor:` `docs:` `i18n:` `fix(deps):`

依赖变动要写出包名与 `from -> to` 版本。禁止 AI 署名与生成器标记。

## 交付格式

- **改动**：改了哪些文件/区域。
- **行为**：用户或维护者应看到什么变化。
- **验证**：跑了什么命令、结果如何。
- **备注**：迁移风险、未验证场景（仅在确实存在时写）。

# Compose + MD3E 渐进式重构 · 阶段 0 地基 + 阶段 1 样板 设计

> 状态：**设计待审核，不含实现代码**。审核通过、且你确认放置位置后再落实现。
> 前置决策（已由用户拍板）：strangler 逐步替换至全量；无硬指标；部分功能冻结、缓慢跟进上游 Flutter；**不引入 Zig**；重构期间不做 Flutter 侧体验优化。
> 本文遵守仓库 `AGENTS.md`：先设计后动手、不擅自改 `pubspec`/`AGENTS.md`/CI、不代用户 commit、不新增遥测。

---

## 0. 结论摘要

- **共存方式（建议采纳）**：`A2 起步 → A1 收尾`。先用**双入口**（同一 Android 工程里两个 Activity 入口：Flutter 版 + Compose 版）让 Compose 版成为可独立运行、可独立发布的"影子 App"；等 Compose 版功能覆盖到约 80%（小程序全迁完）再切主入口、Flutter 壳下线（转入 A1 的混合宿主收尾形态）。
- **理由**：无硬指标 → 用最简单的工程边界（两个 Activity，互不依赖）换取最大回退安全；不需要在重构期维护 Flutter↔Native 桥的复杂度（那是 A1 才要的），把风险推迟到"已经有把握"的最后阶段。
- **阶段 0** 先把 `core/` 四大地基（认证 / 网络+VPN / 主题+MD3E / 日志脱敏 / Store）在 Kotlin 侧立起来，这是决定后续每个小程序迁移速度的关键。
- **阶段 1** 用 `free_classroom`（纯读、无写、无 Passkey 写、无 VPN 直连）做首个样板，产出**可复用的迁移模板 + 验证清单**。

---

## 1. 仓库与技术现状（设计依据）

| 项 | 现状 |
|---|---|
| 框架 | Flutter 3.47.4 stable，Dart SDK `^3.13.3` |
| 结构 | 壳（3 页签）+ 注册表 + 12 个小程序 |
| 状态 | `ChangeNotifier` 私有构造单例 + `ensureStarted()`；`Store` 全 static + try/catch 降级；`Api` 全 static |
| 认证 | `PortalSession`（CAS + Passkey/WebAuthn，Dio + cookie_jar，`pointycastle`/`asn1lib` 处理签名与 ASN.1） |
| VPN | `vpnDio()` 上层 + `libnuist_vpn.so`（Rust 1849 行，随 APK 由 NDK 编译，仅 arm64） |
| 主题 | Material 3；`AppPalette.of(context)`（ThemeExtension，14 个色值 + brightness）；`AppColors/AppColorsDark` 仅供构造 |
| 日志 | `AppLog` 结构化 + 定点脱敏 + 轮转 + 用户导出；禁 `print/debugPrint/developer.log` |
| 依赖方向 | `mini_apps/*` 只能 import `core/*` 与三方包（CI 强制）；`shell/*` 只 import `core/*` 与 `registry.dart` |
| 测试 | 135 个测试文件；CI 三道（ci / commit-lint / build-release） |

阶段 0 的直接映射目标即上表 `core/` 各行。

---

## 2. 目标架构（Compose 侧）

### 2.1 仓库/模块布局

建议在**同一仓库**内新建独立 Gradle 工程，与现有 Flutter 工程并列，互不侵入：

```
nuist-sta-app/                     # 现有仓库根
├── lib/ android/ ios/ ...         # 现有 Flutter 工程（不动，继续可发布）
├── native/vpn/                    # Rust VPN 核心（不动，复用 .so）
└── compose/                       # ★ 新增：Compose 工程（独立 Gradle）
    ├── settings.gradle.kts
    ├── build.gradle.kts
    ├── gradle/libs.versions.toml  # version catalog
    ├── core/                      # 阶段 0 地基（Compose 侧的 core/）
    │   ├── auth/  network/  theme/  logging/  storage/  config/
    ├── shell/                     # 阶段 3 才迁；阶段 1 只放一个最小导航壳
    ├── features/                  # 各"小程序"，对应 lib/mini_apps/*
    │   └── free_classroom/        # 阶段 1 样板
    └── app/                       # Compose 版 Application + MainActivity（影子入口）
```

**依赖方向沿用原约定**：`features/*` 只能依赖 `core/*` 与三方库，禁止依赖 `shell/*` 或其他 feature。用 Gradle 模块隔离天然强制（比 Dart 的 CI 检查更硬）。

> ⚠️ 待你确认（Q1）：Compose 工程放 `compose/` 子目录，还是你希望放在独立仓库？（放同仓便于复用 `native/vpn` 与 `lib/core/config/campus_config.dart` 的 hex 常量；独立仓则需同步那些常量。）

### 2.2 关键映射表（Flutter → Kotlin/Compose）

| Flutter 概念 | Compose 侧对应 |
|---|---|
| `ChangeNotifier` 单例 + `ensureStarted()` | `ViewModel` + `StateFlow<UiState>`；`ensureStarted` → `init{}` 内幂等加载 |
| `ListenableBuilder` | `collectAsStateWithLifecycle()` |
| `Store`（static + JSON 文件） | `DataStore`（偏好/小数据）或 `Room`（结构化）；写操作返回 `Result<T>`，失败降级"无存储" |
| `Api`（static + Dio） | `Repository` + OkHttp；抛 `PortalException` 的 Kotlin 等价 `sealed class` |
| `go_router` `/apps/:appId` | Navigation Compose `NavHost` + 路由 `"apps/{appId}"` |
| `AppPalette.of(context)` | `MaterialTheme.colorScheme` + 自定义 `CompositionLocal`（夜间随 `isSystemInDarkTheme()` 或手动模式） |
| `AppManifest` / `AppRegistry` | `Feature` 接口 + `FeatureRegistry`（宫格/卡片/入口的声明，编译期注册） |
| `context.push('/apps/<id>')` | `navController.navigate("apps/$id")`（**不用 popUpTo 清栈，保证返回**） |
| 预测性返回 | `enableOnBackInvokedCallback=true`（已有）+ Compose `PredictiveBackHandler` |
| `AppLog` | 同规则集的 Kotlin 日志门面（`Timber` 风格但禁用三方上传；自写 `AppLog` 保持脱敏一致） |
| `flutter_secure_storage` | Android `EncryptedSharedPreferences` / Keystore 包裹 |
| Passkey / WebAuthn | `androidx.credentials`（Credential Manager）+ `java.security` 签名 |

### 2.3 MD3E 主题骨架（阶段 0 产出）

用 `md3e` 技能（Material3 Compose，目标 `material3 1.5.0-rc01`）搭：
- `ColorScheme`：沿用现有品牌色（从 `AppColors` 的种子色复刻），明暗两套；`AppPalette` 的 14 个语义色 → M3 role + 少数自定义 `extendedColors`（用 `CompositionLocal` 承载 `success/warning/danger` 这类 M3 没有的）。
- `Typography` / `Shape` / 动效（M3E 的 expressive motion）骨架。
- 一份 `Md3eTheme` 样板，供后续 feature 统一引用。

---

## 3. 阶段 0：地基详细设计

阶段 0 的验收标准：**Compose 影子 App 能完成一次真实登录 + 一次真实业务请求（空教室），且日志脱敏、VPN、主题明暗、返回手势全部走通**。即阶段 0 与阶段 1 的验收在样板里合并验证。

### 3.1 认证层（最高风险）

- 目标：Kotlin 复刻 `PortalSession` 的能力子集——`ensureLoggedIn(service)`、`clientFor(service)`、`request(...)`、`clear()`；先不追求 100% 特性（证据链、WebView 同步等可后置）。
- 关键风险点：
  1. **Passkey/WebAuthn 签名**：Flutter 用 `pointycastle`+`asn1lib` 手做 ECDSA/ASN.1。Kotlin 侧改用 `androidx.credentials.CredentialManager` 走系统 Passkey（更原生、更安全）；需在**阶段 0 用真实账号做一次绑定 + 一次登录**验证等价。若系统 Passkey 与现有服务端协议不兼容，回退方案：`BouncyCastle` 复刻现有 ASN.1 流程。
  2. **CAS 重定向跟随**：现有 `PortalHttp` 手动跟随重定向（`followRedirects` 自定义）。OkHttp 需用 `followRedirects(false)` + 手动跟随，保持一致，否则 CAS 精确匹配 service 会失败。
  3. **cookie 管理**：`cookie_jar` → `CookieJar`(OkHttp) + `EncryptedSharedPreferences` 持久化，语义对齐 `secure_cookie_storage`。
- 交付物：`core/auth/` 包（`PortalSession.kt`、`PortalHttp.kt`、`PortalExceptions.kt`、`CasLogin.kt`、`PasskeyStore.kt`）+ 一个可跑的登录 Demo 页。
- 测试：JVM 单测覆盖异常分类（Network/Credential/Login）、重定向跟随逻辑；真机做一次真登录。

### 3.2 网络 + VPN 层

- `vpnDio()` 语义 → `VpnClient`（OkHttp `Interceptor`）：按 `forceVpn` / `Options.extra[forceVpnKey]` 选路；**复用现有 `libnuist_vpn.so`**，Kotlin 侧写 JNI 封装（`native/vpn` Rust 源码**不改**，仅新增 Kotlin binding）。
- 复用现有 `client_metadata` native 通道逻辑（SSAID 派生、ROM 解析）→ Kotlin 复刻，注意**不传原始 AndroidID/指纹**（AGENTS.md 隐私红线）。
- 交付物：`core/network/`（`VpnClient.kt`、`VpnJni.kt`、`ClientMetadata.kt`、`OwnedApiClient.kt`）。

### 3.3 主题 + MD3E（见 2.3）

交付物：`core/theme/`（`Md3eTheme.kt`、`ExtendedColors.kt`、`ThemeMode.kt`）。

### 3.4 日志脱敏层

- 复刻 `AppLog` 的**规则集**（哪些字段遮蔽：Cookie/Authorization/ticket/JWT/Passkey/私钥/签名/PIN/设备标识/学号/姓名/成绩/课程/宿舍/搜索词/位置）。
- 结构化事件 + 轮转存储 + 用户导出；**不得引入任何上传型崩溃/统计 SDK**（AGENTS.md 红线）。
- 交付物：`core/logging/`（`AppLog.kt`、`Redactor.kt`、`LogStore.kt`、`DiagnosticError.kt`）。

### 3.5 Store 层

- `core/storage/`：DataStore 封装 + `Result` 降级；对应 `free_classroom_store` 只存"上次选的楼/楼列表"。

### 3.6 配置常量

- `core/config/`：从 `lib/core/config/campus_config.dart` 的 hex 常量派生 Kotlin `CampusHosts`（**同一份 hex 数据**，避免明文硬编码，遵守 AGENTS.md 第 7 节）。
- ⚠️ 待确认（Q2）：hex 常量是"手工同步两处"还是"用一个脚本从 Dart 生成 Kotlin"？（后者更不易漂移，但属于脚本，需你授权新增。）

---

## 4. 阶段 1：`free_classroom` 样板迁移设计

### 4.1 为什么选它（复核结论）

- 纯**读**：无写操作、无 Passkey 写、无 VPN 强制（走普通 `PortalSession`）。
- 有**本地筛选**（时段/楼层/分区/类型/座位）→ 能验证 ViewModel 持有 UI 状态、不发请求的纯筛选路径。
- 有**列表 + 持久化表头 + 空态/错误态 + 下拉刷新** → 覆盖最常用的 Compose UI 模式，模板价值高。
- 有 **Store**（上次选的楼）→ 验证 DataStore 降级。

### 4.2 文件级映射

| Flutter 文件（行数） | Compose 侧文件 | 说明 |
|---|---|---|
| `free_classroom_models.dart` (325) | `FreeClassroomModels.kt` | `Building`/`PeriodGroup`/`TermCalendar`/`Classroom`/`ClassroomDay` 等 data class + JSON 解析（`kotlinx.serialization` 或 `Moshi`，待定 Q3） |
| `free_classroom_api.dart` (300) | `FreeClassroomApi.kt`（Repository） | 4 个公开方法 `listBuildings`/`occupationNames`/`calendar`/`classrooms`，走 `PortalSession.clientFor(jwxt)`；EMAP "先访问首页"逻辑保留 |
| `free_classroom_controller.dart` (383) | `FreeClassroomViewModel.kt` | 状态 → `StateFlow<FreeClassroomUiState>`；动作方法 → ViewModel 函数；`_cache`/`_querySeq` 保留 |
| `free_classroom_store.dart` (96) | `FreeClassroomStore.kt`（DataStore） | 只存 `building`/`buildings` |
| `free_classroom_manifest.dart` (16) | `FreeClassroomFeature.kt` | 实现 `Feature` 接口：id/label/icon/entry |
| `free_classroom_page.dart` (1456) | `FreeClassroomScreen.kt` + 子组件文件 | 见 4.3 |
| `free_classroom_sheets.dart` (382) | `FreeClassroomSheets.kt` | 选楼/选类型/选座位 BottomSheet → `ModalBottomSheet` |
| `free_classroom_widgets.dart` (120) | `FreeClassroomWidgets.kt` | `kClassroomColor` 等常量/小组件 |

### 4.3 页面拆分（page.dart 1456 行的 Compose 化）

原页面结构：`Scaffold` + `AppBar('空教室')` + 刷新 action；body 为 `RefreshIndicator` + `CustomScrollView`，含 `_FilterCard`、`_PeriodHeader`(pinned sliver)、`_RoomList`、空态/错误态、`_Footer`。

Compose 侧建议拆：
- `FreeClassroomScreen`：`Scaffold` + `TopAppBar` + `PullToRefreshBox`(material3) + `LazyColumn`。
- `FilterCard`（对应 `_FilterCard`/`_BuildingRow`/`_DateStrip`/`_ChipRow`）：`ElevatedCard` + `LazyRow`(日期条) + `FilterChip`。
- `PeriodHeader`：`stickyHeader`（`LazyColumn` 的 `stickyHeader`，或 `LazyListScope` 自定义）。
- `RoomList`/`RoomRow`（`_RoomList`/`_RoomEntry`）：`items` + 折叠分节（`collapsed` 状态在 ViewModel）。
- `StateView`/`EmptyResult`/`Footer`：独立 composable。
- 房间详情：`ModalBottomSheet`。

### 4.4 状态模型草案（Kotlin，示意非最终代码）

```kotlin
data class FreeClassroomUiState(
  val portalBound: Boolean = true,
  val buildings: List<Building>? = null,
  val buildingsLoading: Boolean = false,
  val buildingsError: String? = null,
  val building: Building? = null,
  val date: LocalDate = LocalDate.now(),
  val day: ClassroomDay? = null,
  val loading: Boolean = false,
  val error: String? = null,
  val selectedGroups: Set<Int> = emptySet(),
  val floor: Int? = null,
  val zone: String? = null,
  val type: String? = null,
  val minSeats: Int = 0,
  val collapsed: Set<String> = emptySet(),
)
```
动作：`ensureStarted()` / `refreshBuildings()` / `selectBuilding()` / `selectDate()` / `toggleGroup()` / `selectAllGroups()` / `selectUpcomingGroups()` / `setFloor/Zone/Type/MinSeats()` / `query(force)`。

### 4.5 验收清单（样板必须全部通过）

- [ ] 真实登录后能拉教学楼列表、按日期+楼查到占用。
- [ ] 本地筛选（时段/楼层/分区/类型/座位）不发请求、即时生效。
- [ ] 缓存命中（来回切楼/切日期秒开）；强制刷新无视缓存。
- [ ] 门户未绑定时显示引导页（对应 `_PortalGuide`）；点进绑定。
- [ ] 明暗两套主题下配色正确（`AppPalette` 语义色 → Compose 扩展色）。
- [ ] Android 预测性返回：从宫格进 → 手势跟随、取消不丢状态、完成回上一页。
- [ ] 异常分类：网络错误重试 2 次（1s/3s）、凭据错误提示重绑，不把原始异常抛 UI。
- [ ] 日志脱敏：请求/响应正文按规则遮蔽，无明文 Cookie/学号。
- [ ] Store 降级：无存储时不崩溃（对应 widget 测试无平台通道场景）。
- [ ] 凭据无外泄：不向非学校出口携带 Cookie/学号。

### 4.6 测试策略

- JVM 单测：models 解析（对齐现有 `free_classroom_models` 覆盖的边界：`1_01,0_04` 解析、寒暑假抛错等）、ViewModel 状态迁移、筛选逻辑。
- UI 测试：Compose `createComposeRule` 覆盖空态/错误态/筛选。
- 真机：登录 + 查询 + 返回手势 + 明暗切换。
- **对照现有 Dart 测试**：把 `test/free_classroom*` 的用例逐条翻译成 Kotlin，确保行为等价（这是"迁移而非重写"的证据）。

---

## 5. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| Passkey/WebAuthn 协议与现有服务端不兼容 | 登录走不通 | 阶段 0 真机先验一次；不兼容则 BouncyCastle 复刻现有流程 |
| CAS 重定向精确匹配 | 登录失败 | OkHttp 手动跟随重定向，语义对齐 `PortalHttp` |
| `libnuist_vpn.so` 仅 arm64 | 无 arm64 设备无法测 VPN | 真机测试；模拟器仅跑非 VPN 路径 |
| 双份 UI 并存维护 | 迁移期工作量 | 按 strangler 逐模块冻结 Dart 侧，只跟上游 bugfix |
| hex 常量两处漂移 | 配置不一致 | Q2 决定是否脚本生成 |
| 日志脱敏规则不一致 | 隐私风险 | 复用同一规则集，写对照用例 |

---

## 6. 待你确认的问题（阻塞实现，不阻塞审核）

- **Q1 仓储布局**：Compose 工程放同仓 `compose/` 子目录（推荐，可复用 VPN 与常量）还是独立仓库？
- **Q2 hex 常量同步**：手工同步，还是授权新增"从 Dart 生成 Kotlin"的小脚本？
- **Q3 序列化库**：`kotlinx.serialization`（推荐，纯 Kotlin、无反射）还是 `Moshi`？
- **Q4 共存确认**：采纳 `A2 起步 → A1 收尾`？
- **Q5 文档落点**：本文暂放 `design/compose-migration-phase0-1.md`；是否合适，或改放 `docs/`（需你授权，因 AGENTS.md 限制擅自新增文档）。

---

## 7. 本轮不做的事（避免误解）

- 不写任何实现代码、不改 `pubspec.yaml`/`AGENTS.md`/CI/签名配置。
- 不迁 `shell/`、不迁其他小程序（阶段 2/3 才做）。
- 不引入 Zig、不动 `native/vpn` Rust 源码。
- 不做 Flutter 侧体验优化（你已明确重构期不投入）。

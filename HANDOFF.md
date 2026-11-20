# 交接清单（HANDOFF）

面向「换一个 AI/开发者继续干活」的状态说明。最后更新：2026-11-20，对应提交 `8f7f165`。

一句话：把原 Flutter App **只重写前端**，界面用 Jetpack Compose + Material 3 Expressive（MD3E），
校园网协议、教务解析、课表/考试/成绩等数据全部留在无界面的 Dart 层（`dart_bridge/`），
Kotlin 只负责渲染和调桥。

---

## 1. 目录与职责

| 路径 | 内容 |
| --- | --- |
| `lib/` | 原 Flutter App（**参考实现**，不要改；查行为、查文案、查交互都看这里） |
| `dart_bridge/` | 无界面 Dart 核心，`MethodChannel "nuist.bridge"`，打成 AAR 供 Compose 依赖。校园网登录、教务同步、课表/考试/成绩/公告/电费/空教室等全部在这里 |
| `compose/` | Kotlin/Jetpack Compose 前端（单模块 `:app` + `:baselineprofile`）。`core/`、`features/` 旧多模块已合并进 `app` |
| `toolchain/` | 工作区自包含工具链（Flutter/Android SDK/JDK/Gradle 缓存），**不入库**。`source toolchain/env.sh` 后所有 `$HOME`/XDG 写入都落这里 |

Compose 侧分层：`ui/`（页面、组件、导航、动画、主题）→ `data/`（桥调用、repository）→ `domain/`（模型、接口）。

主要包名：`dev.duohuo.nuist_sta_app.compose`。

---

## 2. 环境与构建

```bash
# 一切命令都要先进入仓库根并 source 工具链（注意路径：toolchain 在工作区根，不在子仓里）
cd compose && source ../../toolchain/env.sh

# Kotlin 编译检查（快，改完先跑这个）
./gradlew :app:compileStableDebugKotlin --offline

# 出 APK（arm64 + x86_64 分包）
./gradlew :app:assembleStableDebug --offline
# → app/build/outputs/apk/Stable/debug/app-Stable-<abi>-debug.apk

# 重新打 Dart AAR（改了 dart_bridge 必须做，AAR 不在仓库里）
cd dart_bridge && source ../../toolchain/env.sh
flutter analyze --no-pub
flutter build aar --release --target-platform android-arm64,android-x64
# → build/host/outputs/repo/dev/duohuo/nuist_bridge/flutter_release/1.0/flutter_release-1.0.aar
```

- `source ../toolchain/env.sh` 是**错的**，会报 `ERROR: JAVA_HOME is not set`。
- 手机：`adb -s 3B164V00ZUX00000`。装包后核对：`adb shell dumpsys package dev.duohuo.nuist_sta_app.compose | grep lastUpdateTime`。
- waydroid（`192.168.240.112:5555`）：无 root，写不了 app 私有目录；`adb install` 会卡死，用
  `adb push app.apk /data/local/tmp/` + `adb shell pm install -r -t /data/local/tmp/app.apk`。
- 设备上的本地数据（课表手动改动等）在 app 私有目录 `app_flutter/timetable/`，debug 包可用
  `adb exec-out run-as dev.duohuo.nuist_sta_app.compose tar -c app_flutter/timetable | tar -x` 导出。
- 手机 debug 签名私钥：`toolchain/android-sdk/.android/debug.keystore`（AGP 的 `ANDROID_USER_HOME` 被 env.sh 指到这里）。
  丢了之后新 debug 证书不同 → 装新包会 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，只能卸载重装。

---

## 3. 硬约束（`AGENTS.md`，务必先读）

- 不得自行 `git commit` 之外的 git 操作：不 commit 需批准前先问；不 force push、不 `reset --hard`、不 `clean`、不改 git 配置；只 `git add <路径>`。
- 迁移/移植时**不得自行删除既有代码块**；删前须列「删什么、为什么、影响面」并获批。
- 代码/注释/文档/提交信息里**禁止出现参考实现的项目名**（统称「参考实现」）。
- 只做对齐、不自作扩展；用户没提时不要改 `AGENTS.md`。
- 依赖走国内镜像；工具链只装在工作区。
- **改完不要自己操作手机看效果**，汇报进度由用户人工验收。报错细节不上页面，进诊断/日志。

界面风格：MD3E；中文文案、中文注释；文件头保留 SPDX + 版权行。
组件优先用 `ui/kit/`（`BaseWidget`、`SegmentedColumn`、`SwitchWidget`、`SingleChoiceDialog`、`ExpressiveBackButton`、`ColorPickerSheet` 等）。

---

## 4. 已实现

### 4.1 框架与主题

- 导航：`top.yukonga.miuix.kmp.nav` 的 `NavDisplay` + `rememberNavBackStack`，路由见 `ui/navigation/Routes.kt`
  （Main/Theme/FreeClassroom/Electricity/Score/Timetable/TimetableSettings/Exams/BulletinList/BulletinDetail/
  PortalAccount/Bind/Import/Export/CampusMap/InnovationCredit/LaborScore/TimetableFocus）。
- 预测性返回动画 5 种（None/MIUIX/AOSP/Scale/Classic），选择器 `ui/animation/predictiveback/AppNavTransition.kt`，
  设置里实时切换。
- 主题：`ui/theme/AppTheme.kt` —— 18 套预设调色板 + 两档色规 + AMOLED + Android 12 动态取色，
  `animateAsState()` 做颜色平滑过渡，外边套 `MaterialExpressiveTheme`（MD3E 官方）。主题设置页 `Route.Theme`。
- ViewModel 走 Koin：共享数据层用 `single`（例如 `TimetableViewModel` 全应用一份，避免每页各建一份、每进页面重走桥）。

### 4.2 首页

- 「今日课程 / 明日课程」：**每节课一张卡**，条数 = 设置里的 `homeMaxItems`（1..4）；
  点击跳课表对应周/天/节次并**描边一圈**（`Route.TimetableFocus(courseKey=…)`）。
- 「宿舍电费」「待考科目」两张卡。整页下拉刷新；卡片右上只有一个箭头，**没有刷新按钮**。

### 4.3 课表

- 周视图 + 列表视图（设置「显示方式」切换）。
- 左右滑动切周，顶部周次栏不跟着动、只换数字；第一周/最后一周有橡皮筋（翻不动），翻页平滑。
- 点周次卡弹 MD3E 日历选周（只选周）。
- 周次卡状态行：非本周 / 本周第 N 周·课程已结束 / 周次待同步 / 日期区间 · 学期 · 共 20 周；同步失败时多一行提示。
- 格内文字自动排版（`CellTextLayout.kt`，带 LRU 排版缓存）；胶囊标签竖排；课程块可点开详情。
- 考试：格内画考试、非本周期「幽灵」只在考试还在后面的周出现、已考完不画；从考试页跳过来会临时显示并描边。
- 「＋」三选一：课程（`CourseFormPage`）/ 考试 / 其他日程（`EventFormPage`，桥 `timetable.addEvent`）。
- 编辑课程：保存范围（仅这一次课 / 以后 / 全部，默认仅这一次）、改周次二次确认（新旧对照）、
  课程级换色（预设色板 + 取色面板 + 恢复自动，改完即时刷新）、分范围恢复（这一天 / 按周 / 原位置 / 恢复教务课程）。
- 整天调课：长按表头日期 → `TimetableDaySwapPage`；已调课日期在表头标「调」，点开可恢复。
- 表头未排进课表的时间：日期下画圆点，点开清单。
- 「其他」栏（无固定节次的课）：**只读**，不可编辑/删除（上游同样保存不了时间）。
- 课表管理：整天调课、变动记录（`TimetableChangeLogPage`）、恢复教务课程。
- 导出到 WakeUp（`TimetableExportPage`，桥 `timetable.exportPreview` / `exportWakeup`）。
- 后台保活设置页（`TimetableKeepAlivePage`）。
- 设置页分组：显示方式 / 列表视图 / 周视图 / 格子信息 / 课块角标 / 外观 / 考试 / 提醒 / 首页与桌面 / 课表管理。
- 提醒：上课提醒 + 明日课程提醒 + 提前分钟；通知点击跳课表；通知/精确闹钟权限缺失时在设置页显示提示行 + 「去设置」。

### 4.4 其余页面

学习页（学业统计/成绩入口）、成绩、考试安排（数据来自 `timetable.exams`，按标题+日期去重合并）、
空教室（真实日期 + 时段表头逐列对齐）、电费、公告（列表 + 详情）、校园地图、
创新学分、劳动成绩、门户账号（绑定/扫码/导入/导出）、诊断 ViewModel（见 5.6）。

### 4.5 桥（`dart_bridge/lib/bridge/router.dart`，约 60 个方法）

课表相关：`timetable.state/overview/week/snapshot/display/updateSettings/setWeek/sync/cell/saveCourse/deleteCourse/
moveCell/moveEvent/setCourseColor/changes/restoreChanges/restoreCourse/exams/event/saveEvent/addEvent/deleteEvent/
restoreEvent/conflicts/daySwaps/addDaySwap/removeDaySwap/restoreAllJwxt/exportPreview/exportWakeup/notifyPlan`。

性能注意：桥是**单通道串行队列**，一次挂载里发多个重调用会排队。已用 `timetable.overview`（一次回 state+week）
+ ViewModel 60 秒新鲜度 + 静默刷新来压。

---

## 5. 未实现 / 待做

按建议优先级：

### 5.1 桌面小组件（用户明确推迟）

- 现状：全仓无 `AppWidgetProvider` / Glance，桥也没有 widget 快照方法。
- 参考实现：`lib/mini_apps/timetable/timetable_settings_page.dart:366-557`、
  `lib/mini_apps/timetable/timetable_reminder_guide.dart:57-100`、同文件 `903-1149`（小组件配置）。
- 建议路径：桥新增 `timetable.widgetSnapshot`（当前周 + 今天剩余课程 + 明日），
  用 `GlanceAppWidget` + 一个配置 Activity（周/尺寸/条目数），设置页加回「桌面小组件」组。
- 用户口径：先做别的（公告页、校园地图完善），再回来做小组件。

### 5.2 同步后的「变动摘要」弹层

- 现状：`grep "变动摘要|changeAlert"` 零命中。桥已有 `timetable.changes`。
- 参考实现：`lib/mini_apps/timetable/timetable_change_alert.dart:11-40`（按 fingerprint 只弹一次）。
- 做法：同步成功后取 changes，非空则弹 MD3E 底部弹层（数量 + 前几条 + 「查看变动记录」）。

### 5.3 课表表单吞掉预测性返回

- 现状：`CourseFormPage.kt:141`、`EventFormPage.kt:104`、`ExamDetailPage.kt:88`、
  `TimetableExportPage.kt:106`、`TimetableDialogPage.kt:49` 都是
  `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))` —— 全屏 Dialog 拿不到返回手势/预测性返回。
- 参考实现用 `MaterialPageRoute` 整页路由。
- 做法：改成 `Route` 页面（NavDisplay entry），返回走返回栈并带上预测性返回动画；
  涉及「从课表页打开表单再回来刷新本周」的状态回传。

### 5.4 变动记录 / 删除范围文案粒度

- 现状：`TimetableChangeLogPage.kt` 文案较粗（直接拼字段）。
- 参考实现：`lib/mini_apps/timetable/timetable_change_format.dart:186-231`、
  `lib/mini_apps/timetable/timetable_delete_scope_sheet.dart:17-31`。

### 5.5 明日提醒不聚焦「明天」

- 现状：`ui/activity/TimetableLaunchRoute.kt:65` 只 `push(Route.Timetable)`，不带周次/日期。
- 做法：给路由加参数（或复用 `Route.TimetableFocus`），明日提醒跳到「明天」所在周并定位。

### 5.6 诊断页未接线

- 现状：`ui/page/nuist/diagnostics/DiagnosticsViewModel.kt` 存在，但没有任何 UI 引用它。
- 用户要求：「详细报错信息不要在页面上展示，做进诊断与日志」。所以需要一个诊断页（日志/最近同步/桥错误）从设置进入。

### 5.7 零散待清理（需先向用户列清单获批）

- 桥方法 `exam.fetch` 已无 Kotlin 调用方（考试页改走 `timetable.exams`）。
- `data/timetable/TimetablePreferences.kt` 里可能有历史遗留字段未被 UI 使用。
- 首页卡已不再显示「今日共 N 节」「第 x-y 节」等旧文案，确认是否要补。

---

## 6. 待用户拍板

1. 首页卡是否还需要「今日共 N 节」这类汇总（当前每节课一张卡，没做汇总）。
2. 通知点击：现在用 `push`（栈变成 Main → Timetable，可返回），是否要改成「替换栈顶清中间页」。
3. 明日提醒是否必须落到「明天」（见 5.5）。
4. 桌面小组件何时返工（用户说等公告页、校园地图做完之后）。

已明确**不做**：其他栏无节次课「补排」（上游同样保存不了）、配色方案 A/B/C、学期周数/开学日手改、
格子圆角、`[DEBUG] 清空本地课表`、横向周条（与「点周次卡选周」冲突）、「上课提醒」开关（改为权限可见性表达）。

---

## 7. 需要人工验收（不要自己点页面）

- 课表管理三页：整天调课 / 变动记录 / 恢复教务课程
- 提醒：上课提醒、明日提醒、Android 13 通知权限与精确闹钟的授权流程
- 后台运行设置页、WakeUp 导出页
- 拖拽调课、考试拖拽
- 课表列表视图
- 首页三卡 + 点击定位描边
- 「其他」栏只读

---

## 8. 已知坑（踩过的）

1. **进课表页转场卡顿**（已修）：根因不是数据加载，而是整页首次组合 ~100ms + 相邻周两张网格在动画中途组合。
   现做法：`WeekSwipeBox` 里相邻周 `delay(420)` 后再铺、`onDragStart` 立即补；`CellTextLayout` 加排版 LRU 缓存。
   打点工具（`TTPERF`）已删除，需要时按同样方式临时加。
2. **桥是串行队列**：不要在一次挂载里连发多个重调用；优先用 `timetable.overview` 合并。
3. **周次**：学期固定 20 周（`kDefaultTotalWeeks = 20`，`timetable_controller.dart:644/652`），
   开学日来自教务 `dqzc`；教务不提供总周数，别去猜。
4. **颜色是课程级**：`course_colors.json`，16 组调色板（`kCoursePaletteSize` ↔ `courseColors()`）；
   换色后必须重取本周（`TimetableViewModel.setCourseColor` 里 `reloadVisibleWeek()`），否则格子颜色不变。
5. **字体**：Material Symbols 字体已子集化到 ~222 KB（原 15.2 MB），新增图标要重新子集化，否则图标空白。
6. **baseline profile 产物**（`compose/app/src/*/generated/baselineProfiles/`）已 gitignore，不要提交。
7. **工具链 `$HOME` 被重定向**：找 keystore、adb key、gradle 缓存都去 `toolchain/` 下找，不在 `~`。
8. **节气/时间**：格式化日期统一用 `java.time.LocalDate`/`DateTimeFormatter`，别手拼。

---

## 9. 常用命令速查

```bash
# 只编译 Kotlin
cd compose && source ../../toolchain/env.sh && ./gradlew :app:compileStableDebugKotlin --offline

# 编译 + 打包 + 装机
./gradlew :app:assembleStableDebug --offline
adb -s 3B164V00ZUX00000 install -r app/build/outputs/apk/Stable/debug/app-Stable-arm64-v8a-debug.apk
adb -s 3B164V00ZUX00000 shell dumpsys package dev.duohuo.nuist_sta_app.compose | grep lastUpdateTime

# 改过 dart_bridge 后
cd dart_bridge && source ../../toolchain/env.sh && flutter analyze --no-pub && \
  flutter build aar --release --target-platform android-arm64,android-x64
```

远端：`https://github.com/C-Fuego/nuist-sta-app`（本机 remote 走 `v6.gh-proxy.org` 前缀的 HTTPS；
推送可用 `git push ssh://git@ssh.github.com:443/C-Fuego/nuist-sta-app.git main`，22 端口在这台机器被拒）。
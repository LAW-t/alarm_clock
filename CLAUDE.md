# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

倒班闹钟 Android 应用（包名 `com.example.alarm_clock_2`，版本 0.9.6），为倒班工人自动生成闹钟。支持三种排班类型：长白班（LONG_DAY）、四班三运转（FOUR_THREE）、四班两运转（FOUR_TWO）。

## 构建与测试命令

```bash
./gradlew assembleDebug              # 构建 Debug APK
./gradlew installDebug               # 安装到已连接设备
./gradlew compileDebugKotlin         # 仅编译检查（比完整构建更快）
./gradlew testDebugUnitTest          # 运行所有 JVM 单元测试
./gradlew testDebugUnitTest --tests "*ShiftCalculator*"  # 运行单个测试类
./gradlew connectedAndroidTest       # 运行所有仪器测试（需要设备/模拟器）
```

CI（`.github/workflows/android.yml`）：JDK 17 + `assembleDebug` + `testDebugUnitTest` + 上传 APK artifact。

## 技术栈

- **Kotlin 2.0.21** + **Compose compiler plugin**（非 deprecated `kotlinCompilerExtensionVersion`）
- **Jetpack Compose** + Material3 + Navigation Compose
- **Hilt** 依赖注入（含 `@HiltViewModel`、`@HiltWorker`、`@HiltAndroidApp`）
- **Room** 数据库（v5，含 3 个 migration）
- **DataStore** Preferences 存储用户设置
- **WorkManager** 后台周期任务
- **AlarmManager**（`setAlarmClock`）精准闹钟调度
- **OkHttp 4.12** 网络请求
- **kotlinx.serialization** JSON 解析
- **coreLibraryDesugaring** 提供 `java.time` API
- **com.nlf.calendar** 中国农历库
- **WheelPickerCompose** 时间滚轮选择器

依赖版本通过 `gradle/libs.versions.toml` 统一管理。第三方库通过 JitPack 获取。

## 架构（Clean Architecture + MVVM + UseCase）

```
UI (Compose Screens + ViewModels)
  ↕
Domain (UseCases: AlarmUseCase, AlarmScheduleUseCase)
  ↕
Data (Repositories → Room DAOs / DataStore)
  ↕
Infrastructure (AlarmManager, WorkManager, Hilt)
```

### 核心层次

**表现层** — `ui/` 目录：
- 三个主要界面：`CalendarScreen`、`AlarmsScreen`、`SettingsScreen`，通过底部导航栏切换
- 每个 Screen 对应一个 `@HiltViewModel`：`CalendarViewModel`、`AlarmsViewModel`、`SettingsViewModel`
- `MainActivity` 提供 `LocalWindowSizeClass`（Material3），锁定 `fontScale = 1f`
- 可复用组件在 `ui/components/`（`AlarmCard`、`AlarmEditBottomSheet`、`EmptyState`）

**领域层** — `domain/usecase/`：
- `AlarmUseCase`：闹钟 CRUD、默认闹钟创建、身份切换
- `AlarmScheduleUseCase`：调度/取消/重新调度闹钟

**数据层** — `data/`：
- 两个 Room Entity：`AlarmTimeEntity`（闹钟表）、`HolidayDayEntity`（节假日表）
- 两个 DAO：`AlarmDao`、`HolidayDao`
- 三个 Repository：`AlarmRepository`（写操作使用 `Mutex` 保护）、`HolidayRepository`（含 `syncedYears` 去重）、`CalendarRepository`（含 LRU 缓存，融合农历和节假日数据）
- `SettingsDataStore`：单一 DataStore 管理所有用户偏好（班制身份、班次索引、铃声、播放模式等），全部通过 Flow 暴露
- `AlarmDisplayItem`：UI 数据模型，包装 `AlarmTimeEntity` 并携带 `label` 和 `isCustom` 标记

**基础设施层**：
- `alarm/AlarmScheduler`：通过 `AlarmManager.setAlarmClock()` 调度闹钟；**NIGHT 班次特殊处理** — 在班次日期的前一天触发；最多搜索 60 天
- `alarm/AlarmReceiver`：`@AndroidEntryPoint` BroadcastReceiver，闹钟触发时获取 WakeLock、启动前台 `AlarmService`、发送通知、启动全屏 `AlarmActivity`
- `alarm/AlarmService`：前台服务（MEDIA_PLAYBACK 类型）；播放铃声时**强制最大音量 + 覆盖勿扰模式**；支持暂停（贪睡）和停止（关闭闹钟）；退出时恢复原始音量/响铃模式/勿扰设置
- `shift/ShiftCalculator`：静态工具类，根据 `ShiftConfig` 计算任意日期的班次
- `worker/`：三个 WorkManager Worker

### DI 模块（`di/AppModule.kt`）

`@Module @InstallIn(SingletonComponent)`，提供：
- `AppDatabase`（Room，含 migration）
- `AlarmDao`、`HolidayDao`
- `AlarmRepository`、`HolidayRepository`、`CalendarRepository`
- `AlarmUseCase`、`AlarmScheduleUseCase`

`AlarmScheduler` 和 `SettingsDataStore` 通过构造函数注入（不在此 Module 中）。

### 自定义 Result 类型（`util/Result.kt`）

项目自定义了 `sealed class Result<T>`（`Success`、`Error`、`Loading`、`Empty`），带有 `map`/`flatMap`/`onSuccess`/`onError` 扩展函数。Repository 的写操作全部返回此类型。`AlarmError` sealed class 定义了错误层级（`InvalidInput`、`DatabaseError`、`SchedulingError` 等）。

### Worker 系统

所有 Worker 在 `MyApplication.onCreate()` 或 `MainActivity.onCreate()` 中排队：
- `HolidaySyncWorker`（周期 24h）：从 GitHub/NateScarlet 拉取中国节假日数据
- `ShiftAlarmWorker`（周期 24h）：每日同步 — 根据当天和明天的班次启用/禁用闹钟
- `RescheduleAlarmsWorker`（一次性，5 秒延迟，REPLACE 策略）：重调度所有启用的闹钟（开机、应用启动时触发）
- `UpdateCheckWorker`（周期 24h，需网络）：检查 GitHub Release 更新

`MyApplication` 实现 `Configuration.Provider` 以自定义 WorkManager 初始化（使用 HiltWorkerFactory）。AndroidManifest 中移除了默认的 `InitializationProvider`。

### 更新系统（`update/`）

独立的更新检测和下载安装流程，使用独立的 DataStore（`update_preferences`）、OkHttp 请求 GitHub Releases API、系统 DownloadManager 下载 APK、FileProvider 安装。`UpdateViewModel` 以 1 秒轮询监控下载进度。

### SettingsDataStore 键值汇总

| Key | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `IDENTITY` | String | `LONG_DAY` | 班制身份 |
| `HOLIDAY_REST` | Boolean | 长白班=true,其他=false | 节假日休息开关 |
| `FOUR3_INDEX` | Int | 0 | 四三班基准索引 (0-7) |
| `FOUR2_INDEX` | Int | 0 | 四二班基准索引 (0-3) |
| `FOUR3_BASE_DATE` | String | 今天 | 四三班基准日期 |
| `FOUR2_BASE_DATE` | String | 今天 | 四二班基准日期 |
| `RINGTONE_URI` | String | `""` | 闹钟铃声 URI |
| `PLAY_MODE` | String | `SOUND` | 播放模式 |
| `SNOOZE_COUNT` | Int | 3 | 贪睡次数 |
| `SNOOZE_INTERVAL` | Int | 5 | 贪睡间隔(分钟) |

## 关键设计决策

1. **NIGHT 班次触发逻辑**：NIGHT 班次在班次日期的**前一天**触发闹钟（因为夜班在头一天晚上就需要起床/出门）。`AlarmScheduler.computeNextTriggerMillisForShift()` 中实现。

2. **节假日覆盖**：长白班模式下，节假日闹钟不响（`isOffDay=true` 时返回 OFF），但法定调休工作日（`workdayOverride`）按工作日处理。

3. **闹钟重调度**：`AlarmReceiver` 收到触发后立即重调度所有启用的闹钟，确保在没有打开应用的情况下闹钟持续工作。

4. **Hilt EntryPoint**：部分组件（Worker、BroadcastReceiver）不使用标准 Hilt 注入，而是通过 `EntryPointAccessors.fromApplication()` 获取依赖。

5. **数据库 Migration**：Room 数据库从 v2 升级到 v5，保留完整 migration 链（非 `fallbackToDestructiveMigration()`，虽然声明了这一选项作为兜底），确保用户数据不丢失。

6. **CalendarRepository 预热**：月份缓存切换时，自动在后台预加载该年所有 12 个月的数据。

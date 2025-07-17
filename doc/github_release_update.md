# GitHub 发布 & 自动更新指引

本文档说明了如何在 GitHub 上发布 **三班倒闹钟** 的新版本，使应用内“检查更新”功能可以自动获取版本号并下载安装最新 APK。

---

## 1. 准备签名 APK

1. 打开 **Android Studio** → `Build › Generate Signed Bundle / APK…`。
2. 选择 **APK**，构建变体选 **release**，使用正式 keystore 签名。
3. 构建完成后得到 `app-release.apk`（可重命名为 `alarm_clock_vX.Y.Z.apk`）。

> 目前应用内 Updater 仅识别 **`.apk`** 资源；AAB 如需发布可附加上传，但不会被自动下载。

---

## 2. 版本号 & Tag 规范

| 位置 | 示例 | 说明 |
|------|------|------|
| `build.gradle` → `versionName` | `1.2.3` | 代码中的版本号，**必须提升** 才会触发更新 |
| GitHub **Tag** | `v1.2.3` 或 `1.2.3` | `Updater` 通过 `/releases/latest` 读取 `tag_name` 作为“最新版本” |
| APK 文件名 | `alarm_clock_v1.2.3.apk` | 建议包含版本号，易于识别（可不强制） |

三处版本号需保持一致；否则可能出现提示异常或下载后覆盖失败。

---

## 3. 发布 Release 步骤

1. 进入仓库 → **Releases** → `Draft a new release`。
2. **Tag version**：输入 `v1.2.3`（或 `1.2.3`）。
3. **Release title**：填写版本标题，如 `1.2.3`。
4. **Description**：填写更新日志。
5. **Attach binaries**：拖入 **已签名 APK** 文件。
6. 确认 **不要勾选** `This is a pre-release`，点击 **Publish release**。

> 公开仓库或公开 Release 可直接被 Updater 访问。私有仓库需修改 Updater 以携带 Token。

---

## 4. Updater 工作流程

1. 应用调用 `https://api.github.com/repos/LAW-t/alarm_clock/releases/latest` 获取 JSON。
2. 解析 `tag_name` → 最新版本号。与本地 `versionName` 比较。
3. 遍历 `assets`，找到第一个以 `.apk` 结尾的 **`browser_download_url`**。
4. 通过 `DownloadManager` 下载到 `Download/` 目录，下载完成后自动触发安装页。

> 需要在系统中允许“安装未知来源应用”，并已在 `AndroidManifest.xml` 中声明 `REQUEST_INSTALL_PACKAGES` 权限。

---

## 5. 常见问题 & 解决方案

| 问题 | 可能原因 | 解决办法 |
|------|----------|----------|
| App 显示“检查中…”且长时间无响应 | GitHub API 速率限制（未登录 60 次/小时/IP） | 等待恢复或在 Updater 中添加个人访问令牌（PAT） |
| 点击更新后下载失败 | Release 未包含 `.apk`；或文件 >2 GB | 核对 Release 资源，确认文件类型与大小 |
| 安装界面未弹出 | 未授予“允许安装未知应用”权限 | 在系统设置中允许当前应用安装 APK |
| 无法获取最新版本 | Release 被标记为 Pre-release | 取消勾选或发布正式版 |

---

## 6. 发布清单检查

- [ ] `versionName` & `Tag` & APK 命名一致
- [ ] Release **Public** 且非 Pre-release
- [ ] 至少包含 **一个 .apk** 资产
- [ ] APK 已使用正式 keystore 签名，可独立安装

完成上述步骤后，应用内“应用版本”弹窗将正确显示最新版本号，并可一键下载安装。 
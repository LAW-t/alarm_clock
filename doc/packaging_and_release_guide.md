# 打包与发布流程

本文档描述了如何打包 Android 应用并创建正式的 GitHub Release。

## 第一步：构建 Release APK

APK (Android Package Kit) 是 Android 用来分发和安装移动应用的文件格式。要构建应用的发布版本，请按照以下步骤操作：

1.  在你的项目根目录中打开一个终端或命令提示符。
2.  运行以下 Gradle 命令：

    ```bash
    ./gradlew assembleRelease
    ```

    此命令会编译你的源代码、处理资源，并将它们打包成一个单独的 APK 文件。这个 APK 是未签名的，这意味着它还没有准备好通过官方应用商店进行公开发布，但它非常适合内部测试或直接分发。

3.  构建成功完成后，你可以在以下目录中找到生成的 APK：

    `app/build/outputs/apk/release/app-release-unsigned.apk`

## 第二步：对 APK 进行签名（分发所必需）

为了将你的应用分发给用户，你必须使用发布密钥对其进行签名。此步骤对于验证你作为开发者的身份和确保应用的完整性至关重要。

> **重要提示:** 你需要生成一个私有的签名密钥。**绝对不要**将你的签名密钥提交到 Git 仓库或公开分享。

一旦你有了密钥库，你需要配置应用的 `build.gradle.kts` 文件，以便在构建过程中使用它。这通常通过在你的 `local.properties` 文件（该文件应在 `.gitignore` 中列出）中安全地存储密钥凭据来完成。

有关如何生成密钥和为应用签名的详细说明，请参阅 [官方 Android 开发者文档](https://developer.android.com/studio/publish/app-signing)。

## 第三步：在 Git 中为版本打上标签

在 GitHub 上创建发布之前，一个好的做法是创建一个带注释的 Git 标签，以标记项目历史中与此发布相对应的确切点。

1.  确保你所有的更改都已提交。
2.  运行以下命令来创建一个新标签（用你的版本号替换 `v1.1.0`）：

    ```bash
    git tag -a v1.1.0 -m "版本 1.1.0 发布"
    ```

3.  将标签推送到远程仓库：

    ```bash
    git push origin v1.1.0
    ```

## 第四步：在 GitHub 上创建 Release

现在你已经准备好在 GitHub 的网页界面上创建正式的发布了。

1.  在 GitHub 上导航到你的仓库。
2.  点击右侧的 **"Releases"** 标签页。
3.  点击 **"Draft a new release"** 按钮。
4.  **选择你的标签:** 在 "Choose a tag" 下拉菜单中，选择你刚刚推送的标签（例如 `v1.1.0`）。
5.  **填写发布详情:**
    *   **Release title:** 给一个有意义的标题，例如 "版本 1.1.0"。
    *   **Description:** 编写详细的发布说明。描述此版本中的新功能、错误修复以及任何其他重要更改。你可以使用 Markdown 进行格式化。
6.  **上传 APK 文件:**
    *   在 "Attach binaries by dropping them here or selecting them" 部分，拖放你已签名的 APK 文件（例如 `app-release.apk`）或点击从你的计算机中选择它。
7.  **发布 Release:**
    *   检查所有信息。如果一切正确，点击 **"Publish release"** 按钮。

你的发布现在已经上线了！用户可以访问发布页面来阅读有关更改并下载 APK 文件。 
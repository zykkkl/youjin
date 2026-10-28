# 发布与自动更新指南（GitHub 托管）

> 目标：用 GitHub 托管 APK 与版本清单（manifest），让 App 自动发现新版本、通知用户并引导下载安装。

## 0. 先明确"自动"到什么程度

| 能力 | 是否可行 | 说明 |
| --- | --- | --- |
| 后台自动检查是否有新版本 | ✅ | 每天检查一次（需联网），有新版发通知 |
| App 内提示 + 引导下载安装 | ✅ | 用户点「去下载」，浏览器下载 APK，再点安装确认 |
| **静默自动安装** | ❌ | Android 禁止普通应用静默安装；只有应用商店（Play/国内商店）能静默更新 |

所以 GitHub 方案 = "自动提醒 + 一键下载"，最后一步安装需要用户确认（点"安装"按钮）。

---

## 1. 一次性准备：生成签名密钥（最重要）

覆盖安装要求新旧 APK **同一签名**；密钥丢失后老用户无法升级（只能卸载重装，丢失数据）。

在项目根目录执行（Windows，JDK 已自带 keytool）：

```bat
keytool -genkeypair -v ^
  -keystore youjin-release.jks ^
  -alias youjin ^
  -keyalg RSA -keysize 2048 -validity 10000 ^
  -storepass 你的密码 -keypass 你的密码 ^
  -dname "CN=Youjin, OU=Personal, O=Youjin, L=Shanghai, ST=Shanghai, C=CN"
```

> PowerShell 中把行尾 `^` 换成反引号 `` ` ``，或整条命令写成一行。

然后复制 `keystore.properties.example` 为 `keystore.properties`，填写：

```properties
storeFile=youjin-release.jks
storePassword=你的密码
keyAlias=youjin
keyPassword=你的密码
```

这两个文件都已在 `.gitignore` 中，**不要提交**。密钥文件请另存备份。

## 2. 构建 release APK

```bat
gradlew.bat assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`
建议改名为 `youjin-0.1.5.apk`（与 latest.json 里的 apkUrl 一致）。

## 3. 把项目传到 GitHub

```bat
git init
git add .
git commit -m "youjin 0.1.5"
git branch -M main
git remote add origin https://github.com/你的用户名/你的仓库.git
git push -u origin main
```

建议用**公开仓库**（私有仓库的 Pages 需要付费计划，且 raw 文件无法匿名访问）。

## 4. 开启 GitHub Pages（托管版本清单）

仓库 → **Settings → Pages**：
- Source：`Deploy from a branch`
- Branch：`main`，目录选 **`/docs`** → Save

约 1 分钟后可访问：
`https://你的用户名.github.io/你的仓库/latest.json`

（`docs/latest.json` 已在项目中，先换成真实内容。）

## 5. 上传 APK 到 Release

仓库 → **Releases → Draft a new release**：
- Tag：`v0.1.5`
- 上传 `youjin-0.1.5.apk`
- Publish

下载直链格式：
`https://github.com/你的用户名/你的仓库/releases/download/v0.1.5/youjin-0.1.5.apk`

## 6. 更新版本清单 docs/latest.json

```json
{
  "versionCode": 6,
  "versionName": "0.1.5",
  "apkUrl": "https://github.com/你的用户名/你的仓库/releases/download/v0.1.5/youjin-0.1.5.apk",
  "notes": "本次更新：长按目标归档/删除、自动检查更新。"
}
```

- `versionCode` 必须**严格大于**已安装版本的 versionCode（App 用它比较版本）
- `apkUrl` 必须是可直接下载 APK 的 HTTPS 地址
- 改完 `git push`，Pages 约 1–2 分钟生效

## 7. App 端启用更新源

编辑 `app/src/main/java/com/youjin/app/data/UpdateChecker.kt`：

```kotlin
object UpdateConfig {
    const val MANIFEST_URL = "https://你的用户名.github.io/你的仓库/latest.json"
}
```

重新构建并安装这一版——**这是最后一次手动安装**。之后：
- 打开 App → 设置 → 关于 →「检查更新」可手动检查
- 「自动检查更新」默认开启：每天后台检查一次，发现新版本会发通知（需通知权限）
- 发现新版本时弹窗显示更新说明，点「去下载」打开浏览器下载，再手动点安装

## 8. 以后每次发版的清单

1. 改 `app/build.gradle.kts`：`versionCode + 1`、`versionName` 更新
2. `gradlew.bat assembleRelease`
3. GitHub 新建 Release，tag `v<versionName>`，上传新 APK
4. 更新 `docs/latest.json`（versionCode / versionName / apkUrl / notes）
5. `git push` → 等 1–2 分钟生效
6. 老用户次日（或手动检查时）收到更新提示

## 9. 常见问题

| 问题 | 处理 |
| --- | --- |
| 安装时提示"应用未安装" | 签名与已安装版本不一致：必须用同一个 keystore 出包 |
| 检查更新一直显示"未配置更新源" | `UpdateConfig.MANIFEST_URL` 为空，填好后重新打包 |
| 检查更新失败/超时 | 网络不通、仓库是私有的、或 manifest 地址错；GitHub 国内访问不稳定时可在 `apkUrl` 用国内对象存储 |
| 改了 latest.json 但 App 没反应 | Pages CDN 有 1–2 分钟缓存；确认 versionCode 已增大 |
| 后台自动检查没收到通知 | Android 13+ 需开启通知权限；系统省电策略也会延迟后台任务 |
| 首次安装被拦 | 需在系统设置里允许浏览器/文件管理器"安装未知应用" |

## 10. 可选：GitHub Actions 自动构建

如果后续想"打 tag 自动出包"，可加 GitHub Actions 工作流（需把签名密钥存为 Secrets）。首版手动流程已足够。

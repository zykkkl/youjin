package com.youjin.app.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查更新（自建更新源）。
 *
 * 机制：应用读取一个 HTTPS 上的 manifest JSON；若其中的 versionCode 大于当前安装版本，
 * 提示用户前往下载新 APK（由系统浏览器/下载器完成，安装仍需用户确认——
 * Android 不允许普通应用静默自装）。
 *
 * manifest 示例（部署在你的 HTTPS 站点）：
 * {
 *   "versionCode": 5,
 *   "versionName": "0.1.4",
 *   "apkUrl": "https://example.com/youjin/youjin-0.1.4.apk",
 *   "notes": "修复若干问题，新增长按操作"
 * }
 *
 * 注意：
 * - 覆盖安装要求新旧 APK 使用同一签名密钥（签名不同会提示“应用未安装”）
 * - 服务器需为 HTTPS（Android 9+ 默认禁止明文 HTTP）
 */
object UpdateConfig {
    /** manifest 地址：由 GitHub Pages 托管（仓库 docs/latest.json） */
    const val MANIFEST_URL = "https://zykkkl.github.io/youjin/latest.json"
}

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
)

class UpdateChecker(private val context: Context) {

    data class Result(
        val currentVersionName: String,
        /** 有可用更新时为最新版本信息，否则为 null */
        val latest: UpdateInfo?,
        /** 是否已配置更新源 */
        val configured: Boolean,
    )

    @Suppress("DEPRECATION")
    suspend fun check(): Result = withContext(Dispatchers.IO) {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentCode = if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode.toInt() else pkg.versionCode
        val currentName = pkg.versionName ?: "0"

        if (UpdateConfig.MANIFEST_URL.isBlank()) {
            return@withContext Result(currentName, null, configured = false)
        }

        val json = JSONObject(httpGet(UpdateConfig.MANIFEST_URL))
        val latest = UpdateInfo(
            versionCode = json.optInt("versionCode", 0),
            versionName = json.optString("versionName", ""),
            apkUrl = json.optString("apkUrl", ""),
            notes = json.optString("notes", ""),
        )
        val available = latest.versionCode > currentCode && latest.apkUrl.isNotBlank()
        Result(currentName, if (available) latest else null, configured = true)
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            if (conn.responseCode !in 200..299) error("更新源返回 HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

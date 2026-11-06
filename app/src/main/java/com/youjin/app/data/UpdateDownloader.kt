package com.youjin.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * 应用内下载更新包。
 *
 * 不依赖浏览器：有些设备没有可处理 https 链接的应用，直接 ACTION_VIEW 会失败。
 * 这里交给系统 DownloadManager 下载到应用专属目录（无需存储权限），
 * 下载完成后由界面引导安装（Android 8+ 需允许「安装未知应用」）。
 */
object UpdateDownloader {

    data class Progress(
        val status: Int,
        val bytesSoFar: Long,
        val totalBytes: Long,
    ) {
        val percent: Int
            get() = if (totalBytes > 0) ((bytesSoFar * 100) / totalBytes).toInt().coerceIn(0, 100) else -1

        val done: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL
        val failed: Boolean get() = status == DownloadManager.STATUS_FAILED
    }

    fun start(context: Context, info: UpdateInfo): Long {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val file = fileFor(context, info)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("有进 ${info.versionName}")
            .setDescription("正在下载更新")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "updates/${fileName(info)}",
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        return manager.enqueue(request)
    }

    fun query(context: Context, downloadId: Long): Progress? {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Progress(
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                bytesSoFar = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                ),
                totalBytes = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                ),
            )
        }
        return null
    }

    fun fileFor(context: Context, info: UpdateInfo): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates/${fileName(info)}")

    private fun fileName(info: UpdateInfo): String = "youjin-${info.versionName}.apk"
}

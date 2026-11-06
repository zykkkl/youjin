package com.youjin.app.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// 单独的 DataStore 文件，避免与设置页的 settings 冲突
private val Context.aiDataStore by preferencesDataStore(name = "ai_settings")

/**
 * API Key 加密：优先使用 Android Keystore 中的 AES-GCM 密钥（Android 6.0+ 由系统保护）。
 * Keystore 不可用时退化为应用私有目录中的 Base64（带 raw: 前缀），仅作为兜底，不用于联网传输之外的任何场景。
 */
internal object AiKeyCipher {

    private const val KEY_ALIAS = "youjin_ai_key"
    private const val PREFIX = "v1:"
    private const val RAW_PREFIX = "raw:"

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(iv + body, Base64.NO_WRAP)
        }.getOrElse {
            RAW_PREFIX + Base64.encodeToString(plain.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        return runCatching {
            when {
                stored.startsWith(RAW_PREFIX) ->
                    String(Base64.decode(stored.removePrefix(RAW_PREFIX), Base64.NO_WRAP), Charsets.UTF_8)

                stored.startsWith(PREFIX) -> {
                    val bytes = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
                    val iv = bytes.copyOfRange(0, 12)
                    val body = bytes.copyOfRange(12, bytes.size)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
                    String(cipher.doFinal(body), Charsets.UTF_8)
                }

                else -> stored
            }
        }.getOrDefault("")
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}

/** AI 配置的本地存储（BYOK：Key 只在本机） */
class AiSettingsRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val PROVIDER = stringPreferencesKey("provider")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val API_KEY = stringPreferencesKey("api_key")
        val PRIVACY = booleanPreferencesKey("privacy_accepted")
    }

    val config: Flow<AiConfig> = context.aiDataStore.data.map { prefs ->
        val providerId = prefs[Keys.PROVIDER] ?: AiProviders.default.id
        val template = AiProviders.byId(providerId)
        AiConfig(
            enabled = prefs[Keys.ENABLED] ?: false,
            providerId = providerId,
            baseUrl = prefs[Keys.BASE_URL] ?: template?.baseUrl.orEmpty(),
            model = prefs[Keys.MODEL] ?: template?.defaultModel.orEmpty(),
            apiKey = runCatching { AiKeyCipher.decrypt(prefs[Keys.API_KEY].orEmpty()) }.getOrDefault(""),
            privacyAccepted = prefs[Keys.PRIVACY] ?: false,
        )
    }

    suspend fun saveFields(providerId: String, baseUrl: String, model: String, apiKey: String) {
        context.aiDataStore.edit { prefs ->
            prefs[Keys.PROVIDER] = providerId
            prefs[Keys.BASE_URL] = AiProviders.normalizeBaseUrl(baseUrl)
            prefs[Keys.MODEL] = model.trim()
            prefs[Keys.API_KEY] = AiKeyCipher.encrypt(apiKey.trim())
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.aiDataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setPrivacyAccepted(accepted: Boolean) {
        context.aiDataStore.edit { it[Keys.PRIVACY] = accepted }
    }
}

package com.webhook.sender

import com.google.gson.Gson
import com.webhook.sender.model.WebhookPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class WebhookApi {

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        private val gson = Gson()
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        private const val USER_AGENT = "DiscordWebhookSender-Android/1.0"
    }

    suspend fun sendWebhook(webhookUrl: String, payload: WebhookPayload): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(payload)
                val body = json.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    when {
                        response.isSuccessful || response.code == 204 -> {
                            Result.success(if (responseBody.isEmpty()) "訊息已成功發送！" else responseBody)
                        }
                        response.code == 400 -> {
                            Result.failure(Exception("發送內容格式錯誤 (HTTP 400)\n詳細資訊: $responseBody"))
                        }
                        response.code == 404 -> {
                            Result.failure(Exception("找不到此 Webhook URL (HTTP 404)\n請檢查 URL 是否正確或該 Webhook 是否已被刪除。"))
                        }
                        response.code == 429 -> {
                            Result.failure(Exception("遇到 Discord 速率限制 (HTTP 429 Rate Limited)\n請稍後再嘗試發送。\n詳細資訊: $responseBody"))
                        }
                        response.code in 500..599 -> {
                            Result.failure(Exception("Discord 伺服器錯誤 (HTTP ${response.code})\n請稍後再試。"))
                        }
                        else -> {
                            Result.failure(Exception("發送失敗 (HTTP ${response.code}): $responseBody"))
                        }
                    }
                }
            } catch (e: UnknownHostException) {
                Result.failure(Exception("無法連線到 Discord 伺服器，請檢查網路連線或 DNS 設定。"))
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("連線逾時 (Timeout)，請檢查網路連線狀態或稍後再試。"))
            } catch (e: SSLException) {
                Result.failure(Exception("SSL/TLS 安全連線失敗: ${e.localizedMessage}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

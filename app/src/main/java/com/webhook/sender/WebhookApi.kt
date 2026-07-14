package com.webhook.sender

import com.google.gson.Gson
import com.webhook.sender.model.WebhookPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

class WebhookApi {

    companion object {
        // Certificate Pinning for discord.com and discordapp.com
        // Pins are SPKI SHA-256 hashes of public keys (leaf + intermediate + root)
        // These pins should be reviewed periodically as Discord rotates certificates.
        // Fallback to system CA trust is removed by pinning; if pinning fails, connection is aborted.
        // To update pins: run openssl command to extract SPKI hashes from current certs.
        private val certificatePinner = CertificatePinner.Builder()
            // discord.com pins (leaf, intermediate, root)
            .add("discord.com", "sha256/v+k/7KsiyR0zaVWxsgnD3ohO7cVMwj+c3XHd5GKLjV4=")
            .add("discord.com", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
            .add("discord.com", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
            // discordapp.com pins (leaf, intermediate, root)
            .add("discordapp.com", "sha256/SBWXn/GAK4nczFrWYO0Rjr+pCl/CzAqpFSC9H7WEbNY=")
            .add("discordapp.com", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
            .add("discordapp.com", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
            .build()

        private val client = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
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
                require(WebhookUrlValidator.isAllowed(webhookUrl)) { "只允許標準 Discord Webhook URL。" }
                val json = gson.toJson(payload)
                val body = json.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful || response.code == 204 -> {
                            Result.success("訊息已成功發送！")
                        }
                        response.code == 400 -> {
                            Result.failure(Exception("發送內容格式錯誤 (HTTP 400)。"))
                        }
                        response.code == 404 -> {
                            Result.failure(Exception("找不到此 Webhook URL (HTTP 404)\n請檢查 URL 是否正確或該 Webhook 是否已被刪除。"))
                        }
                        response.code == 429 -> {
                            Result.failure(Exception("遇到 Discord 速率限制 (HTTP 429)，請稍後再嘗試發送。"))
                        }
                        response.code in 500..599 -> {
                            Result.failure(Exception("Discord 伺服器錯誤 (HTTP ${response.code})\n請稍後再試。"))
                        }
                        else -> {
                            Result.failure(Exception("發送失敗 (HTTP ${response.code})。"))
                        }
                    }
                }
            } catch (e: SSLPeerUnverifiedException) {
                Result.failure(Exception("憑證綁定驗證失敗 (Certificate Pinning Failure)，可能為中間人攻擊或憑證已輪替，請更新應用程式後重試。"))
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

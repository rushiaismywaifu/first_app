package com.webhook.sender

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.webhook.sender.databinding.ActivityMainBinding
import com.webhook.sender.model.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val webhookApi = WebhookApi()
    private val fieldAdapter = EmbedFieldAdapter(mutableListOf())

    companion object {
        private const val PREFS_NAME = "WebhookSenderPrefs"
        private const val KEY_WEBHOOK_URL = "saved_webhook_url"
        private const val KEY_USERNAME = "saved_username"
        private const val KEY_AVATAR_URL = "saved_avatar_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupViews()
        loadSavedData()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViews() {
        // RecyclerView for embed fields
        binding.rvEmbedFields.adapter = fieldAdapter
        binding.rvEmbedFields.isNestedScrollingEnabled = false

        // Toggle embed section
        binding.switchEnableEmbed.setOnCheckedChangeListener { _, isChecked ->
            binding.embedSection.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Paste URL from clipboard button
        binding.btnPasteUrl.setOnClickListener {
            pasteUrlFromClipboard()
        }

        // Add field button
        binding.btnAddField.setOnClickListener {
            fieldAdapter.addField()
        }

        // Color picker helper
        binding.btnPickColor.setOnClickListener {
            showColorPickerDialog()
        }

        // Send button
        binding.btnSend.setOnClickListener {
            sendWebhook()
        }

        // Preview JSON
        binding.btnPreview.setOnClickListener {
            showJsonPreview()
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            clearForm()
        }
    }

    private fun loadSavedData() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.etWebhookUrl.setText(prefs.getString(KEY_WEBHOOK_URL, ""))
        binding.etUsername.setText(prefs.getString(KEY_USERNAME, ""))
        binding.etAvatarUrl.setText(prefs.getString(KEY_AVATAR_URL, ""))
    }

    private fun saveInputData(webhookUrl: String, username: String?, avatarUrl: String?) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_WEBHOOK_URL, webhookUrl)
            .putString(KEY_USERNAME, username ?: "")
            .putString(KEY_AVATAR_URL, avatarUrl ?: "")
            .apply()
    }

    private fun pasteUrlFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pastedText = clip.getItemAt(0).text?.toString()?.trim()
            if (!pastedText.isNullOrEmpty()) {
                binding.etWebhookUrl.setText(pastedText)
                binding.etWebhookUrl.setSelection(pastedText.length)
                Toast.makeText(this, "📋 已貼上 Webhook URL", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ 剪貼簿為空", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "⚠️ 剪貼簿沒有內容", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showColorPickerDialog() {
        val colors = intArrayOf(
            0x5865F2, 0x57F287, 0xFEE75C, 0xEB459E, 0xED4245,
            0xFF0000, 0x00FF00, 0x0000FF, 0xFFA500, 0x800080,
            0x00FFFF, 0xFF69B4, 0x008000, 0xFFD700, 0x4169E1
        )
        val colorNames = arrayOf(
            "Blurple (Discord)", "Green (Discord)", "Yellow (Discord)", "Fuchsia (Discord)", "Red (Discord)",
            "Pure Red", "Pure Green", "Pure Blue", "Orange", "Purple",
            "Cyan", "Hot Pink", "Dark Green", "Gold", "Royal Blue"
        )

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("🎨 選擇 Embed 顏色")
        builder.setItems(colorNames) { _, which ->
            binding.etEmbedColor.setText(String.format("#%06X", colors[which]))
        }
        builder.setNegativeButton("取消", null)

        // Custom input option
        val input = EditText(this)
        input.hint = "自訂 HEX 色碼 (例如: #FF5865F2)"
        input.setPadding(64, 32, 64, 32)
        builder.setView(input)

        builder.setPositiveButton("自訂") { _, _ ->
            val customColor = input.text.toString().trim()
            if (customColor.isNotEmpty()) {
                val cleanColor = customColor.let { if (it.startsWith("#")) it else "#$it" }
                binding.etEmbedColor.setText(cleanColor.uppercase())
            }
        }

        builder.show()
    }

    private fun buildPayload(): WebhookPayload? {
        val username = binding.etUsername.text.toString().trim().ifEmpty { null }
        val avatarUrl = binding.etAvatarUrl.text.toString().trim().ifEmpty { null }
        val content = binding.etContent.text.toString().trim().ifEmpty { null }

        var embeds: List<Embed>? = null

        if (binding.switchEnableEmbed.isChecked) {
            fieldAdapter.updateFromViews(binding.rvEmbedFields)

            val embedTitle = binding.etEmbedTitle.text.toString().trim().ifEmpty { null }
            val embedDescription = binding.etEmbedDescription.text.toString().trim().ifEmpty { null }
            val embedColorStr = binding.etEmbedColor.text.toString().trim()
            val embedUrl = binding.etEmbedUrl.text.toString().trim().ifEmpty { null }
            val embedImageUrl = binding.etEmbedImageUrl.text.toString().trim().ifEmpty { null }
            val embedThumbnailUrl = binding.etEmbedThumbnailUrl.text.toString().trim().ifEmpty { null }
            val embedFooterText = binding.etEmbedFooterText.text.toString().trim().ifEmpty { null }
            val embedFooterIcon = binding.etEmbedFooterIcon.text.toString().trim().ifEmpty { null }
            val embedAuthorName = binding.etEmbedAuthorName.text.toString().trim().ifEmpty { null }
            val embedAuthorUrl = binding.etEmbedAuthorUrl.text.toString().trim().ifEmpty { null }
            val embedAuthorIcon = binding.etEmbedAuthorIcon.text.toString().trim().ifEmpty { null }

            var embedColor: Int? = null
            if (embedColorStr.isNotEmpty()) {
                val colorRegex = Regex("^#?([0-9a-fA-F]{1,6})$")
                val match = colorRegex.find(embedColorStr)
                if (match == null) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("⚠️ 色碼格式錯誤")
                        .setMessage("請輸入有效的 HEX 十六進位色碼，例如 #FF5865F2 或 5865F2。")
                        .setPositiveButton("確定", null)
                        .show()
                    return null
                }
                try {
                    val colorStr = match.groupValues[1]
                    embedColor = colorStr.toInt(16)
                } catch (e: Exception) {
                    // Ignore overflow or parse issue
                }
            }

            val fields = fieldAdapter.getFieldEntries()
                .filter { it.name.trim().isNotEmpty() && it.value.trim().isNotEmpty() }
                .map { EmbedField(it.name.trim(), it.value.trim(), it.inline) }
                .ifEmpty { null }

            val image = embedImageUrl?.let { EmbedMedia(it) }
            val thumbnail = embedThumbnailUrl?.let { EmbedMedia(it) }
            val footer = embedFooterText?.let { EmbedFooter(it, embedFooterIcon) }
            val author = embedAuthorName?.let { EmbedAuthor(it, embedAuthorUrl, embedAuthorIcon) }

            // If embed has at least one property set, construct it
            if (embedTitle != null || embedDescription != null || embedColor != null ||
                embedUrl != null || fields != null || image != null ||
                thumbnail != null || footer != null || author != null) {
                val embed = Embed(
                    title = embedTitle,
                    description = embedDescription,
                    color = embedColor,
                    url = embedUrl,
                    fields = fields,
                    image = image,
                    thumbnail = thumbnail,
                    footer = footer,
                    author = author
                )
                embeds = listOf(embed)
            }
        }

        if (content == null && embeds == null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ 訊息為空")
                .setMessage("無法發送空訊息！請填寫「訊息內容」或在「Embed 嵌入訊息」中設定至少一項內容。")
                .setPositiveButton("確定", null)
                .show()
            return null
        }

        return WebhookPayload(
            username = username,
            avatarUrl = avatarUrl,
            content = content,
            embeds = embeds
        )
    }

    private fun sendWebhook() {
        var webhookUrl = binding.etWebhookUrl.text.toString().trim()
        // Remove accidental quotes when copying/pasting
        webhookUrl = webhookUrl.removePrefix("\"").removeSuffix("\"").removePrefix("'").removeSuffix("'")

        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "⚠️ 請填寫 Webhook URL", Toast.LENGTH_SHORT).show()
            return
        }

        if (!webhookUrl.startsWith("https://discord.com/api/webhooks/") &&
            !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ URL 格式確認")
                .setMessage("您輸入的 URL 似乎不是標準 Discord Webhook 網址 (開頭通常為 https://discord.com/api/webhooks/)。確定要繼續嘗試發送嗎？")
                .setPositiveButton("繼續發送") { _, _ -> doSend(webhookUrl) }
                .setNegativeButton("檢查修改", null)
                .show()
            return
        }

        doSend(webhookUrl)
    }

    private fun doSend(webhookUrl: String) {
        val payload = buildPayload() ?: return

        // Auto save inputs on send attempt
        saveInputData(
            webhookUrl = webhookUrl,
            username = payload.username,
            avatarUrl = payload.avatarUrl
        )

        setButtonsEnabled(false)
        binding.btnSend.text = "發送中..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = webhookApi.sendWebhook(webhookUrl, payload)
            setButtonsEnabled(true)
            binding.btnSend.text = "🚀 發送"
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { responseText ->
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("✅ 發送成功")
                        .setMessage(responseText)
                        .setPositiveButton("確定", null)
                        .show()
                },
                onFailure = { error ->
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("❌ 發送失敗")
                        .setMessage(error.message ?: "未知錯誤")
                        .setPositiveButton("確定", null)
                        .show()
                }
            )
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnSend.isEnabled = enabled
        binding.btnPreview.isEnabled = enabled
        binding.btnClear.isEnabled = enabled
        binding.btnPasteUrl.isEnabled = enabled
        binding.btnAddField.isEnabled = enabled
    }

    private fun showJsonPreview() {
        val payload = buildPayload() ?: return
        val prettyJson = GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
            .toJson(payload)

        MaterialAlertDialogBuilder(this)
            .setTitle("📋 JSON 預覽")
            .setMessage(prettyJson)
            .setPositiveButton("複製到剪貼簿") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Webhook JSON", prettyJson)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "✅ 已複製到剪貼簿", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("關閉", null)
            .show()
    }

    private fun clearForm() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🗑️ 清除表單")
            .setMessage("確定要清除所有填寫的欄位與 Embed 設定嗎？(已儲存的 Webhook URL 也會從畫面上清空)")
            .setPositiveButton("確定清除") { _, _ ->
                binding.etWebhookUrl.text?.clear()
                binding.etUsername.text?.clear()
                binding.etAvatarUrl.text?.clear()
                binding.etContent.text?.clear()
                binding.switchEnableEmbed.isChecked = false
                binding.etEmbedTitle.text?.clear()
                binding.etEmbedDescription.text?.clear()
                binding.etEmbedColor.text?.clear()
                binding.etEmbedUrl.text?.clear()
                binding.etEmbedImageUrl.text?.clear()
                binding.etEmbedThumbnailUrl.text?.clear()
                binding.etEmbedFooterText.text?.clear()
                binding.etEmbedFooterIcon.text?.clear()
                binding.etEmbedAuthorName.text?.clear()
                binding.etEmbedAuthorUrl.text?.clear()
                binding.etEmbedAuthorIcon.text?.clear()
                fieldAdapter.clearFields()
                Toast.makeText(this, "🗑️ 已清除所有欄位", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

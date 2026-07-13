package com.webhook.sender

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.webhook.sender.databinding.ActivityMainBinding
import com.webhook.sender.model.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val webhookApi = WebhookApi()
    private val fieldAdapter = EmbedFieldAdapter(mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        // RecyclerView for embed fields
        binding.rvEmbedFields.adapter = fieldAdapter
        binding.rvEmbedFields.isNestedScrollingEnabled = false

        // Toggle embed section
        binding.switchEnableEmbed.setOnCheckedChangeListener { _, isChecked ->
            binding.embedSection.visibility = if (isChecked) View.VISIBLE else View.GONE
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

    private fun showColorPickerDialog() {
        val colors = intArrayOf(
            0x5865F2, 0x57F287, 0xFEE75C, 0xEB459E, 0xED4245,
            0xFF0000, 0x00FF00, 0x0000FF, 0xFFA500, 0x800080,
            0x00FFFF, 0xFF69B4, 0x008000, 0xFFD700, 0x4169E1
        )
        val colorNames = arrayOf(
            "Blurple", "Green", "Yellow", "Fuchsia", "Red",
            "Pure Red", "Pure Green", "Pure Blue", "Orange", "Purple",
            "Cyan", "Hot Pink", "Dark Green", "Gold", "Royal Blue"
        )

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("選擇 Embed 顏色")
        builder.setItems(colorNames) { _, which ->
            binding.etEmbedColor.setText(String.format("#%06X", colors[which]))
        }
        builder.setNegativeButton("取消", null)

        // Custom input option
        val input = EditText(this)
        input.hint = "自訂 HEX 色碼 (例如: #FF0000)"
        input.setPadding(48, 24, 48, 24)
        builder.setView(input)

        builder.setPositiveButton("自訂") { _, _ ->
            val customColor = input.text.toString().trim()
            if (customColor.isNotEmpty()) {
                binding.etEmbedColor.setText(customColor)
            }
        }

        builder.show()
    }

    private fun buildPayload(): WebhookPayload {
        val username = binding.etUsername.text.toString().trim().ifEmpty { null }
        val avatarUrl = binding.etAvatarUrl.text.toString().trim().ifEmpty { null }
        val content = binding.etContent.text.toString().trim().ifEmpty { null }

        var embeds: List<Embed>? = null

        if (binding.switchEnableEmbed.isChecked) {
            // Sync field data from views
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
                try {
                    val colorStr = embedColorStr.removePrefix("#")
                    embedColor = colorStr.toInt(16)
                } catch (e: Exception) {
                    // Ignore invalid color
                }
            }

            val fields = fieldAdapter.getFields()
                .filter { it.name.isNotEmpty() && it.value.isNotEmpty() }
                .map { EmbedField(it.name, it.value, it.inline) }
                .ifEmpty { null }

            val image = embedImageUrl?.let { EmbedMedia(it) }
            val thumbnail = embedThumbnailUrl?.let { EmbedMedia(it) }
            val footer = embedFooterText?.let { EmbedFooter(it, embedFooterIcon) }
            val author = embedAuthorName?.let { EmbedAuthor(it, embedAuthorUrl, embedAuthorIcon) }

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

        return WebhookPayload(
            username = username,
            avatarUrl = avatarUrl,
            content = content,
            embeds = embeds
        )
    }

    private fun sendWebhook() {
        val webhookUrl = binding.etWebhookUrl.text.toString().trim()
        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "請輸入 Webhook URL", Toast.LENGTH_SHORT).show()
            return
        }

        if (!webhookUrl.startsWith("https://discord.com/api/webhooks/") &&
            !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ URL 看起來不是 Discord Webhook")
                .setMessage("您輸入的 URL 似乎不是 Discord Webhook URL，確定要繼續嗎？")
                .setPositiveButton("繼續發送") { _, _ -> doSend(webhookUrl) }
                .setNegativeButton("取消", null)
                .show()
            return
        }

        doSend(webhookUrl)
    }

    private fun doSend(webhookUrl: String) {
        val payload = buildPayload()

        binding.btnSend.isEnabled = false
        binding.btnSend.text = "發送中..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = webhookApi.sendWebhook(webhookUrl, payload)
            binding.btnSend.isEnabled = true
            binding.btnSend.text = "🚀 發送"
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("✅ 發送成功")
                        .setMessage(it)
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

    private fun showJsonPreview() {
        val webhookUrl = binding.etWebhookUrl.text.toString().trim()
        if (webhookUrl.isEmpty() && binding.etContent.text.toString().trim().isEmpty() &&
            !binding.switchEnableEmbed.isChecked) {
            Toast.makeText(this, "請至少填寫一個欄位", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = buildPayload()
        val gson = Gson()
        val prettyJson = com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
            .toJson(payload)

        MaterialAlertDialogBuilder(this)
            .setTitle("📋 JSON 預覽")
            .setMessage(prettyJson)
            .setPositiveButton("複製") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Webhook JSON", prettyJson)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "已複製到剪貼簿", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("關閉", null)
            .show()
    }

    private fun clearForm() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清除表單")
            .setMessage("確定要清除所有欄位嗎？")
            .setPositiveButton("確定") { _, _ ->
                binding.etWebhookUrl.text.clear()
                binding.etUsername.text.clear()
                binding.etAvatarUrl.text.clear()
                binding.etContent.text.clear()
                binding.switchEnableEmbed.isChecked = false
                binding.etEmbedTitle.text.clear()
                binding.etEmbedDescription.text.clear()
                binding.etEmbedColor.text.clear()
                binding.etEmbedUrl.text.clear()
                binding.etEmbedImageUrl.text.clear()
                binding.etEmbedThumbnailUrl.text.clear()
                binding.etEmbedFooterText.text.clear()
                binding.etEmbedFooterIcon.text.clear()
                binding.etEmbedAuthorName.text.clear()
                binding.etEmbedAuthorUrl.text.clear()
                binding.etEmbedAuthorIcon.text.clear()
                fieldAdapter.fields.clear()
                fieldAdapter.notifyDataSetChanged()
                Toast.makeText(this, "已清除所有欄位", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

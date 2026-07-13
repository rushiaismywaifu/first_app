package com.webhook.sender.model

import com.google.gson.annotations.SerializedName

data class WebhookPayload(
    val username: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    val content: String? = null,
    val embeds: List<Embed>? = null
)

data class Embed(
    val title: String? = null,
    val description: String? = null,
    val color: Int? = null,
    val url: String? = null,
    val fields: List<EmbedField>? = null,
    val image: EmbedMedia? = null,
    val thumbnail: EmbedMedia? = null,
    val footer: EmbedFooter? = null,
    val author: EmbedAuthor? = null,
    @SerializedName("timestamp")
    val timestamp: String? = null
)

data class EmbedField(
    val name: String,
    val value: String,
    val inline: Boolean? = null
)

data class EmbedMedia(
    val url: String
)

data class EmbedFooter(
    val text: String,
    @SerializedName("icon_url")
    val iconUrl: String? = null
)

data class EmbedAuthor(
    val name: String,
    val url: String? = null,
    @SerializedName("icon_url")
    val iconUrl: String? = null
)

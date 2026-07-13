package com.webhook.sender

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object WebhookUrlValidator {
    private val allowedHosts = setOf("discord.com", "discordapp.com")
    private val webhookPath = Regex("^/api(?:/v\\d+)?/webhooks/\\d+/[A-Za-z0-9._-]+/?$")

    fun isAllowed(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        return parsed.scheme == "https" &&
            parsed.host in allowedHosts &&
            parsed.username.isEmpty() &&
            parsed.password.isEmpty() &&
            parsed.query == null &&
            parsed.fragment == null &&
            webhookPath.matches(parsed.encodedPath)
    }
}

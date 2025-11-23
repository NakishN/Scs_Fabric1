package com.scs.client.hud

import com.scs.client.config.ScsConfig
import com.scs.client.monitor.ChatMonitor
import com.scs.client.online.OnlineStatusService
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.time.Duration
import java.time.Instant

object HudRenderer {

    private var cachedOnlinePlayers: List<OnlineStatusService.OnlinePlayer> = emptyList()
    private var lastPlayersCacheUpdate: Long = 0
    private val PLAYERS_CACHE_UPDATE_INTERVAL_MS = 1000L

    fun render(drawContext: DrawContext, tickDelta: Float) {
        if (!ScsConfig.enableHud) return

        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val textRenderer = client.textRenderer

        val window = client.window
        val screenWidth = window.scaledWidth
        val screenHeight = window.scaledHeight

        val x = if (ScsConfig.hudX < 0) {
            screenWidth + ScsConfig.hudX
        } else {
            ScsConfig.hudX
        }
        var currentY = if (ScsConfig.hudY < 0) {
            screenHeight + ScsConfig.hudY
        } else {
            ScsConfig.hudY
        }

        var mainPanelHeight = 0
        if (ScsConfig.showMainPanel) {
            mainPanelHeight = renderMainPanel(drawContext, textRenderer, x, currentY)
            currentY += mainPanelHeight
        }

        if (ScsConfig.showOnlinePanel && ScsConfig.enableOnlineStatus) {
            val onlineX = if (ScsConfig.onlinePanelX < 0) {
                screenWidth + ScsConfig.onlinePanelX
            } else {
                ScsConfig.onlinePanelX
            }
            val onlineY = if (ScsConfig.onlinePanelY < 0) {
                screenHeight + ScsConfig.onlinePanelY
            } else {
                ScsConfig.onlinePanelY
            }
            renderOnlinePanel(drawContext, textRenderer, onlineX, onlineY)
        }

        if (ScsConfig.showServerOnlinePanel && ScsConfig.enableOnlineStatus) {
            val serverOnlineX = if (ScsConfig.serverOnlinePanelX < 0) {
                screenWidth + ScsConfig.serverOnlinePanelX
            } else {
                ScsConfig.serverOnlinePanelX
            }
            val serverOnlineY = if (ScsConfig.serverOnlinePanelY < 0) {
                screenHeight + ScsConfig.serverOnlinePanelY
            } else {
                ScsConfig.serverOnlinePanelY
            }
            renderServerOnlinePanel(drawContext, textRenderer, serverOnlineX, serverOnlineY)
        }
    }

    private fun renderMainPanel(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int
    ): Int {
        val currentCheckPlayer = com.scs.client.monitor.CheckSession.getCurrentPlayer()

        val filteredEntries = ChatMonitor.entries.filter { entry ->
            if (entry.kind == "CHAT") {
                currentCheckPlayer != null && entry.playerName?.equals(currentCheckPlayer, ignoreCase = true) == true
            } else {
                true
            }
        }

        val entries = filteredEntries.take(5)

        if (entries.isEmpty()) return 0

        val maxTextWidth = entries.maxOfOrNull { textRenderer.getWidth(getEntryText(it)) } ?: 100
        val panelWidth = maxTextWidth + 8
        val panelHeight = entries.size * (textRenderer.fontHeight + 2) + 4

        val bgColor = if (ScsConfig.hudEditMode) {
            0x900000FF.toInt()
        } else {
            0x80000000.toInt()
        }
        drawContext.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, bgColor)

        if (ScsConfig.hudEditMode) {
            val frameColor = 0xFFFFFFFF.toInt()
            drawContext.fill(x - 2, y - 2, x + panelWidth, y - 1, frameColor)
            drawContext.fill(x - 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, frameColor)
            drawContext.fill(x - 2, y - 2, x - 1, y + panelHeight, frameColor)
            drawContext.fill(x + panelWidth - 1, y - 2, x + panelWidth, y + panelHeight, frameColor)

            val labelText = Text.literal("Основная панель")
                .formatted(Formatting.YELLOW, Formatting.BOLD)
            drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
        }

        var currentY = y
        for (entry in entries) {
            val entryText = getEntryText(entry)
            val color = getEntryColor(entry.kind)

            drawContext.drawTextWithShadow(textRenderer, entryText, x, currentY, color)

            currentY += textRenderer.fontHeight + 2
        }

        return panelHeight + 4
    }

    private fun getEntryText(entry: ChatMonitor.Entry): Text {
        val prefix = when (entry.kind) {
            "CHECK" -> "✓"
            "VIOLATION" -> "⚠"
            "CHAT" -> "💬"
            else -> "•"
        }
        return Text.literal("$prefix ${entry.text}")
    }

    private fun getEntryColor(kind: String): Int {
        return when (kind) {
            "CHECK" -> parseColor(ScsConfig.checkColor)
            "VIOLATION" -> parseColor(ScsConfig.violationColor)
            "AC" -> parseColor(ScsConfig.acColor)
            else -> 0xFFFFFF
        }
    }

    private fun parseColor(hex: String): Int {
        try {
            return Integer.parseInt(hex, 16)
        } catch (e: Exception) {
            return 0xFFFFFF
        }
    }

    private fun formatTimeAgo(timestamp: Instant): String {
        val duration = Duration.between(timestamp, Instant.now())
        val seconds = duration.seconds

        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            else -> "${seconds / 3600}h"
        }
    }

    private fun renderOnlinePanel(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int
    ): Int {
        val currentTime = System.currentTimeMillis()
        val players = if (currentTime - lastPlayersCacheUpdate >= PLAYERS_CACHE_UPDATE_INTERVAL_MS) {
            try {
                val newCache = ArrayList(OnlineStatusService.onlinePlayers).take(10)
                lastPlayersCacheUpdate = currentTime
                cachedOnlinePlayers = newCache
                newCache
            } catch (e: Exception) {
                if (cachedOnlinePlayers.isEmpty()) emptyList() else cachedOnlinePlayers
            }
        } else {
            cachedOnlinePlayers
        }

        if (players.isEmpty()) {
            val panelWidth = 200
            val panelHeight = textRenderer.fontHeight + 4

            val bgColor = if (ScsConfig.hudEditMode) {
                0x9000FF00.toInt()
            } else {
                0x80000000.toInt()
            }
            drawContext.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, bgColor)

            if (ScsConfig.hudEditMode) {
                val frameColor = 0xFFFFFFFF.toInt()
                drawContext.fill(x - 2, y - 2, x + panelWidth, y - 1, frameColor)
                drawContext.fill(x - 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, frameColor)
                drawContext.fill(x - 2, y - 2, x - 1, y + panelHeight, frameColor)
                drawContext.fill(x + panelWidth - 1, y - 2, x + panelWidth, y + panelHeight, frameColor)

                val labelText = Text.literal("Онлайн панель")
                    .formatted(Formatting.YELLOW, Formatting.BOLD)
                drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
            }

            val emptyText = Text.literal("🟢 Онлайн: 0")
                .formatted(Formatting.GREEN)
            drawContext.drawTextWithShadow(textRenderer, emptyText, x, y, 0xFFFFFF)

            return panelHeight + 4
        }

        val panelWidth = 250
        var panelHeight = textRenderer.fontHeight + 4
        for (player in players) {
            panelHeight += textRenderer.fontHeight + 2
            if (player.serverAddress != "unknown" && player.serverAddress != "singleplayer") {
                panelHeight += textRenderer.fontHeight + 1
            }
        }
        panelHeight += 4

        val bgColor = if (ScsConfig.hudEditMode) {
            0x9000FF00.toInt()
        } else {
            0x80000000.toInt()
        }
        drawContext.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, bgColor)

        if (ScsConfig.hudEditMode) {
            val frameColor = 0xFFFFFFFF.toInt()
            drawContext.fill(x - 2, y - 2, x + panelWidth, y - 1, frameColor)
            drawContext.fill(x - 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, frameColor)
            drawContext.fill(x - 2, y - 2, x - 1, y + panelHeight, frameColor)
            drawContext.fill(x + panelWidth - 1, y - 2, x + panelWidth, y + panelHeight, frameColor)

            val labelText = Text.literal("Онлайн панель")
                .formatted(Formatting.YELLOW, Formatting.BOLD)
            drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
        }

        val headerText = Text.literal("🟢 Онлайн: ${players.size}")
            .formatted(Formatting.GREEN, Formatting.BOLD)
        drawContext.drawTextWithShadow(textRenderer, headerText, x, y, 0xFFFFFF)

        var currentY = y + textRenderer.fontHeight + 4
        for (player in players) {
            val playerText = Text.literal("  • ${player.playerName}")
                .formatted(Formatting.WHITE)
            drawContext.drawTextWithShadow(textRenderer, playerText, x, currentY, 0xFFFFFF)

            if (player.serverAddress != "unknown" && player.serverAddress != "singleplayer") {
                val serverText = Text.literal("    → ${player.serverAddress}")
                    .formatted(Formatting.GRAY)
                currentY += textRenderer.fontHeight + 1
                drawContext.drawTextWithShadow(textRenderer, serverText, x, currentY, 0xFFFFFF)
            }

            currentY += textRenderer.fontHeight + 2
        }

        return panelHeight + 4
    }

    private fun renderServerOnlinePanel(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int
    ): Int {
        val count = OnlineStatusService.currentServerPlayerCount

        val panelWidth = 200
        val panelHeight = textRenderer.fontHeight + 4

        val bgColor = if (ScsConfig.hudEditMode) {
            0x9000FF00.toInt()
        } else {
            0x80000000.toInt()
        }
        drawContext.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, bgColor)

        if (ScsConfig.hudEditMode) {
            val frameColor = 0xFFFFFFFF.toInt()
            drawContext.fill(x - 2, y - 2, x + panelWidth, y - 1, frameColor)
            drawContext.fill(x - 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, frameColor)
            drawContext.fill(x - 2, y - 2, x - 1, y + panelHeight, frameColor)
            drawContext.fill(x + panelWidth - 1, y - 2, x + panelWidth, y + panelHeight, frameColor)

            val labelText = Text.literal("Онлайн на сервере")
                .formatted(Formatting.YELLOW, Formatting.BOLD)
            drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
        }

        val headerText = Text.literal("👥 Онлайн: $count")
            .formatted(Formatting.GREEN, Formatting.BOLD)
        drawContext.drawTextWithShadow(textRenderer, headerText, x, y, 0xFFFFFF)

        return panelHeight + 4
    }

    private fun formatOnlineTime(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}с"
            seconds < 3600 -> "${seconds / 60}м"
            else -> "${seconds / 3600}ч ${(seconds % 3600) / 60}м"
        }
    }
}

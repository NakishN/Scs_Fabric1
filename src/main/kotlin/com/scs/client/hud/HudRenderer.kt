package com.scs.client.hud

import com.scs.client.config.ScsConfig
import com.scs.client.monitor.ChatMonitor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.time.Duration
import java.time.Instant

object HudRenderer {
    
    // Обработка кликов мыши для перетаскивания в режиме редактирования
    // Используется HudMouseHandler через миксин Screen или тик
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

        // 1. Основная панель античита (если включена)
        var mainPanelHeight = 0
        if (ScsConfig.showMainPanel) {
            mainPanelHeight = renderMainPanel(drawContext, textRenderer, x, currentY)
            currentY += mainPanelHeight
        }

        // 2. Панель DupeIP (если включена и есть недавнее обнаружение)
        if (ScsConfig.showDupeIPPanel) {
            val latestDupeIP = ChatMonitor.dupeIPResults.firstOrNull()
            if (latestDupeIP != null && isRecentDupeIP(latestDupeIP)) {
                // Используем независимые координаты для DupeIP панели
                val dupeIPX = if (ScsConfig.dupeIPPanelX < 0) {
                    screenWidth + ScsConfig.dupeIPPanelX
                } else {
                    ScsConfig.dupeIPPanelX
                }
                val dupeIPY = if (ScsConfig.dupeIPPanelY < 0) {
                    screenHeight + ScsConfig.dupeIPPanelY
                } else {
                    ScsConfig.dupeIPPanelY
                }
                renderDupeIPPanel(drawContext, textRenderer, dupeIPX, dupeIPY, latestDupeIP)
            }
        }
    }
    
    /**
     * Рендерит основную панель античита (максимум 5 записей)
     */
    private fun renderMainPanel(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int
    ): Int {
        // Ограничиваем до 5 записей для HUD
        val entries = ChatMonitor.entries.toList().take(5)
        
        if (entries.isEmpty()) return 0

        // Фоновая панель
        val maxTextWidth = entries.maxOfOrNull { textRenderer.getWidth(getEntryText(it)) } ?: 100
        val panelWidth = maxTextWidth + 8
        val panelHeight = entries.size * (textRenderer.fontHeight + 2) + 4

        // Рисуем полупрозрачный фон
        // В режиме редактирования показываем рамку для перетаскивания
        val bgColor = if (ScsConfig.hudEditMode) {
            0x900000FF.toInt() // Синий с прозрачностью в режиме редактирования
        } else {
            0x80000000.toInt() // Черный с прозрачностью
        }
        drawContext.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, bgColor)
        
        // В режиме редактирования рисуем рамку для перетаскивания
        if (ScsConfig.hudEditMode) {
            val frameColor = 0xFFFFFFFF.toInt() // Белая рамка
            drawContext.fill(x - 2, y - 2, x + panelWidth, y - 1, frameColor) // Верхняя
            drawContext.fill(x - 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, frameColor) // Нижняя
            drawContext.fill(x - 2, y - 2, x - 1, y + panelHeight, frameColor) // Левая
            drawContext.fill(x + panelWidth - 1, y - 2, x + panelWidth, y + panelHeight, frameColor) // Правая
            
            // Подпись "Основная панель" в режиме редактирования
            val labelText = Text.literal("Основная панель")
                .formatted(Formatting.YELLOW, Formatting.BOLD)
            drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
        }

        // Рисуем записи
        var currentY = y
        for (entry in entries) {
            val entryText = getEntryText(entry)
            val color = getEntryColor(entry.kind)
            
            drawContext.drawTextWithShadow(textRenderer, entryText, x, currentY, color)
            
            currentY += textRenderer.fontHeight + 2
        }

        return panelHeight + 4
    }
    
    /**
     * Рендерит панель DupeIP (временная, показывается 30 секунд)
     */
    private fun renderDupeIPPanel(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int,
        dupeIPEntry: ChatMonitor.DupeIPEntry
    ): Int {
        val panelHeight = textRenderer.fontHeight * 3 + 6
        val panelWidth = 250
        
        // Синий фон для DupeIP
        // В режиме редактирования показываем рамку для перетаскивания
        val bgColor = if (ScsConfig.hudEditMode) {
            0x904444FF.toInt() // Более яркий синий в режиме редактирования
        } else {
            0x804444FF.toInt() // Синий с прозрачностью
        }
        drawContext.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, bgColor)
        
        // Рамка
        val frameColor = if (ScsConfig.hudEditMode) {
            0xFFFFFFFF.toInt() // Белая рамка в режиме редактирования
        } else {
            0xFF4444FF.toInt() // Синяя рамка
        }
        drawContext.fill(x - 2, y - 2, x + panelWidth, y - 1, frameColor)
        drawContext.fill(x - 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, frameColor)
        drawContext.fill(x - 2, y - 2, x - 1, y + panelHeight, frameColor)
        drawContext.fill(x + panelWidth - 1, y - 2, x + panelWidth, y + panelHeight, frameColor)
        
        // В режиме редактирования показываем подпись
        if (ScsConfig.hudEditMode) {
            val labelText = Text.literal("DupeIP панель")
                .formatted(Formatting.YELLOW, Formatting.BOLD)
            drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
        }
        
        // Текст DupeIP
        val header = Text.literal("🔍 DupeIP: ${dupeIPEntry.scannedPlayer}")
            .formatted(Formatting.BLUE, Formatting.BOLD)
        drawContext.drawTextWithShadow(textRenderer, header, x, y, 0xFFFFFF)
        
        // Список дублей (первые 5)
        val dupesText = dupeIPEntry.duplicateAccounts.take(5).joinToString(", ")
        val dupesDisplay = if (dupeIPEntry.duplicateAccounts.size > 5) {
            "$dupesText... (+${dupeIPEntry.duplicateAccounts.size - 5})"
        } else {
            dupesText
        }
        val accountsText = Text.literal("Дублей: $dupesDisplay")
            .formatted(Formatting.AQUA)
        drawContext.drawTextWithShadow(textRenderer, accountsText, x, y + textRenderer.fontHeight + 2, 0xFFFFFF)
        
        return panelHeight + 4
    }
    
    /**
     * Проверяет, является ли DupeIP запись недавней (в пределах 30 секунд)
     */
    private fun isRecentDupeIP(entry: ChatMonitor.DupeIPEntry): Boolean {
        val duration = Duration.between(entry.timestamp, Instant.now())
        return duration.seconds < 30
    }

    private fun getEntryText(entry: ChatMonitor.Entry): Text {
        val prefix = when (entry.kind) {
            "CHECK" -> "✓"
            "VIOLATION" -> "⚠"
            "DUPEIP_SCAN" -> "🔍"
            "DUPEIP_RESULT" -> "🔗"
            "CHAT" -> "💬"
            else -> "•"
        }
        return Text.literal("$prefix ${entry.text}")
    }

    private fun getEntryColor(kind: String): Int {
        return when (kind) {
            "CHECK" -> parseColor(ScsConfig.checkColor)
            "VIOLATION" -> parseColor(ScsConfig.violationColor)
            "AC", "DUPEIP_SCAN", "DUPEIP_RESULT" -> parseColor(ScsConfig.acColor)
            else -> 0xFFFFFF // Белый
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
}

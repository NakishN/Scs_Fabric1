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
        
        // 2. Панель онлайн игроков (если включена)
        if (ScsConfig.showOnlinePanel && ScsConfig.enableOnlineStatus) {
            // Используем независимые координаты для онлайн панели
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
        
        // 3. Панель игроков с нарушениями (если включена)
        if (ScsConfig.showViolationsPanel && ScsConfig.enableOnlineStatus) {
            val violationsX = if (ScsConfig.violationsPanelX < 0) {
                screenWidth + ScsConfig.violationsPanelX
            } else {
                ScsConfig.violationsPanelX
            }
            val violationsY = if (ScsConfig.violationsPanelY < 0) {
                screenHeight + ScsConfig.violationsPanelY
            } else {
                ScsConfig.violationsPanelY
            }
            renderViolationsPanel(drawContext, textRenderer, violationsX, violationsY)
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
    
    /**
     * Рендерит панель онлайн игроков
     */
    private fun renderOnlinePanel(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int
    ): Int {
        val players = OnlineStatusService.onlinePlayers.toList().take(10) // Показываем максимум 10 игроков
        
        if (players.isEmpty()) {
            // Показываем пустую панель с сообщением
            val panelWidth = 200
            val panelHeight = textRenderer.fontHeight + 4
            
            val bgColor = if (ScsConfig.hudEditMode) {
                0x9000FF00.toInt() // Зеленый с прозрачностью в режиме редактирования
            } else {
                0x80000000.toInt() // Черный с прозрачностью
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
        
        // Фоновая панель
        val panelWidth = 250
        var panelHeight = textRenderer.fontHeight + 4 // Заголовок
        // Высота зависит от количества игроков и наличия серверов
        for (player in players) {
            panelHeight += textRenderer.fontHeight + 2 // Имя игрока
            if (player.serverAddress != "unknown" && player.serverAddress != "singleplayer") {
                panelHeight += textRenderer.fontHeight + 1 // Сервер
            }
        }
        panelHeight += 4 // Отступы
        
        val bgColor = if (ScsConfig.hudEditMode) {
            0x9000FF00.toInt() // Зеленый с прозрачностью в режиме редактирования
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
            
            // Подпись "Онлайн панель" в режиме редактирования
            val labelText = Text.literal("Онлайн панель")
                .formatted(Formatting.YELLOW, Formatting.BOLD)
            drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
        }
        
        // Заголовок
        val headerText = Text.literal("🟢 Онлайн: ${players.size}")
            .formatted(Formatting.GREEN, Formatting.BOLD)
        drawContext.drawTextWithShadow(textRenderer, headerText, x, y, 0xFFFFFF)
        
        // Рисуем список игроков
        var currentY = y + textRenderer.fontHeight + 4
        for (player in players) {
            val playerText = Text.literal("  • ${player.playerName}")
                .formatted(Formatting.WHITE)
            drawContext.drawTextWithShadow(textRenderer, playerText, x, currentY, 0xFFFFFF)
            
            // Показываем сервер под именем (если есть место)
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
    
    /**
     * Рендерит панель игроков с нарушениями
     */
    private fun renderViolationsPanel(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int
    ): Int {
        val players = OnlineStatusService.playersWithViolations.toList()
        
        if (players.isEmpty()) {
            // Показываем пустую панель с сообщением
            val panelWidth = 250
            val panelHeight = textRenderer.fontHeight + 4
            
            val bgColor = if (ScsConfig.hudEditMode) {
                0x90FF0000.toInt() // Красный с прозрачностью в режиме редактирования
            } else {
                0x80000000.toInt() // Черный с прозрачностью
            }
            drawContext.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, bgColor)
            
            if (ScsConfig.hudEditMode) {
                val frameColor = 0xFFFFFFFF.toInt()
                drawContext.fill(x - 2, y - 2, x + panelWidth, y - 1, frameColor)
                drawContext.fill(x - 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, frameColor)
                drawContext.fill(x - 2, y - 2, x - 1, y + panelHeight, frameColor)
                drawContext.fill(x + panelWidth - 1, y - 2, x + panelWidth, y + panelHeight, frameColor)
                
                val labelText = Text.literal("Панель нарушений")
                    .formatted(Formatting.YELLOW, Formatting.BOLD)
                drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
            }
            
            val emptyText = Text.literal("🚨 Нарушения: 0")
                .formatted(Formatting.RED)
            drawContext.drawTextWithShadow(textRenderer, emptyText, x, y, 0xFFFFFF)
            
            return panelHeight + 4
        }
        
        // Фоновая панель
        val panelWidth = 280
        var panelHeight = textRenderer.fontHeight + 4 // Заголовок
        for (player in players) {
            panelHeight += textRenderer.fontHeight + 2 // Имя игрока
            if (player.violationTypes.isNotEmpty()) {
                panelHeight += textRenderer.fontHeight + 1 // Типы нарушений
            }
        }
        panelHeight += 4 // Отступы
        
        val bgColor = if (ScsConfig.hudEditMode) {
            0x90FF0000.toInt() // Красный с прозрачностью в режиме редактирования
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
            
            // Подпись "Панель нарушений" в режиме редактирования
            val labelText = Text.literal("Панель нарушений")
                .formatted(Formatting.YELLOW, Formatting.BOLD)
            drawContext.drawTextWithShadow(textRenderer, labelText, x, y - 12, 0xFFFFFF)
        }
        
        // Заголовок
        val headerText = Text.literal("🚨 Нарушения: ${players.size}")
            .formatted(Formatting.RED, Formatting.BOLD)
        drawContext.drawTextWithShadow(textRenderer, headerText, x, y, 0xFFFFFF)
        
        // Рисуем список игроков
        var currentY = y + textRenderer.fontHeight + 4
        for (player in players.take(10)) { // Показываем максимум 10 игроков
            val playerText = Text.literal("  • ${player.playerName} (${player.violationCount})")
                .formatted(Formatting.WHITE)
            drawContext.drawTextWithShadow(textRenderer, playerText, x, currentY, 0xFFFFFF)
            currentY += textRenderer.fontHeight + 2
            
            // Показываем типы нарушений
            if (player.violationTypes.isNotEmpty()) {
                val typesText = player.violationTypes.take(3).joinToString(", ") // Показываем максимум 3 типа
                val typesDisplay = if (player.violationTypes.size > 3) {
                    "$typesText +${player.violationTypes.size - 3}"
                } else {
                    typesText
                }
                val violationTypesText = Text.literal("    → $typesDisplay")
                    .formatted(Formatting.YELLOW)
                drawContext.drawTextWithShadow(textRenderer, violationTypesText, x, currentY, 0xFFFFFF)
                currentY += textRenderer.fontHeight + 1
            }
            
            currentY += 1
        }
        
        return panelHeight + 4
    }
    
    /**
     * Форматирует время онлайн в читаемый вид
     */
    private fun formatOnlineTime(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}с"
            seconds < 3600 -> "${seconds / 60}м"
            else -> "${seconds / 3600}ч ${(seconds % 3600) / 60}м"
        }
    }
}

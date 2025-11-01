package com.scs.client.gui

import com.scs.client.config.ScsConfig
import com.scs.client.monitor.ChatMonitor
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CheckboxWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * GUI экран истории нарушений с фильтрацией
 * Открывается по F9
 */
class ChatHistoryScreen(parent: Screen?) : Screen(Text.literal("ScS - История нарушений")) {
    private var filterAll = true
    private var filterViolations = false
    private var filterChecks = false
    private var filterSerious = false
    private var filterDupeIP = false
    private var filterPlayerChat = false
    private var selectedPlayer: String? = null
    
    private lateinit var allCheckbox: CheckboxWidget
    private lateinit var violationsCheckbox: CheckboxWidget
    private lateinit var checksCheckbox: CheckboxWidget
    private lateinit var seriousCheckbox: CheckboxWidget
    private lateinit var dupeIPCheckbox: CheckboxWidget
    private lateinit var playerChatCheckbox: CheckboxWidget
    private lateinit var closeButton: ButtonWidget
    
    private val entries = mutableListOf<DisplayEntry>()
    private val playerChatEntries = mutableListOf<ChatMonitor.PlayerChatEntry>()
    private var scrollOffset = 0
    private val maxVisibleEntries = 15
    
    init {
        updateEntries()
    }
    
    override fun init() {
        super.init()
        
        val checkboxY = 30
        val checkboxX = 10
        val checkboxSpacing = 20
        
        // Чекбоксы фильтров
        allCheckbox = CheckboxWidget.builder(
            Text.literal("Все"),
            textRenderer
        ).pos(checkboxX, checkboxY)
            .checked(filterAll)
            .callback { checkbox, checked ->
                filterAll = checked
                if (checked) {
                    filterViolations = false
                    filterChecks = false
                    filterSerious = false
                    filterDupeIP = false
                    filterPlayerChat = false
                }
                updateEntries()
            }
            .build()
        addDrawableChild(allCheckbox)
        
        violationsCheckbox = CheckboxWidget.builder(
            Text.literal("Нарушения"),
            textRenderer
        ).pos(checkboxX, checkboxY + checkboxSpacing)
            .checked(filterViolations)
            .callback { _, checked ->
                filterViolations = checked
                filterAll = false
                updateEntries()
            }
            .build()
        addDrawableChild(violationsCheckbox)
        
        checksCheckbox = CheckboxWidget.builder(
            Text.literal("Проверки"),
            textRenderer
        ).pos(checkboxX, checkboxY + checkboxSpacing * 2)
            .checked(filterChecks)
            .callback { _, checked ->
                filterChecks = checked
                filterAll = false
                updateEntries()
            }
            .build()
        addDrawableChild(checksCheckbox)
        
        seriousCheckbox = CheckboxWidget.builder(
            Text.literal("Серьезные"),
            textRenderer
        ).pos(checkboxX, checkboxY + checkboxSpacing * 3)
            .checked(filterSerious)
            .callback { _, checked ->
                filterSerious = checked
                filterAll = false
                updateEntries()
            }
            .build()
        addDrawableChild(seriousCheckbox)
        
        dupeIPCheckbox = CheckboxWidget.builder(
            Text.literal("DupeIP"),
            textRenderer
        ).pos(checkboxX, checkboxY + checkboxSpacing * 4)
            .checked(filterDupeIP)
            .callback { _, checked ->
                filterDupeIP = checked
                filterAll = false
                updateEntries()
            }
            .build()
        addDrawableChild(dupeIPCheckbox)
        
        // Чекбокс для чата игрока
        playerChatCheckbox = CheckboxWidget.builder(
            Text.literal("Чат игрока"),
            textRenderer
        ).pos(checkboxX, checkboxY + checkboxSpacing * 5)
            .checked(filterPlayerChat)
            .callback { _, checked ->
                filterPlayerChat = checked
                filterAll = false
                // Если включен фильтр чата, используем текущего проверяемого игрока
                if (checked && selectedPlayer == null) {
                    selectedPlayer = com.scs.client.monitor.CheckSession.getCurrentPlayer()
                }
                updateEntries()
            }
            .build()
        addDrawableChild(playerChatCheckbox)
        
        // Кнопка закрытия
        closeButton = ButtonWidget.builder(
            Text.literal("Закрыть"),
            { close() }
        ).dimensions(width - 110, height - 30, 100, 20)
            .build()
        addDrawableChild(closeButton)
    }
    
    private fun updateCheckboxes() {
        // CheckboxWidget обновляет состояние через callback, 
        // поэтому просто синхронизируем состояние без прямого доступа к checked
        // Это делается автоматически при создании через builder
    }
    
    private fun updateEntries() {
        entries.clear()
        playerChatEntries.clear()
        
        // Если включен фильтр чата игрока, показываем чат вместо записей
        if (filterPlayerChat && selectedPlayer != null) {
            playerChatEntries.addAll(
                ChatMonitor.playerChat.filter { 
                    it.playerName.equals(selectedPlayer, ignoreCase = true) 
                }.take(50) // Последние 50 сообщений
            )
            return
        }
        
        if (filterAll || filterChecks) {
            // Добавляем проверки
            ChatMonitor.entries.filter { it.kind == "CHECK" }
                .forEach { entry ->
                    entries.add(DisplayEntry(entry.kind, entry.text, entry.timestamp, entry.playerName))
                }
        }
        
        if (filterAll || filterViolations) {
            // Добавляем нарушения (если не фильтруем только серьезные)
            if (!filterSerious) {
                ChatMonitor.violations.forEach { violation ->
                    entries.add(DisplayEntry(
                        violation.kind,
                        violation.text,
                        violation.timestamp,
                        violation.playerName
                    ))
                }
            }
        }
        
        if (filterSerious) {
            // Добавляем только серьезные нарушения
            ChatMonitor.violations.filter { it.isSerious }
                .forEach { violation ->
                    entries.add(DisplayEntry(
                        violation.kind,
                        violation.text,
                        violation.timestamp,
                        violation.playerName
                    ))
                }
        }
        
        if (filterAll || filterDupeIP) {
            // Добавляем DupeIP записи
            ChatMonitor.entries.filter { it.kind == "DUPEIP_SCAN" || it.kind == "DUPEIP_RESULT" }
                .forEach { entry ->
                    entries.add(DisplayEntry(entry.kind, entry.text, entry.timestamp, entry.playerName))
                }
            
            // Добавляем DupeIP результаты
            ChatMonitor.dupeIPResults.forEach { dupeIP ->
                entries.add(DisplayEntry(
                    "DUPEIP_RESULT",
                    dupeIP.getFormattedText(),
                    dupeIP.timestamp,
                    dupeIP.scannedPlayer
                ))
            }
        }
        
        // Сортируем по времени (новые сверху)
        entries.sortByDescending { it.timestamp.toEpochMilli() }
        
        // Ограничиваем количество
        while (entries.size > 100) {
            entries.removeAt(entries.size - 1)
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        
        // Заголовок
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("История нарушений ScS").formatted(Formatting.BOLD, Formatting.GOLD),
            width / 2,
            10,
            0xFFFFFF
        )
        
        // Статистика
        val statsText = if (filterPlayerChat && selectedPlayer != null) {
            "Чат игрока: $selectedPlayer | Сообщений: ${playerChatEntries.size}"
        } else {
            "Всего записей: ${entries.size} | Нарушений: ${ChatMonitor.violations.size} | DupeIP: ${ChatMonitor.dupeIPResults.size}"
        }
        context.drawTextWithShadow(
            textRenderer,
            Text.literal(statsText).formatted(Formatting.GRAY),
            10,
            height - 50,
            0xFFFFFF
        )
        
        // Информация о текущей проверке
        val currentCheckPlayer = com.scs.client.monitor.CheckSession.getCurrentPlayer()
        if (currentCheckPlayer != null && com.scs.client.monitor.CheckSession.isActive()) {
            val checkTime = com.scs.client.monitor.CheckSession.getStartTime()
            val checkDuration = if (checkTime != null) {
                val duration = java.time.Duration.between(checkTime, java.time.Instant.now())
                "${duration.toMinutes()}м ${duration.seconds % 60}с"
            } else {
                "?"
            }
            val checkInfo = Text.literal("Проверка: $currentCheckPlayer | Время: $checkDuration")
                .formatted(Formatting.YELLOW)
            context.drawTextWithShadow(
                textRenderer,
                checkInfo,
                10,
                height - 30,
                0xFFFFFF
            )
        }
        
        // Рисуем записи или чат игрока
        val startY = 140
        val entryHeight = textRenderer.fontHeight + 4
        val listX = 200
        val listWidth = width - listX - 20
        
        // Фон списка
        context.fill(listX - 5, startY - 5, listX + listWidth, height - 60, 0x80000000.toInt())
        
        // Если включен фильтр чата игрока, показываем чат
        if (filterPlayerChat && selectedPlayer != null && playerChatEntries.isNotEmpty()) {
            var currentY = startY
            
            // Заголовок с именем игрока
            val headerText = Text.literal("💬 Чат игрока: $selectedPlayer")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
            context.drawTextWithShadow(textRenderer, headerText, listX, currentY, 0xFFFFFF)
            currentY += entryHeight + 5
            
            // Сообщения чата
            val visibleChat = playerChatEntries.drop(scrollOffset).take(maxVisibleEntries)
            for (chatEntry in visibleChat) {
                if (currentY + entryHeight > height - 60) break
                
                val chatText = Text.literal("${chatEntry.playerName}: ${chatEntry.message}")
                    .formatted(Formatting.GRAY)
                context.drawTextWithShadow(textRenderer, chatText, listX, currentY, 0xFFFFFF)
                
                currentY += entryHeight
            }
        } else {
            // Рисуем видимые записи
            val visibleEntries = entries.drop(scrollOffset).take(maxVisibleEntries)
            var currentY = startY
            
            for (entry in visibleEntries) {
                if (currentY + entryHeight > height - 60) break
                
                val prefix = getPrefix(entry.kind)
                val color = getColor(entry.kind)
                
                val entryText = Text.literal("$prefix ${entry.text}")
                    .formatted(getFormatting(entry.kind))
                
                context.drawTextWithShadow(textRenderer, entryText, listX, currentY, color)
                
                currentY += entryHeight
            }
        }
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    private fun getPrefix(kind: String): String {
        return when (kind) {
            "CHECK" -> "✓"
            "VIOLATION" -> "⚠"
            "DUPEIP_SCAN" -> "🔍"
            "DUPEIP_RESULT" -> "🔗"
            "CHAT" -> "💬"
            else -> "•"
        }
    }
    
    private fun getFormatting(kind: String): Formatting {
        return when (kind) {
            "CHECK" -> Formatting.GREEN
            "VIOLATION" -> Formatting.GOLD
            "DUPEIP_SCAN", "DUPEIP_RESULT" -> Formatting.BLUE
            "CHAT" -> Formatting.LIGHT_PURPLE
            else -> Formatting.WHITE
        }
    }
    
    private fun getColor(kind: String): Int {
        return when (kind) {
            "CHECK" -> parseColor(ScsConfig.checkColor)
            "VIOLATION" -> parseColor(ScsConfig.violationColor)
            "DUPEIP_SCAN", "DUPEIP_RESULT" -> parseColor(ScsConfig.acColor)
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
    
    private fun formatTime(timestamp: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val dateTime = java.time.LocalDateTime.ofInstant(timestamp, java.time.ZoneId.systemDefault())
        return dateTime.format(formatter)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount > 0) {
            scrollOffset = (scrollOffset - 1).coerceAtLeast(0)
        } else if (verticalAmount < 0) {
            scrollOffset = (scrollOffset + 1).coerceAtMost((entries.size - maxVisibleEntries).coerceAtLeast(0))
        }
        return true
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) { // ESC
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    data class DisplayEntry(
        val kind: String,
        val text: String,
        val timestamp: Instant,
        val playerName: String? = null
    )
}


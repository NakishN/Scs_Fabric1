package com.scs.client.monitor

import com.scs.Scs
import com.scs.client.config.ScsConfig
import com.scs.client.sound.SoundNotificationSystem
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.io.path.appendText

object ChatMonitor {

    private val logPath: Path = FabricLoader.getInstance()
        .gameDir.resolve("logs/scs-chat.log")

    private val logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    val entries = ConcurrentLinkedDeque<Entry>()
    val violations = ConcurrentLinkedDeque<ViolationEntry>()
    val playerChat = ConcurrentLinkedDeque<PlayerChatEntry>()

    private val processedMessages = mutableSetOf<String>()

    // Паттерны для поиска
    private val anticheatPatterns = listOf(
        Regex(""".*\[.*анти.*чит.*\]\s*(\w+)\s+(.+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+(tried to .+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+(suspected .+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+tried to move abnormally.*(?:\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+suspected using.*(?:\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE)
    )

    // Паттерны для системных сообщений [System] [CHAT]
    // Учитываем возможные временные метки в любом формате после [System] [CHAT]
    private val checkPatterns = listOf(
        // Формат: [System] [CHAT] [15:38:11] ▶ Проверка успешно начата!
        // Очень гибкий паттерн - ищем любые символы между [CHAT] и "Проверка"
        Regex("""\[System\].*?\[CHAT\].*?Проверка.*?успешно.*?начата""", RegexOption.IGNORE_CASE),
        // Альтернативный - с символом стрелки
        Regex("""\[System\].*?\[CHAT\].*?[►▶].*?Проверка.*?успешно.*?начата""", RegexOption.IGNORE_CASE),
        // Еще более простой - просто ищем ключевые слова
        Regex("""Проверка.*?успешно.*?начата""", RegexOption.IGNORE_CASE)
    )

    private val playerPatterns = listOf(
        // Формат: [System] [CHAT] [15:38:11]    Проверяемый игрок: yaneloh2026
        // Очень гибкий паттерн - ищем любые символы между [CHAT] и "Проверяемый"
        Regex("""\[System\].*?\[CHAT\].*?Проверяемый\s+игрок\s*[:：]\s*([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE),
        // Альтернативный формат без слова "Проверяемый"
        Regex("""\[System\].*?\[CHAT\].*?игрок\s*[:：]\s*([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE),
        // Еще более простой - просто ищем "игрок:" и имя после него
        Regex("""игрок\s*[:：]\s*([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)
    )

    private val modePatterns = listOf(
        // Формат: [System] [CHAT] [15:38:11]    Вы находитесь на режиме: КланЛайт #1
        // Очень гибкий паттерн - захватываем все после двоеточия до конца строки
        Regex("""\[System\].*?\[CHAT\].*?Вы\s+находитесь\s+на\s+режиме\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE),
        // Альтернативный формат
        Regex("""\[System\].*?\[CHAT\].*?режим\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE),
        // Еще более простой
        Regex("""на\s+режиме\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE)
    )

    // Паттерны для системного чата [System] [CHAT]
    // Учитываем возможные временные метки в любом формате после [System] [CHAT]
    private val systemChatPatterns = listOf(
        // Формат: [System] [CHAT] [15:39:20] ᴄ | «ᴄʜᴇᴀᴛᴇʀ» yaneloh2026 » 120732
        // Очень гибкий паттерн - ищем любые символы между [CHAT] и именем игрока
        // Используем .*? для нежадного поиска
        Regex("""\[System\].*?\[CHAT\].*?[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),
        // Формат: [System] [CHAT] [15:39:20] ᴄ | «ᴄʜᴇᴀᴛᴇʀ» yaneloh2026 » окей (без пробела перед »)
        Regex("""\[System\].*?\[CHAT\].*?[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"'](.+)""", RegexOption.IGNORE_CASE),
        // Формат: [System] [CHAT] [ЧЧ:ММ:СС] ʟ | «sᴜᴘᴇʀᴠɪsᴏʀ» PlayerName » message
        Regex("""\[System\].*?\[CHAT\].*?[ʟl]\s*\|\s*.*?sᴜᴘᴇʀᴠɪsᴏʀ.*?\s*([a-zA-Z0-9_]+)\s+.*?[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),
        // Формат: [System] [CHAT] [ЧЧ:ММ:СС] PlayerName: message (обычный чат)
        Regex("""\[System\].*?\[CHAT\].*?([a-zA-Z0-9_]+)\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE),
        // Общий формат для чата игроков с кавычками
        Regex("""\[System\].*?\[CHAT\].*?[«"'"']\s*([a-zA-Z0-9_]+)\s*[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),
        // Упрощенный паттерн для чата с cheater - без требования [System] [CHAT]
        Regex("""[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),
        Regex("""[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"'](.+)""", RegexOption.IGNORE_CASE)
    )


    fun processMessage(text: String, source: String = "UNKNOWN") {
        if (text.isBlank()) return

        val cleanText = stripFormatting(text)

        // Логируем если включено
        if (ScsConfig.logAllChat) {
            logMessage(source, cleanText)
        }


        // Проверяем сообщение
        checkMessage(text, source)
        if (text != cleanText) {
            checkMessage(cleanText, source)
        }
    }

    private fun checkMessage(text: String, source: String) {
        if (text.isBlank()) return


        // Проверка на проверки
        for ((index, pattern) in checkPatterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                addEntry(Entry("CHECK", "Проверка начата"))
                return
            }
        }

        // Проверка на игрока
        for ((index, pattern) in playerPatterns.withIndex()) {
            pattern.find(text)?.let { match ->
                if (match.groupValues.size >= 2) {
                    val player = match.groupValues[1]
                    if (isValidPlayerName(player)) {
                        addEntry(Entry("CHECK", "Проверяемый: $player", player))
                        // Начинаем сессию проверки для этого игрока
                        com.scs.client.monitor.CheckSession.startCheck(player)
                        return
                    }
                }
            }
        }

        // Проверка на режим (только для отображения в HUD, не сохраняем)
        for ((index, pattern) in modePatterns.withIndex()) {
            pattern.find(text)?.let { match ->
                if (match.groupValues.size >= 2) {
                    val mode = match.groupValues[1].trim()
                    if (mode.isNotEmpty()) {
                        addEntry(Entry("CHECK", "Режим: $mode"))
                        return
                    }
                }
            }
        }

        // Проверка на системный чат [System] [CHAT] - только чат игроков
        for ((index, pattern) in systemChatPatterns.withIndex()) {
            pattern.find(text)?.let { match ->
                if (match.groupValues.size >= 3) {
                    val player = match.groupValues[1]
                    val message = match.groupValues[2]

                    if (isValidPlayerName(player) && message.trim().isNotEmpty()) {
                        val chatEntry = PlayerChatEntry(player.trim(), message.trim())
                        playerChat.addFirst(chatEntry)
                        while (playerChat.size > 50) playerChat.removeLast()

                        
                        if (ScsConfig.logAllChat) {
                            logMessage("CHAT", "$player: $message")
                        }
                        return
                    }
                }
            }
        }

        // Проверка на античит
        for (pattern in anticheatPatterns) {
            pattern.matchEntire(text)?.let { match ->
                val player = match.groupValues.getOrNull(1)
                val violation = match.groupValues.getOrNull(2)
                val type = match.groupValues.getOrNull(3)
                val countStr = match.groupValues.getOrNull(4)
                val count = countStr?.toIntOrNull() ?: 0

                if (player != null && violation != null &&
                    isValidPlayerName(player) && violation.trim().length >= 3) {
                    processViolation(player.trim(), violation.trim(), type, count, source)
                    return
                }
            }
        }
    }

    private fun processViolation(player: String, violation: String, type: String?, count: Int, source: String) {
        val entry = ViolationEntry(player, violation, type, count)
        violations.addFirst(entry)
        while (violations.size > 50) violations.removeLast()

        addEntry(Entry("VIOLATION", entry.text, entry.playerName))
        
        // Проигрываем звук уведомления
        SoundNotificationSystem.playViolationSound(entry.isSerious)

        // Отправляем нарушение на сервер
        com.scs.client.online.OnlineStatusService.sendViolation(entry)

        if (entry.isSerious) {
        }
    }

    fun addEntry(entry: Entry) {
        entries.addFirst(entry)
        while (entries.size > ScsConfig.maxMessages) {
            entries.removeLast()
        }

        if (ScsConfig.enableLogging) {
            logMessage(entry.kind, entry.text)
        }
    }

    private fun logMessage(type: String, message: String) {
        try {
            if (!Files.exists(logPath.parent)) {
                Files.createDirectories(logPath.parent)
            }

            val timestamp = LocalDateTime.now().format(logFormatter)
            val logEntry = "[$timestamp] [$type] $message\n"

            logPath.appendText(logEntry)
        } catch (e: Exception) {
            // Игнорируем ошибки логирования
        }
    }

    fun clearEntries() {
        entries.clear()
        violations.clear()
        playerChat.clear()
        processedMessages.clear()
    }

    private fun isValidPlayerName(name: String): Boolean {
        return name.length in 3..16 && name.matches(Regex("[a-zA-Z0-9_]+"))
    }

    private fun stripFormatting(text: String): String {
        return text.replace(Regex("§[0-9a-fk-or]"), "")
            .replace(Regex("[\\u00A7\\u001B]\\[[0-9;]*[a-zA-Z]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // Классы данных
    data class Entry(
        val kind: String,
        val text: String,
        val playerName: String? = null,
        val timestamp: Instant = Instant.now()
    )

    data class ViolationEntry(
        val playerName: String,
        val violation: String,
        val detectionType: String?,
        val count: Int,
        val timestamp: Instant = Instant.now()
    ) {
        val kind = "VIOLATION"
        val text = formatViolation()
        val isSerious = checkIfSerious()

        private fun formatViolation(): String {
            val countStr = if (count > 0) " #$count" else ""
            val typeStr = detectionType?.let { " ($it)" } ?: ""
            return "$playerName → $violation$typeStr$countStr"
        }

        private fun checkIfSerious(): Boolean {
            val v = violation.lowercase()
            return v.contains("combat") || v.contains("killaura") ||
                    v.contains("speed") || v.contains("fly") ||
                    v.contains("bot") || v.contains("velocity") ||
                    v.contains("hack") || v.contains("aura") || v.contains("vehicle")
        }
    }

    data class PlayerChatEntry(
        val playerName: String,
        val message: String,
        val timestamp: Instant = Instant.now()
    ) {
        val kind = "CHAT"
        val text = "$playerName: $message"
    }
}
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

    private var lastCheckedPlayer: String? = null
    private var lastCheckPlayerTime: Instant? = null


    private val anticheatPatterns = listOf(
        Regex(""".*\[.*анти.*чит.*\]\s*(\w+)\s+(.+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+(tried to .+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+(suspected .+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+tried to move abnormally.*(?:\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+suspected using.*(?:\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE)
    )



    private val checkPatterns = listOf(


        Regex("""\[System\].*?\[CHAT\].*?Проверка.*?успешно.*?начата""", RegexOption.IGNORE_CASE),

        Regex("""\[System\].*?\[CHAT\].*?[►▶].*?Проверка.*?успешно.*?начата""", RegexOption.IGNORE_CASE),

        Regex("""Проверка.*?успешно.*?начата""", RegexOption.IGNORE_CASE)
    )

    private val playerPatterns = listOf(


        Regex("""\[System\].*?\[CHAT\].*?Проверяемый\s+игрок\s*[:：]\s*([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE),

        Regex("""\[System\].*?\[CHAT\].*?игрок\s*[:：]\s*([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE),

        Regex("""игрок\s*[:：]\s*([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)
    )

    private val modePatterns = listOf(


        Regex("""\[System\].*?\[CHAT\].*?Вы\s+находитесь\s+на\s+режиме\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE),

        Regex("""\[System\].*?\[CHAT\].*?режим\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE),

        Regex("""на\s+режиме\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE)
    )



    private val systemChatPatterns = listOf(



        Regex("""\[System\].*?\[CHAT\].*?[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),

        Regex("""\[System\].*?\[CHAT\].*?[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"'](.+)""", RegexOption.IGNORE_CASE),

        Regex("""\[System\].*?\[CHAT\].*?[ʟl]\s*\|\s*.*?sᴜᴘᴇʀᴠɪsᴏʀ.*?\s*([a-zA-Z0-9_]+)\s+.*?[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),

        Regex("""\[System\].*?\[CHAT\].*?([a-zA-Z0-9_]+)\s*[:：]\s*(.+)""", RegexOption.IGNORE_CASE),

        Regex("""\[System\].*?\[CHAT\].*?[«"'"']\s*([a-zA-Z0-9_]+)\s*[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),

        Regex("""[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"']\s*(.+)""", RegexOption.IGNORE_CASE),
        Regex("""[ᴄc]\s*\|\s*.*?ᴄʜᴇᴀᴛᴇʀ.*?\s*([a-zA-Z0-9_]+)\s*[»"'"'](.+)""", RegexOption.IGNORE_CASE)
    )


    fun processMessage(text: String, source: String = "UNKNOWN") {
        if (text.isBlank()) return

        val cleanText = stripFormatting(text)


        if (ScsConfig.logAllChat) {
            logMessage(source, cleanText)
        }



        checkMessage(text, source)
        if (text != cleanText) {
            checkMessage(cleanText, source)
        }
    }

    private fun checkMessage(text: String, source: String) {
        if (text.isBlank()) return



        var playerFound = false
        for ((index, pattern) in playerPatterns.withIndex()) {
            pattern.find(text)?.let { match ->
                if (match.groupValues.size >= 2) {
                    val player = match.groupValues[1]
                    if (isValidPlayerName(player)) {

                        val now = Instant.now()
                        val isDuplicate = lastCheckedPlayer?.equals(player, ignoreCase = true) == true &&
                                lastCheckPlayerTime != null &&
                                java.time.Duration.between(lastCheckPlayerTime, now).seconds < 2

                        if (!isDuplicate) {

                            val currentPlayer = com.scs.client.monitor.CheckSession.getCurrentPlayer()
                            if (currentPlayer != null && currentPlayer != player) {

                                playerChat.removeAll { it.playerName.equals(currentPlayer, ignoreCase = true) }
                            }

                            addEntry(Entry("CHECK", "Проверяемый: $player", player))
                            lastCheckedPlayer = player
                            lastCheckPlayerTime = now
                        }


                        com.scs.client.monitor.CheckSession.startCheck(player)
                        playerFound = true
                        return
                    }
                }
            }
        }


        if (!playerFound) {
            for ((index, pattern) in checkPatterns.withIndex()) {
                val match = pattern.find(text)
                if (match != null) {
                    addEntry(Entry("CHECK", "Проверка начата"))
                    return
                }
            }
        }


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



        for ((index, pattern) in systemChatPatterns.withIndex()) {
            pattern.find(text)?.let { match ->
                if (match.groupValues.size >= 3) {
                    val player = match.groupValues[1]
                    val message = match.groupValues[2]

                    if (isValidPlayerName(player) && message.trim().isNotEmpty()) {
                        val currentCheckPlayer = com.scs.client.monitor.CheckSession.getCurrentPlayer()




                        if (currentCheckPlayer == null || player.equals(currentCheckPlayer, ignoreCase = true)) {
                            val chatEntry = PlayerChatEntry(player.trim(), message.trim())
                            playerChat.addFirst(chatEntry)


                            while (playerChat.size > 50) playerChat.removeLast()


                            if (currentCheckPlayer != null && player.equals(currentCheckPlayer, ignoreCase = true)) {
                                addEntry(Entry("CHAT", chatEntry.text, player.trim()))
                            }

                            if (ScsConfig.logAllChat) {
                                logMessage("CHAT", "$player: $message")
                            }
                        }
                        return
                    }
                }
            }
        }


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


        SoundNotificationSystem.playViolationSound(entry.isSerious)


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
        }
    }

    fun clearEntries() {
        entries.clear()
        violations.clear()
        playerChat.clear()
        lastCheckedPlayer = null
        lastCheckPlayerTime = null
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
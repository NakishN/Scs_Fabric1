package com.scs.client.monitor

import com.scs.ScsMod
import com.scs.client.config.ScsConfig
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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
    val dupeIPResults = ConcurrentLinkedDeque<DupeIPEntry>()

    private val processedMessages = mutableSetOf<String>()

    // DupeIP отслеживание
    var lastScannedPlayer: String? = null
    var lastDupeIPScanTime: Long = 0

    // Паттерны для поиска
    private val anticheatPatterns = listOf(
        Regex(""".*\[.*анти.*чит.*\]\s*(\w+)\s+(.+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+(tried to .+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+(suspected .+?)(?:\s*\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+tried to move abnormally.*(?:\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE),
        Regex(""".*(\w+)\s+suspected using.*(?:\((.+?)\))?(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE)
    )

    private val checkPatterns = listOf(
        Regex(""".*[►▶]\s*проверка.*успешно.*начата.*""", RegexOption.IGNORE_CASE),
        Regex(""".*проверка.*успешно.*начата.*""", RegexOption.IGNORE_CASE)
    )

    private val playerPatterns = listOf(
        Regex(""".*проверяемый\s+игрок\s*[:：]\s*(\w+).*""", RegexOption.IGNORE_CASE),
        Regex(""".*игрок\s*[:：]\s*(\w+).*""", RegexOption.IGNORE_CASE)
    )

    private val modePatterns = listOf(
        Regex(""".*вы\s+находитесь\s+на\s+режиме\s*[:：]\s*(.+?)\s*$""", RegexOption.IGNORE_CASE),
        Regex(""".*режим\s*[:：]\s*(.+?)\s*$""", RegexOption.IGNORE_CASE)
    )

    private val playerChatPatterns = listOf(
        Regex(""".*ᴄ\s*\|\s*«ᴄʜᴇᴀᴛᴇʀ»\s*(\w+)\s*»\s*(.+)""", RegexOption.IGNORE_CASE),
        Regex(""".*ʟ\s*\|\s*«sᴜᴘᴇʀᴠɪsᴏʀ»\s*(\w+)\s+.*?»\s*(.+)""", RegexOption.IGNORE_CASE),
        Regex(""".*[«»]\s*([a-zA-Z0-9_]+)\s*[»]\s*(.+)""", RegexOption.IGNORE_CASE)
    )

    private val dupeIPScanPattern = Regex(""".*Сканирование\s+(\w+).*""", RegexOption.IGNORE_CASE)
    private val dupeIPResultsPattern = Regex("""^([A-Za-z0-9_]+(?:,\s*[A-Za-z0-9_]+)+)$""")

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

        // Проверка на DupeIP сканирование
        dupeIPScanPattern.matchEntire(text)?.let { match ->
            val player = match.groupValues[1]
            lastScannedPlayer = player
            lastDupeIPScanTime = System.currentTimeMillis()
            addEntry(Entry("DUPEIP_SCAN", "Сканирование DupeIP: $player", player))
            ScsMod.LOGGER.info("[ScS] DUPEIP scan detected: $player")
            return
        }

        // Проверка на DupeIP результаты
        if (lastScannedPlayer != null &&
            System.currentTimeMillis() - lastDupeIPScanTime < 15000) {
            dupeIPResultsPattern.matchEntire(text.trim())?.let { match ->
                val nicknamesStr = match.groupValues[1]
                val nicknames = nicknamesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                if (nicknames.isNotEmpty()) {
                    val dupeEntry = DupeIPEntry(lastScannedPlayer!!, nicknames)
                    dupeIPResults.addFirst(dupeEntry)
                    while (dupeIPResults.size > 20) {
                        dupeIPResults.removeLast()
                    }

                    addEntry(Entry("DUPEIP_RESULT", dupeEntry.getFormattedText(), lastScannedPlayer))
                    ScsMod.LOGGER.info("[ScS] DUPEIP results: $lastScannedPlayer -> ${nicknames.size} accounts")
                }
                lastScannedPlayer = null
                return
            }
        }

        // Проверка на проверки
        for (pattern in checkPatterns) {
            if (pattern.matches(text)) {
                addEntry(Entry("CHECK", "Проверка начата"))
                ScsMod.LOGGER.info("[ScS] CHECK detected")
                return
            }
        }

        // Проверка на игрока
        for (pattern in playerPatterns) {
            pattern.matchEntire(text)?.let { match ->
                val player = match.groupValues[1]
                if (isValidPlayerName(player)) {
                    addEntry(Entry("CHECK", "Проверяемый: $player", player))
                    ScsMod.LOGGER.info("[ScS] PLAYER detected: $player")
                    return
                }
            }
        }

        // Проверка на режим
        for (pattern in modePatterns) {
            pattern.matchEntire(text)?.let { match ->
                val mode = match.groupValues[1].trim()
                if (mode.isNotEmpty()) {
                    addEntry(Entry("CHECK", "Режим: $mode"))
                    return
                }
            }
        }

        // Проверка на чат игроков
        for (pattern in playerChatPatterns) {
            pattern.matchEntire(text)?.let { match ->
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
                    ScsMod.LOGGER.info("[ScS] VIOLATION: $player - $violation")
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

        if (entry.isSerious) {
            ScsMod.LOGGER.info("[ScS] SERIOUS VIOLATION: $player - $violation")
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
        dupeIPResults.clear()
        processedMessages.clear()
        lastScannedPlayer = null
        lastDupeIPScanTime = 0
        ScsMod.LOGGER.info("[ScS] All entries cleared")
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

    data class DupeIPEntry(
        val scannedPlayer: String,
        val duplicateAccounts: List<String>,
        val timestamp: Instant = Instant.now()
    ) {
        val totalDupes = duplicateAccounts.size

        fun getFormattedText(): String {
            return "DupeIP $scannedPlayer: $totalDupes дубл (${duplicateAccounts.joinToString(", ")})"
        }
    }
}
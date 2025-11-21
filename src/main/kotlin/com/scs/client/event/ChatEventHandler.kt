package com.scs.client.event

import com.scs.client.command.ChatButtonHandler
import com.scs.client.config.ScsConfig
import com.scs.client.monitor.ChatMonitor
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.text.Text
import java.util.regex.Pattern

object ChatEventHandler {
    
    // Паттерны для поиска нарушений и DupeIP
    // Формат: [Анти-Чит] Player: violation (type) #count
    // Упрощенный паттерн: ищем "анти" и "чит" в квадратных скобках (любой порядок символов)
    private val violationPattern = Pattern.compile(
        """\[[^\]]*(?:анти|чит)[^\]]*\]\s*([a-zA-Z0-9_]+)\s*[:：]\s*(.+?)(?:\s*\(([^)]+)\))?(?:\s*#(\d+))?""",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )
    
    // Второй паттерн без двоеточия
    private val violationPattern2 = Pattern.compile(
        """\[[^\]]*(?:анти|чит)[^\]]*\]\s*([a-zA-Z0-9_]+)\s+(.+?)(?:\s*\(([^)]+)\))?(?:\s*#(\d+))?""",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )
    
    private val dupeIPScanPattern = Pattern.compile(
        """Сканирование\s+(\w+).*""",
        Pattern.CASE_INSENSITIVE
    )
    
    private val dupeIPResultsPattern = Pattern.compile(
        """^([A-Za-z0-9_]+(?:,\s*[A-Za-z0-9_]+)+)$"""
    )
    
    private var lastScannedPlayer: String? = null
    private var lastDupeIPTime: Long = 0
    
    fun register() {
        // Перехватываем и модифицируем игровые сообщения чата ДО отображения
        ClientReceiveMessageEvents.MODIFY_GAME.register { message: Text, _ ->
            try {
                // Получаем текст сообщения
                val messageText = message.string
                
                if (messageText.isNotBlank()) {
                    // Обрабатываем сообщение для мониторинга
                    ChatMonitor.processMessage(messageText, "CHAT")
                    
                    // Модифицируем сообщение, добавляя кнопки если нужно
                    if (ScsConfig.enableChatButtons) {
                        return@register modifyMessageWithButtons(message, messageText)
                    }
                }
            } catch (e: Exception) {
                // Error processing chat message
            }
            message // Возвращаем исходное сообщение
        }
    }
    
    private fun modifyMessageWithButtons(originalMessage: Text, messageText: String): Text {
        // Проверяем на DupeIP сканирование
        val dupeScanMatcher = dupeIPScanPattern.matcher(messageText)
        if (dupeScanMatcher.matches()) {
            val player = dupeScanMatcher.group(1)
            lastScannedPlayer = player
            lastDupeIPTime = System.currentTimeMillis()
            return originalMessage // Возвращаем без изменений, кнопки добавятся при получении результатов
        }
        
        // Проверяем на DupeIP результаты (в течение 15 секунд после сканирования)
        if (lastScannedPlayer != null && 
            System.currentTimeMillis() - lastDupeIPTime < 15000) {
            val resultsMatcher = dupeIPResultsPattern.matcher(messageText.trim())
            if (resultsMatcher.matches()) {
                val nicknames = messageText.trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (nicknames.size >= 1) {
                    val dupeIPMessage = ChatButtonHandler.createDupeIPMessage(lastScannedPlayer!!, nicknames)
                    // Добавляем кнопки к оригинальному сообщению, а не заменяем его
                    lastScannedPlayer = null
                    return originalMessage.copy().append(Text.literal(" ")).append(dupeIPMessage)
                }
            }
        }
        
        // Проверяем на нарушения античита напрямую в сообщении
        val violationMatcher = violationPattern.matcher(messageText)
        if (violationMatcher.find()) {
            val playerName = violationMatcher.group(1)
            val violationText = violationMatcher.group(2) ?: ""
            val detectionType = if (violationMatcher.groupCount() > 2 && violationMatcher.group(3) != null) violationMatcher.group(3) else null
            
            if (playerName != null && violationText.isNotEmpty() && playerName.length in 3..16) {
                val buttonMessage = ChatButtonHandler.createViolationMessage(
                    playerName,
                    violationText.trim(),
                    detectionType
                )
                
                // Объединяем оригинальное сообщение и кнопки
                return originalMessage.copy().append(Text.literal(" ")).append(buttonMessage)
            }
        }
        
        // Второй паттерн без двоеточия
        val violationMatcher2 = violationPattern2.matcher(messageText)
        if (violationMatcher2.find()) {
            val playerName = violationMatcher2.group(1)
            val violationText = violationMatcher2.group(2) ?: ""
            val detectionType = if (violationMatcher2.groupCount() > 2 && violationMatcher2.group(3) != null) violationMatcher2.group(3) else null
            
            if (playerName != null && violationText.isNotEmpty() && playerName.length in 3..16) {
                val buttonMessage = ChatButtonHandler.createViolationMessage(
                    playerName,
                    violationText.trim(),
                    detectionType
                )
                
                // Объединяем оригинальное сообщение и кнопки
                return originalMessage.copy().append(Text.literal(" ")).append(buttonMessage)
            }
        }
        
        // Возвращаем исходное сообщение если не подошло ни под один паттерн
        return originalMessage
    }
}


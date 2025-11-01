package com.scs.client.monitor

import com.scs.Scs
import java.time.Instant

/**
 * Управляет сессией проверки игрока
 * Автоматически очищает чат игрока после завершения проверки
 */
object CheckSession {
    private var currentPlayer: String? = null
    private var checkStartTime: Instant? = null
    private var checkTimeoutSeconds: Long = 300 // 5 минут по умолчанию
    private var isActive = false
    
    /**
     * Начинает новую сессию проверки для игрока
     */
    fun startCheck(playerName: String) {
        currentPlayer = playerName
        checkStartTime = Instant.now()
        isActive = true
        Scs.LOGGER.info("[ScS] Check session started for: $playerName")
    }
    
    /**
     * Завершает текущую сессию проверки и очищает чат игрока
     */
    fun endCheck() {
        if (!isActive || currentPlayer == null) return
        
        val player = currentPlayer!!
        isActive = false
        
        // Очищаем чат игрока
        clearPlayerChat(player)
        
        currentPlayer = null
        checkStartTime = null
        
        Scs.LOGGER.info("[ScS] Check session ended and chat cleared for: $player")
    }
    
    /**
     * Проверяет, нужно ли автоматически завершить проверку по таймауту
     */
    fun checkTimeout() {
        if (!isActive || checkStartTime == null) return
        
        val duration = java.time.Duration.between(checkStartTime, Instant.now())
        if (duration.seconds >= checkTimeoutSeconds) {
            Scs.LOGGER.info("[ScS] Check session timed out after ${duration.seconds} seconds")
            endCheck()
        }
    }
    
    /**
     * Очищает чат для конкретного игрока
     */
    private fun clearPlayerChat(playerName: String) {
        ChatMonitor.playerChat.removeAll { it.playerName.equals(playerName, ignoreCase = true) }
        Scs.LOGGER.info("[ScS] Cleared chat for player: $playerName")
    }
    
    /**
     * Получает текущего проверяемого игрока
     */
    fun getCurrentPlayer(): String? = currentPlayer
    
    /**
     * Проверяет, активна ли сессия проверки
     */
    fun isActive(): Boolean = isActive
    
    /**
     * Устанавливает таймаут для проверки (в секундах)
     */
    fun setTimeout(seconds: Long) {
        checkTimeoutSeconds = seconds.coerceAtLeast(60) // Минимум 1 минута
    }
    
    /**
     * Получает время начала проверки
     */
    fun getStartTime(): Instant? = checkStartTime
}


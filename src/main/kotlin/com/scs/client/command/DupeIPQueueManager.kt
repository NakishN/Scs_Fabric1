package com.scs.client.command

import com.scs.Scs

/**
 * Менеджер очереди для DupeIP результатов
 * Сохраняет список игроков для последовательной обработки
 */
object DupeIPQueueManager {
    private var queuedPlayers: MutableList<String> = mutableListOf()
    private var queuedCommand: String = "/history"
    
    fun setQueuePlayers(players: List<String>, command: String) {
        queuedPlayers.clear()
        // Добавляем всех игроков кроме первого (первый будет выполнен при клике)
        queuedPlayers.addAll(players.drop(1))
        queuedCommand = command
        Scs.LOGGER.info("[ScS] Queue set: ${queuedPlayers.size} players for $command (total: ${players.size}, first executed immediately)")
    }
    
    /**
     * Обрабатывает следующего игрока из очереди
     */
    fun processNextFromQueue() {
        if (queuedPlayers.isEmpty()) {
            val client = net.minecraft.client.MinecraftClient.getInstance()
            client.player?.sendMessage(
                net.minecraft.text.Text.literal("§a[ScS] Очередь DupeIP завершена").formatted(net.minecraft.util.Formatting.GREEN),
                false
            )
            return
        }
        
        val nextPlayer = queuedPlayers.removeAt(0)
        val command = "$queuedCommand $nextPlayer"
        
        Scs.LOGGER.info("[ScS] Processing next: $command (${queuedPlayers.size} remaining)")
        
        // Добавляем команду в очередь CommandScheduler для выполнения с задержкой
        CommandScheduler.scheduleCommand(command)
    }
    
    fun getQueueSize(): Int = queuedPlayers.size
    
    fun hasQueue(): Boolean = queuedPlayers.isNotEmpty()
}


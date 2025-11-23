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

        queuedPlayers.addAll(players.drop(1))
        queuedCommand = command
    }

    /**
     * Обрабатывает следующего игрока из очереди
     * Отправляет команду через MinecraftClient, чтобы она проходила через ClientSendMessageEvents.COMMAND
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



        val client = net.minecraft.client.MinecraftClient.getInstance()
        val player = client.player

        if (player != null) {
            val fullCommand = if (command.startsWith("/")) command else "/$command"

            client.networkHandler?.sendChatCommand(fullCommand)
        }
    }

    /**
     * Просматривает следующего игрока без удаления из очереди
     */
    fun peekNextPlayer(): String? {
        return queuedPlayers.firstOrNull()
    }

    /**
     * Удаляет следующего игрока из очереди
     */
    fun removeNextPlayer(): String? {
        return if (queuedPlayers.isNotEmpty()) queuedPlayers.removeAt(0) else null
    }

    fun getQueueSize(): Int = queuedPlayers.size

    fun hasQueue(): Boolean = queuedPlayers.isNotEmpty()
}


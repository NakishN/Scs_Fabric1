package com.scs.client.command

import com.scs.Scs
import com.scs.client.config.ScsConfig
import net.minecraft.client.MinecraftClient
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Система антиспама команд для массовых операций DupeIP
 * Выполняет команды с настраиваемой задержкой для защиты от спама
 */
object CommandScheduler {
    private val commandQueue = ConcurrentLinkedQueue<ScheduledCommand>()
    private var lastCommandTime = 0L
    var commandDelay = 1200L
        private set

    private var isProcessing = false

    data class ScheduledCommand(
        val command: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Добавляет команду в очередь
     */
    fun scheduleCommand(command: String) {
        if (command.isBlank()) return


        val cleanCommand = command.removePrefix("/")

        commandQueue.offer(ScheduledCommand(cleanCommand))
    }

    /**
     * Добавляет несколько команд в очередь
     */
    fun scheduleCommands(commands: List<String>) {
        commands.forEach { scheduleCommand(it) }
    }

    /**
     * Обработка очереди команд (вызывается каждый тик)
     */
    fun processQueue(client: MinecraftClient) {
        if (commandQueue.isEmpty() || isProcessing) return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastCommand = currentTime - lastCommandTime

        if (timeSinceLastCommand >= commandDelay) {
            val nextCommand = commandQueue.poll()
            if (nextCommand != null) {
                isProcessing = true
                executeCommand(client, nextCommand.command)
                lastCommandTime = currentTime
                isProcessing = false
            }
        }
    }

    /**
     * Выполняет команду через сеть
     */
    private fun executeCommand(client: MinecraftClient, command: String) {
        try {
            val player = client.player
            val networkHandler = client.networkHandler

            if (player == null || networkHandler == null) {
                return
            }

            // sendChatCommand expects command WITHOUT leading slash
            val cleanCommand = command.trimStart('/')


            networkHandler.sendChatCommand(cleanCommand)


            if (ScsConfig.enableChatButtons) {
                player.sendMessage(
                    net.minecraft.text.Text.literal("§7[ScS] §fКоманда: §e/$cleanCommand").apply {
                        style = style.withItalic(true)
                    },
                    false
                )
            }
        } catch (e: Exception) {

        }
    }

    /**
     * Устанавливает задержку между командами (в миллисекундах)
     */
    fun setDelay(delayMs: Long) {
        if (delayMs < 100) {
            commandDelay = 100L
        } else if (delayMs > 10000) {
            commandDelay = 10000L
        } else {
            commandDelay = delayMs
        }
    }

    /**
     * Очищает очередь команд
     */
    fun clearQueue() {
        commandQueue.clear()
        lastCommandTime = 0L
    }

    /**
     * Получает текущий размер очереди
     */
    fun getQueueSize(): Int = commandQueue.size

    /**
     * Проверяет, пуста ли очередь
     */
    fun isQueueEmpty(): Boolean = commandQueue.isEmpty()

    /**
     * Получает все команды в очереди (для отображения)
     */
    fun getQueueSnapshot(): List<String> = commandQueue.map { it.command }
}


package com.scs.client.command

import com.scs.Scs
import com.scs.client.command.DupeIPQueueManager
import com.scs.client.monitor.ChatMonitor
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Обработчик специальных команд /scs:*
 */
object CommandHandler {

    fun register() {

        ClientSendMessageEvents.COMMAND.register { command: String ->
            if (command.startsWith("/scs:")) {
                handleCommand(command.substring(5))
                false
            } else {

                val shouldTrack = CommandTracker.shouldTrack(command)
                if (shouldTrack) {
                    CommandTracker.trackCommand(command)


                    val client = MinecraftClient.getInstance()
                    val player = client.player
                    if (player != null) {
                        val warningText = net.minecraft.text.Text.literal("⚠ Использование данных команд требует видеозаписи, желательно включить видео")
                            .formatted(net.minecraft.util.Formatting.YELLOW, net.minecraft.util.Formatting.BOLD)
                        player.sendMessage(warningText, false)
                    }
                }



                if (command.startsWith("/history ")) {
                    val hasQueue = DupeIPQueueManager.hasQueue()
                    val queueSize = if (hasQueue) DupeIPQueueManager.getQueueSize() else 0


                    if (hasQueue) {

                        val delayMs = 3000L



                        Thread {
                            try {
                                Thread.sleep(delayMs)


                                if (DupeIPQueueManager.hasQueue()) {
                                    DupeIPQueueManager.processNextFromQueue()
                                } else {
                                }
                            } catch (e: Exception) {

                            }
                        }.start()
                    }
                }
                true
            }
        }
    }


    private fun handleCommand(command: String) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        val parts = command.split(" ").filter { it.isNotBlank() }
        val cmd = parts.getOrNull(0)?.lowercase() ?: ""
        val args = parts.drop(1)

        when (cmd) {
            "help" -> handleHelp(player)
            "history_all" -> handleHistoryAll(player, args)
            "freezing_history_all" -> handleFreezingHistoryAll(player, args)
            "clear_queue" -> handleClearQueue(player)
            "delay" -> handleDelay(player, args)
            "queue_history" -> handleQueueHistory(player)
            "mass_command" -> handleMassCommand(player, args)
            else -> {

                if (command == "add_history") {
                    handleQueueHistory(player)
                } else {
                    player.sendMessage(
                        Text.literal("§c[ScS] Неизвестная команда: /scs:$command")
                            .formatted(Formatting.RED),
                        false
                    )
                    handleHelp(player)
                }
            }
        }
    }

    private fun handleHelp(player: net.minecraft.entity.player.PlayerEntity) {
        val helpText = """
            §e=== ScS Enhanced - Справка по командам ===
            §6/scs:help §7- Показать эту справку
            §6/scs:history_all <игрок1,игрок2,...> §7- История для всех игроков
            §6/scs:freezing_history_all <игрок1,игрок2,...> §7- Freezing история для всех
            §6/scs:clear_queue §7- Очистить очередь команд
            §6/scs:delay <миллисекунды> §7- Установить задержку между командами
            §7Текущая очередь: ${CommandScheduler.getQueueSize()} команд
            §7Задержка: ${CommandScheduler.commandDelay}ms
        """.trimIndent()

        player.sendMessage(Text.literal(helpText).formatted(Formatting.YELLOW), false)
    }

    private fun handleHistoryAll(player: net.minecraft.entity.player.PlayerEntity, args: List<String>) {
        if (args.isEmpty()) {
            player.sendMessage(
                Text.literal("§c[ScS] Использование: /scs:history_all <игрок1,игрок2,...>")
                    .formatted(Formatting.RED),
                false
            )
            return
        }

        val players = args.joinToString(" ").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (players.isEmpty()) {
            player.sendMessage(
                Text.literal("§c[ScS] Укажите хотя бы одного игрока")
                    .formatted(Formatting.RED),
                false
            )
            return
        }


        players.forEach { playerName ->
            CommandScheduler.scheduleCommand("/history $playerName")
        }
        player.sendMessage(
            Text.literal("§e[ScS] Добавлено ${players.size} команд в очередь")
                .formatted(Formatting.YELLOW),
            false
        )
    }

    private fun handleFreezingHistoryAll(player: net.minecraft.entity.player.PlayerEntity, args: List<String>) {
        if (args.isEmpty()) {
            player.sendMessage(
                Text.literal("§c[ScS] Использование: /scs:freezing_history_all <игрок1,игрок2,...>")
                    .formatted(Formatting.RED),
                false
            )
            return
        }

        val players = args.joinToString(" ").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (players.isEmpty()) {
            player.sendMessage(
                Text.literal("§c[ScS] Укажите хотя бы одного игрока")
                    .formatted(Formatting.RED),
                false
            )
            return
        }


        players.forEach { playerName ->
            CommandScheduler.scheduleCommand("/freezinghistory $playerName")
        }
        player.sendMessage(
            Text.literal("§e[ScS] Добавлено ${players.size} команд в очередь")
                .formatted(Formatting.YELLOW),
            false
        )
    }

    private fun handleClearQueue(player: net.minecraft.entity.player.PlayerEntity) {
        val size = CommandScheduler.getQueueSize()
        CommandScheduler.clearQueue()

        player.sendMessage(
            Text.literal("§e[ScS] Очередь команд очищена (удалено $size команд)")
                .formatted(Formatting.YELLOW),
            false
        )
    }

    private fun handleDelay(player: net.minecraft.entity.player.PlayerEntity, args: List<String>) {
        if (args.isEmpty()) {
            player.sendMessage(
                Text.literal("§7[ScS] Текущая задержка: ${CommandScheduler.commandDelay}ms")
                    .formatted(Formatting.GRAY),
                false
            )
            return
        }

        val delayStr = args[0]
        val delay = delayStr.toLongOrNull()

        if (delay == null || delay < 100) {
            player.sendMessage(
                Text.literal("§c[ScS] Неверное значение задержки. Минимум: 100ms")
                    .formatted(Formatting.RED),
                false
            )
            return
        }

        CommandScheduler.setDelay(delay)
        player.sendMessage(
            Text.literal("§a[ScS] Задержка установлена: ${CommandScheduler.commandDelay}ms")
                .formatted(Formatting.GREEN),
            false
        )
    }

    private fun handleQueueHistory(player: net.minecraft.entity.player.PlayerEntity) {

        if (DupeIPQueueManager.hasQueue()) {
            DupeIPQueueManager.processNextFromQueue()
        } else {
            player.sendMessage(
                Text.literal("§e[ScS] Нет игроков в очереди DupeIP")
                    .formatted(Formatting.YELLOW),
                false
            )
        }
    }

    private fun handleMassCommand(player: net.minecraft.entity.player.PlayerEntity, args: List<String>) {


        player.sendMessage(
            Text.literal("§e[ScS] Используйте кнопки в чате для массовых операций")
                .formatted(Formatting.YELLOW),
            false
        )
    }
}


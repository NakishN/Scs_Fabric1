package com.scs.client.command

import com.scs.Scs
import com.scs.client.config.ScsConfig
import com.scs.client.monitor.ChatMonitor
import net.minecraft.client.MinecraftClient
import net.minecraft.text.*
import net.minecraft.util.Formatting

/**
 * Обработчик интерактивных кнопок в чате
 * Добавляет кнопки к сообщениям античита и обрабатывает клики по ним
 */
object ChatButtonHandler {

    /**
     * Создает сообщение с интерактивными кнопками для нарушения
     */
    fun createViolationMessage(playerName: String, violation: String, type: String? = null): Text {
        val baseMessage = Text.literal("")



        if (ScsConfig.enableChatButtons) {

            baseMessage.siblings.add(createButton(
                "[Проверить]",
                "/freezing $playerName",
                "Выполняет /freezing $playerName (ник скопирован)",
                Formatting.GREEN,
                playerName
            ))

            baseMessage.siblings.add(Text.literal(" "))


            baseMessage.siblings.add(createButton(
                "[Спек]",
                "/matrix spectate $playerName",
                "Выполняет /matrix spectate $playerName (повторно для выхода) (ник скопирован)",
                Formatting.BLUE,
                playerName
            ))

            baseMessage.siblings.add(Text.literal(" "))


            baseMessage.siblings.add(createButton(
                "[Активность]",
                "/playeractivity $playerName",
                "Выполняет /playeractivity $playerName (ник скопирован)",
                Formatting.AQUA,
                playerName
            ))

            baseMessage.siblings.add(Text.literal(" "))


            baseMessage.siblings.add(createButton(
                "[История]",
                "/freezinghistory $playerName",
                "Выполняет /freezinghistory $playerName (ник скопирован)",
                Formatting.YELLOW,
                playerName
            ))
        }

        return baseMessage
    }


    /**
     * Создает интерактивную кнопку (яркий стиль без курсива)
     */
    private fun createButton(text: String, command: String, hoverText: String, color: Formatting, playerName: String? = null): Text {
        return Text.literal(text)
            .formatted(color)
            .styled { style ->
                style.withItalic(false)
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverText).formatted(Formatting.GRAY)))
                    .withUnderline(false)
                    .withBold(false)
            }
    }


    /**
     * Создает сообщение с кнопками для DupeIP результатов
     * Простая кнопка - добавляет всех игроков в очередь последовательно
     * Использует обычные команды /history без кастомных команд
     */
    fun createDupeIPMessage(scannedPlayer: String, duplicateAccounts: List<String>): Text {
        val baseMessage = Text.literal("")

        if (!ScsConfig.enableChatButtons || duplicateAccounts.isEmpty()) {
            return baseMessage
        }



        baseMessage.siblings.add(createQueueAllButton(
            "[История всех]",
            duplicateAccounts,
            "/history",
            "Добавляет /history для всех ${duplicateAccounts.size} игроков в очередь последовательно",
            Formatting.GREEN
        ))

        return baseMessage
    }

    /**
     * Создает кнопку для добавления всех игроков в очередь
     * Все игроки добавляются в очередь последовательно через CommandScheduler
     */
    private fun createQueueAllButton(
        text: String,
        players: List<String>,
        commandPrefix: String,
        hoverText: String,
        color: Formatting
    ): Text {
        if (players.isEmpty()) {
            return Text.literal(text).formatted(color)
        }



        DupeIPQueueManager.setQueuePlayers(players, commandPrefix)



        return Text.literal(text)
            .formatted(color)
            .styled { style ->
                style.withItalic(false)

                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$commandPrefix ${players.first()}"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverText).formatted(Formatting.GRAY)))
                    .withUnderline(false)
                    .withBold(false)
            }
    }
}


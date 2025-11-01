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
        
        // Добавляем кнопки только если они включены
        if (ScsConfig.enableChatButtons) {
            // Кнопка [Проверить] - приглушенный зеленый
            baseMessage.siblings.add(createButton(
                "[Проверить]",
                "/freezing $playerName",
                "Выполняет /freezing $playerName",
                Formatting.DARK_GREEN
            ))
            
            baseMessage.siblings.add(Text.literal(" ")) // Пробел между кнопками
            
            // Кнопка [Спек] - приглушенный синий
            baseMessage.siblings.add(createButton(
                "[Спек]",
                "/matrix spectate $playerName",
                "Выполняет /matrix spectate $playerName (повторно для выхода)",
                Formatting.DARK_BLUE
            ))
            
            baseMessage.siblings.add(Text.literal(" "))
            
            // Кнопка [Активность] - темно-серый
            baseMessage.siblings.add(createButton(
                "[Активность]",
                "/playeractivity $playerName",
                "Выполняет /playeractivity $playerName",
                Formatting.DARK_AQUA
            ))
            
            baseMessage.siblings.add(Text.literal(" "))
            
            // Кнопка [История] - темно-желтый
            baseMessage.siblings.add(createButton(
                "[История]",
                "/freezinghistory $playerName",
                "Выполняет /freezinghistory $playerName",
                Formatting.GOLD
            ))
        }
        
        return baseMessage
    }
    
    /**
     * Создает интерактивную кнопку (приглушенный стиль)
     */
    private fun createButton(text: String, command: String, hoverText: String, color: Formatting): Text {
        return Text.literal(text)
            .formatted(color)
            .styled { style ->
                style.withItalic(true) // Курсив вместо жирного шрифта
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverText).formatted(Formatting.GRAY)))
                    .withUnderline(false) // Убираем подчеркивание
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
        
        // Кнопка [История всех] - добавляет всех игроков в очередь при клике
        // Используем обработчик через handleCommandButtonClick для добавления в очередь
        baseMessage.siblings.add(createQueueAllButton(
            "[История всех]",
            duplicateAccounts,
            "/history",
            "Добавляет /history для всех ${duplicateAccounts.size} игроков в очередь последовательно",
            Formatting.DARK_GREEN
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
        
        // Сохраняем всех игроков для обработки
        DupeIPQueueManager.setQueuePlayers(players, commandPrefix)
        
        // Создаем кнопку, которая при клике запускает обработку очереди
        return Text.literal(text)
            .formatted(color)
            .styled { style ->
                style.withItalic(true) // Курсив вместо жирного шрифта
                    // При клике выполняем первую команду, остальные через очередь
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$commandPrefix ${players.first()}"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverText).formatted(Formatting.GRAY)))
                    .withUnderline(false) // Убираем подчеркивание
            }
    }
    
    
    /**
     * Обрабатывает клик по кнопке команды
     * Все команды отправляются на сервер напрямую, очередь обрабатывается автоматически
     */
    fun handleCommandButtonClick(command: String) {
        if (!ScsConfig.enableChatButtons) return
        
        // Команды отправляются напрямую на сервер через ClickEvent
        // CommandScheduler обрабатывает очередь автоматически
        Scs.LOGGER.info("[ScS] Button command clicked: $command")
    }
}


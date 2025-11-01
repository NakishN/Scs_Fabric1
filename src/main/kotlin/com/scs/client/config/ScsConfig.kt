package com.scs.client.config

object ScsConfig {
    // HUD настройки
    var enableHud = true
    var hudX = -320
    var hudY = 6
    var showLast = 15
    
    // Настройки видимости панелей
    var showMainPanel = true      // Основная панель (нарушения, проверки)
    var showDupeIPPanel = true    // Панель DupeIP
    
    // Позиции панелей (независимые координаты)
    var dupeIPPanelX = -320       // X координата DupeIP панели
    var dupeIPPanelY = 100        // Y координата DupeIP панели
    
    // Режим редактирования HUD
    var hudEditMode = false       // Режим редактирования HUD (перетаскивание панелей)

    // Цвета
    var checkColor = "00FF7F"
    var acColor = "FF4444"
    var violationColor = "FFA500"

    // Интерактивность
    var enableChatButtons = true
    var autoCommands = false

    // Звуки
    var soundAlerts = true
    var alertSound = "minecraft:block.note_block.bell"

    // Логирование
    var enableLogging = true
    var logAllChat = false

    // Система шаурмы
    var enableShaurma = true
    var shaurmaHud = true
    var shaurmaSounds = true
    var shaurmaBonusChance = 15
    var shaurmaBaseReward = 1
    var shaurmaChatMessages = true

    // Расширенные
    var violationKeywords = listOf(
        "tried to move abnormally",
        "might be using combat hacks",
        "suspected use of automatic robots",
        "tried to reach entity outside",
        "invalid movement",
        "speed hacks",
        "fly hacks"
    )
    var maxMessages = 50
}
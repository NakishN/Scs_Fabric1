package com.scs.client.config

import com.scs.Scs
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ScsConfig {
    private val configPath: Path = FabricLoader.getInstance()
        .configDir.resolve("scs-config.txt")
    // HUD настройки
    var enableHud = true
    var hudX = -320
    var hudY = 6
    var showLast = 15
    
    // Настройки видимости панелей
    var showMainPanel = true      // Основная панель (нарушения, проверки)
    var showOnlinePanel = true    // Панель онлайн игроков
    var showViolationsPanel = true // Панель игроков с нарушениями
    
    // Позиции панелей (независимые координаты)
    var onlinePanelX = 320         // X координата онлайн панели
    var onlinePanelY = 6          // Y координата онлайн панели
    var violationsPanelX = -320    // X координата панели нарушений
    var violationsPanelY = 6      // Y координата панели нарушений
    
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

    // Онлайн статус
    var enableOnlineStatus = true
    var onlineStatusUrl = "https://cj814820.tw1.ru/api.php" // URL веб-сервера для онлайн статуса
    
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
    
    /**
     * Загружает конфигурацию из файла
     */
    fun load() {
        try {
            if (!configPath.exists()) {
                save() // Сохраняем дефолтные значения
                return
            }
            
            val lines = configPath.readText().lines()
            val config = mutableMapOf<String, String>()
            
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                
                val separatorIndex = trimmed.indexOf('=')
                if (separatorIndex > 0) {
                    val key = trimmed.substring(0, separatorIndex).trim()
                    val value = trimmed.substring(separatorIndex + 1).trim()
                    config[key] = value
                }
            }
            
            // Загружаем значения из конфига
            config["enableHud"]?.let { enableHud = it.toBoolean() }
            config["hudX"]?.let { hudX = it.toIntOrNull() ?: -320 }
            config["hudY"]?.let { hudY = it.toIntOrNull() ?: 6 }
            config["showLast"]?.let { showLast = it.toIntOrNull() ?: 15 }
            
            config["showMainPanel"]?.let { showMainPanel = it.toBoolean() }
            config["showOnlinePanel"]?.let { showOnlinePanel = it.toBoolean() }
            config["showViolationsPanel"]?.let { showViolationsPanel = it.toBoolean() }
            
            config["onlinePanelX"]?.let { onlinePanelX = it.toIntOrNull() ?: 320 }
            config["onlinePanelY"]?.let { onlinePanelY = it.toIntOrNull() ?: 6 }
            config["violationsPanelX"]?.let { violationsPanelX = it.toIntOrNull() ?: -320 }
            config["violationsPanelY"]?.let { violationsPanelY = it.toIntOrNull() ?: 6 }
            
            config["hudEditMode"]?.let { hudEditMode = it.toBoolean() }
            
            config["checkColor"]?.let { checkColor = it }
            config["acColor"]?.let { acColor = it }
            config["violationColor"]?.let { violationColor = it }
            
            config["enableChatButtons"]?.let { enableChatButtons = it.toBoolean() }
            config["autoCommands"]?.let { autoCommands = it.toBoolean() }
            
            config["soundAlerts"]?.let { soundAlerts = it.toBoolean() }
            config["alertSound"]?.let { alertSound = it }
            
            config["enableLogging"]?.let { enableLogging = it.toBoolean() }
            config["logAllChat"]?.let { logAllChat = it.toBoolean() }
            
            config["enableShaurma"]?.let { enableShaurma = it.toBoolean() }
            config["shaurmaHud"]?.let { shaurmaHud = it.toBoolean() }
            config["shaurmaSounds"]?.let { shaurmaSounds = it.toBoolean() }
            config["shaurmaBonusChance"]?.let { shaurmaBonusChance = it.toIntOrNull() ?: 15 }
            config["shaurmaBaseReward"]?.let { shaurmaBaseReward = it.toIntOrNull() ?: 1 }
            config["shaurmaChatMessages"]?.let { shaurmaChatMessages = it.toBoolean() }
            
            config["enableOnlineStatus"]?.let { enableOnlineStatus = it.toBoolean() }
            config["onlineStatusUrl"]?.let { onlineStatusUrl = it }
            
            config["maxMessages"]?.let { maxMessages = it.toIntOrNull() ?: 50 }
            
            // Загружаем список violationKeywords если есть
            config["violationKeywords"]?.let { keywordsStr ->
                if (keywordsStr.isNotEmpty()) {
                    violationKeywords = keywordsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }
            
        } catch (e: Exception) {
            Scs.LOGGER.error("[ScS] Failed to load config", e)
        }
    }
    
    /**
     * Сохраняет конфигурацию в файл
     */
    fun save() {
        try {
            val config = buildString {
                appendLine("# ScS Enhanced Configuration")
                appendLine("# Этот файл создан автоматически. Редактируйте значения по необходимости.")
                appendLine()
                
                appendLine("# HUD настройки")
                appendLine("enableHud=$enableHud")
                appendLine("hudX=$hudX")
                appendLine("hudY=$hudY")
                appendLine("showLast=$showLast")
                appendLine()
                
                appendLine("# Настройки видимости панелей")
                appendLine("showMainPanel=$showMainPanel")
                appendLine("showOnlinePanel=$showOnlinePanel")
                appendLine("showViolationsPanel=$showViolationsPanel")
                appendLine()
                
                appendLine("# Позиции панелей")
                appendLine("onlinePanelX=$onlinePanelX")
                appendLine("onlinePanelY=$onlinePanelY")
                appendLine("violationsPanelX=$violationsPanelX")
                appendLine("violationsPanelY=$violationsPanelY")
                appendLine()
                
                appendLine("# Режим редактирования HUD")
                appendLine("hudEditMode=$hudEditMode")
                appendLine()
                
                appendLine("# Цвета (HEX без #)")
                appendLine("checkColor=$checkColor")
                appendLine("acColor=$acColor")
                appendLine("violationColor=$violationColor")
                appendLine()
                
                appendLine("# Интерактивность")
                appendLine("enableChatButtons=$enableChatButtons")
                appendLine("autoCommands=$autoCommands")
                appendLine()
                
                appendLine("# Звуки")
                appendLine("soundAlerts=$soundAlerts")
                appendLine("alertSound=$alertSound")
                appendLine()
                
                appendLine("# Логирование")
                appendLine("enableLogging=$enableLogging")
                appendLine("logAllChat=$logAllChat")
                appendLine()
                
                appendLine("# Система шаурмы")
                appendLine("enableShaurma=$enableShaurma")
                appendLine("shaurmaHud=$shaurmaHud")
                appendLine("shaurmaSounds=$shaurmaSounds")
                appendLine("shaurmaBonusChance=$shaurmaBonusChance")
                appendLine("shaurmaBaseReward=$shaurmaBaseReward")
                appendLine("shaurmaChatMessages=$shaurmaChatMessages")
                appendLine()
                
                appendLine("# Онлайн статус")
                appendLine("enableOnlineStatus=$enableOnlineStatus")
                appendLine("onlineStatusUrl=$onlineStatusUrl")
                appendLine()
                
                appendLine("# Расширенные")
                appendLine("violationKeywords=${violationKeywords.joinToString(",")}")
                appendLine("maxMessages=$maxMessages")
            }
            
            configPath.writeText(config)
        } catch (e: Exception) {
            Scs.LOGGER.error("[ScS] Failed to save config", e)
        }
    }
}
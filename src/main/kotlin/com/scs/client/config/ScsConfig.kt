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
    
    /**
     * Загружает конфигурацию из файла
     */
    fun load() {
        try {
            if (!configPath.exists()) {
                Scs.LOGGER.info("[ScS] Config file not found, using defaults")
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
            config["showDupeIPPanel"]?.let { showDupeIPPanel = it.toBoolean() }
            
            config["dupeIPPanelX"]?.let { dupeIPPanelX = it.toIntOrNull() ?: -320 }
            config["dupeIPPanelY"]?.let { dupeIPPanelY = it.toIntOrNull() ?: 100 }
            
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
            
            config["maxMessages"]?.let { maxMessages = it.toIntOrNull() ?: 50 }
            
            // Загружаем список violationKeywords если есть
            config["violationKeywords"]?.let { keywordsStr ->
                if (keywordsStr.isNotEmpty()) {
                    violationKeywords = keywordsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }
            
            Scs.LOGGER.info("[ScS] Config loaded from file")
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
                appendLine("showDupeIPPanel=$showDupeIPPanel")
                appendLine()
                
                appendLine("# Позиции панелей")
                appendLine("dupeIPPanelX=$dupeIPPanelX")
                appendLine("dupeIPPanelY=$dupeIPPanelY")
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
                
                appendLine("# Расширенные")
                appendLine("violationKeywords=${violationKeywords.joinToString(",")}")
                appendLine("maxMessages=$maxMessages")
            }
            
            configPath.writeText(config)
            Scs.LOGGER.info("[ScS] Config saved to file")
        } catch (e: Exception) {
            Scs.LOGGER.error("[ScS] Failed to save config", e)
        }
    }
}
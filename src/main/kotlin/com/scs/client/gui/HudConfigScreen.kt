package com.scs.client.gui

import com.scs.client.config.ScsConfig
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CheckboxWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Экран настроек HUD с возможностью перетаскивания панелей
 */
class HudConfigScreen(parent: Screen?) : Screen(Text.literal("Настройки HUD")) {
    
    private var draggingPanel: String? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    
    // Временные позиции для перетаскивания
    private var tempMainPanelX = ScsConfig.hudX
    private var tempMainPanelY = ScsConfig.hudY
    private var tempOnlinePanelX = ScsConfig.onlinePanelX
    private var tempOnlinePanelY = ScsConfig.onlinePanelY
    private var tempServerOnlinePanelX = ScsConfig.serverOnlinePanelX
    private var tempServerOnlinePanelY = ScsConfig.serverOnlinePanelY
    
    // Чекбоксы для включения/выключения панелей
    private lateinit var showMainPanelCheckbox: CheckboxWidget
    private lateinit var showOnlinePanelCheckbox: CheckboxWidget
    private lateinit var showServerOnlinePanelCheckbox: CheckboxWidget
    
    override fun init() {
        super.init()
        
        val buttonY = height - 30
        val checkboxY = 40
        val spacing = 25
        
        // Чекбоксы для показа панелей
        showMainPanelCheckbox = CheckboxWidget.builder(
            Text.literal("Показывать основную панель"),
            textRenderer
        ).pos(20, checkboxY)
            .checked(ScsConfig.showMainPanel)
            .callback { _, checked ->
                ScsConfig.showMainPanel = checked
                ScsConfig.save()
            }
            .build()
        addDrawableChild(showMainPanelCheckbox)
        
        showOnlinePanelCheckbox = CheckboxWidget.builder(
            Text.literal("Показывать панель онлайн"),
            textRenderer
        ).pos(20, checkboxY + spacing)
            .checked(ScsConfig.showOnlinePanel)
            .callback { _, checked ->
                ScsConfig.showOnlinePanel = checked
                ScsConfig.save()
            }
            .build()
        addDrawableChild(showOnlinePanelCheckbox)
        
        showServerOnlinePanelCheckbox = CheckboxWidget.builder(
            Text.literal("Показывать панель онлайна на сервере"),
            textRenderer
        ).pos(20, checkboxY + spacing * 2)
            .checked(ScsConfig.showServerOnlinePanel)
            .callback { _, checked ->
                ScsConfig.showServerOnlinePanel = checked
                ScsConfig.save()
            }
            .build()
        addDrawableChild(showServerOnlinePanelCheckbox)
        
        // Кнопки управления позицией
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Сбросить позиции"),
            { resetPositions() }
        ).dimensions(20, checkboxY + spacing * 3, 150, 20)
            .build())
        
        // Кнопки для точной настройки позиций "Основная панель"
        val mainButtonsY = checkboxY + spacing * 4
        addDrawableChild(ButtonWidget.builder(
            Text.literal("▲ Основная"),
            { moveMainPanel(0, -10) }
        ).dimensions(20, mainButtonsY, 70, 20)
            .build())
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("▼ Основная"),
            { moveMainPanel(0, 10) }
        ).dimensions(95, mainButtonsY, 70, 20)
            .build())
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("◄ Основная"),
            { moveMainPanel(-10, 0) }
        ).dimensions(20, mainButtonsY + 25, 70, 20)
            .build())
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("► Основная"),
            { moveMainPanel(10, 0) }
        ).dimensions(95, mainButtonsY + 25, 70, 20)
            .build())
        
        // Кнопки для онлайн панели
        val onlineButtonsY = checkboxY + spacing * 6
        addDrawableChild(ButtonWidget.builder(
            Text.literal("▲ Онлайн"),
            { moveOnlinePanel(0, -10) }
        ).dimensions(20, onlineButtonsY, 70, 20)
            .build())
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("▼ Онлайн"),
            { moveOnlinePanel(0, 10) }
        ).dimensions(95, onlineButtonsY, 70, 20)
            .build())
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("◄ Онлайн"),
            { moveOnlinePanel(-10, 0) }
        ).dimensions(20, onlineButtonsY + 25, 70, 20)
            .build())
        
        addDrawableChild(ButtonWidget.builder(
            Text.literal("► Онлайн"),
            { moveOnlinePanel(10, 0) }
        ).dimensions(95, onlineButtonsY + 25, 70, 20)
            .build())
        
        // Кнопка закрытия
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Закрыть"),
            { close() }
        ).dimensions(width - 110, buttonY, 100, 20)
            .build())
        
        // Кнопка сохранения
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Сохранить"),
            { saveAndClose() }
        ).dimensions(width - 220, buttonY, 100, 20)
            .build())
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        
        // Заголовок
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Настройки HUD").formatted(Formatting.BOLD, Formatting.GOLD),
            width / 2,
            10,
            0xFFFFFF
        )
        
        // Инструкция
        val instruction = Text.literal("Используйте кнопки стрелок для перемещения панелей | Зажмите ЛКМ на панели для перетаскивания")
            .formatted(Formatting.GRAY)
        context.drawTextWithShadow(
            textRenderer,
            instruction,
            20,
            height - 60,
            0xFFFFFF
        )
        
        // Показываем предпросмотр панелей на игровом экране
        if (ScsConfig.enableHud) {
            renderPreviewPanels(context, mouseX, mouseY)
        }
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && ScsConfig.enableHud) { // ЛКМ
            val client = net.minecraft.client.MinecraftClient.getInstance()
            val window = client.window
            val screenWidth = window.scaledWidth
            val screenHeight = window.scaledHeight
            
            // Проверяем клик по основной панели
            if (ScsConfig.showMainPanel) {
                val x = if (tempMainPanelX < 0) screenWidth + tempMainPanelX else tempMainPanelX
                val y = if (tempMainPanelY < 0) screenHeight + tempMainPanelY else tempMainPanelY
                
                if (mouseX.toInt() in x..(x + 150) && mouseY.toInt() in y..(y + 50)) {
                    draggingPanel = "main"
                    dragOffsetX = mouseX.toInt() - x
                    dragOffsetY = mouseY.toInt() - y
                    return true
                }
            }
            
            // Проверяем клик по онлайн панели
            if (ScsConfig.showOnlinePanel && ScsConfig.enableOnlineStatus) {
                val x = if (tempOnlinePanelX < 0) screenWidth + tempOnlinePanelX else tempOnlinePanelX
                val y = if (tempOnlinePanelY < 0) screenHeight + tempOnlinePanelY else tempOnlinePanelY
                
                if (mouseX.toInt() in x..(x + 250) && mouseY.toInt() in y..(y + 150)) {
                    draggingPanel = "online"
                    dragOffsetX = mouseX.toInt() - x
                    dragOffsetY = mouseY.toInt() - y
                    return true
                }
            }
            
            // Проверяем клик по панели онлайна на сервере
            if (ScsConfig.showServerOnlinePanel && ScsConfig.enableOnlineStatus) {
                val x = if (tempServerOnlinePanelX < 0) screenWidth + tempServerOnlinePanelX else tempServerOnlinePanelX
                val y = if (tempServerOnlinePanelY < 0) screenHeight + tempServerOnlinePanelY else tempServerOnlinePanelY
                
                if (mouseX.toInt() in x..(x + 200) && mouseY.toInt() in y..(y + 20)) {
                    draggingPanel = "serverOnline"
                    dragOffsetX = mouseX.toInt() - x
                    dragOffsetY = mouseY.toInt() - y
                    return true
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (button == 0 && draggingPanel != null) {
            val client = net.minecraft.client.MinecraftClient.getInstance()
            val window = client.window
            val screenWidth = window.scaledWidth
            val screenHeight = window.scaledHeight
            
            when (draggingPanel) {
                "main" -> {
                    var newX = mouseX.toInt() - dragOffsetX
                    var newY = mouseY.toInt() - dragOffsetY
                    
                    // Преобразуем абсолютные координаты в относительные
                    if (newX > screenWidth / 2) {
                        newX = newX - screenWidth
                    }
                    if (newY > screenHeight / 2) {
                        newY = newY - screenHeight
                    }
                    
                    tempMainPanelX = newX.coerceIn(-screenWidth + 160, screenWidth - 160)
                    tempMainPanelY = newY.coerceIn(-screenHeight + 60, screenHeight - 60)
                }
                "online" -> {
                    var newX = mouseX.toInt() - dragOffsetX
                    var newY = mouseY.toInt() - dragOffsetY
                    
                    if (newX > screenWidth / 2) {
                        newX = newX - screenWidth
                    }
                    if (newY > screenHeight / 2) {
                        newY = newY - screenHeight
                    }
                    
                    tempOnlinePanelX = newX.coerceIn(-screenWidth + 260, screenWidth - 260)
                    tempOnlinePanelY = newY.coerceIn(-screenHeight + 160, screenHeight - 160)
                }
                "serverOnline" -> {
                    var newX = mouseX.toInt() - dragOffsetX
                    var newY = mouseY.toInt() - dragOffsetY
                    
                    // Преобразуем абсолютные координаты в относительные
                    if (newX > screenWidth / 2) {
                        newX = newX - screenWidth
                    }
                    if (newY > screenHeight / 2) {
                        newY = newY - screenHeight
                    }
                    
                    tempServerOnlinePanelX = newX.coerceIn(-screenWidth + 210, screenWidth - 210)
                    tempServerOnlinePanelY = newY.coerceIn(-screenHeight + 30, screenHeight - 30)
                }
            }
            return true
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            draggingPanel = null
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }
    
    private fun renderPreviewPanels(context: DrawContext, mouseX: Int, mouseY: Int) {
        val client = net.minecraft.client.MinecraftClient.getInstance()
        val window = client.window
        val screenWidth = window.scaledWidth
        val screenHeight = window.scaledHeight
        
        // Основная панель (превью)
        if (ScsConfig.showMainPanel) {
            val x = if (tempMainPanelX < 0) screenWidth + tempMainPanelX else tempMainPanelX
            val y = if (tempMainPanelY < 0) screenHeight + tempMainPanelY else tempMainPanelY
            
            // Проверяем, находится ли мышь над панелью
            val isHovering = mouseX in x..(x + 150) && mouseY in y..(y + 50)
            val bgColor = if (isHovering) 0xAA0000FF.toInt() else 0xAA000000.toInt()
            
            context.fill(x - 2, y - 2, x + 150, y + 50, bgColor)
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("Основная панель").formatted(Formatting.WHITE),
                x,
                y,
                0xFFFFFF
            )
        }
        
        // Онлайн панель (превью)
        if (ScsConfig.showOnlinePanel && ScsConfig.enableOnlineStatus) {
            val x = if (tempOnlinePanelX < 0) screenWidth + tempOnlinePanelX else tempOnlinePanelX
            val y = if (tempOnlinePanelY < 0) screenHeight + tempOnlinePanelY else tempOnlinePanelY
            
            val isHovering = mouseX in x..(x + 250) && mouseY in y..(y + 150)
            val bgColor = if (isHovering) 0xAA00FF00.toInt() else 0xAA000000.toInt()
            
            context.fill(x - 2, y - 2, x + 250, y + 150, bgColor)
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("Онлайн панель").formatted(Formatting.GREEN),
                x,
                y,
                0xFFFFFF
            )
        }
        
        // Панель онлайна на сервере (превью)
        if (ScsConfig.showServerOnlinePanel && ScsConfig.enableOnlineStatus) {
            val x = if (tempServerOnlinePanelX < 0) screenWidth + tempServerOnlinePanelX else tempServerOnlinePanelX
            val y = if (tempServerOnlinePanelY < 0) screenHeight + tempServerOnlinePanelY else tempServerOnlinePanelY
            
            val isHovering = mouseX in x..(x + 200) && mouseY in y..(y + 20)
            val bgColor = if (isHovering) 0xAA00FF00.toInt() else 0xAA000000.toInt()
            
            context.fill(x - 2, y - 2, x + 200, y + 20, bgColor)
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("Онлайн на сервере").formatted(Formatting.GREEN),
                x,
                y,
                0xFFFFFF
            )
        }
    }
    
    private fun moveMainPanel(dx: Int, dy: Int) {
        tempMainPanelX += dx
        tempMainPanelY += dy
        // Ограничиваем в пределах экрана
        val maxX = width - 160
        val maxY = height - 60
        tempMainPanelX = tempMainPanelX.coerceIn(-width + 160, maxX)
        tempMainPanelY = tempMainPanelY.coerceIn(-height + 60, maxY)
    }
    
    private fun moveOnlinePanel(dx: Int, dy: Int) {
        tempOnlinePanelX += dx
        tempOnlinePanelY += dy
        val maxX = width - 260
        val maxY = height - 160
        tempOnlinePanelX = tempOnlinePanelX.coerceIn(-width + 260, maxX)
        tempOnlinePanelY = tempOnlinePanelY.coerceIn(-height + 160, maxY)
    }
    
    private fun resetPositions() {
        tempMainPanelX = -320
        tempMainPanelY = 6
        tempOnlinePanelX = 320
        tempOnlinePanelY = 6
        tempServerOnlinePanelX = -320
        tempServerOnlinePanelY = 6
    }
    
    private fun saveAndClose() {
        ScsConfig.hudX = tempMainPanelX
        ScsConfig.hudY = tempMainPanelY
        // Сохраняем независимые координаты онлайн панели
        ScsConfig.onlinePanelX = tempOnlinePanelX
        ScsConfig.onlinePanelY = tempOnlinePanelY
        ScsConfig.serverOnlinePanelX = tempServerOnlinePanelX
        ScsConfig.serverOnlinePanelY = tempServerOnlinePanelY
        
        // Сохраняем конфигурацию в файл
        ScsConfig.save()
        
        val client = net.minecraft.client.MinecraftClient.getInstance()
        client.player?.sendMessage(
            net.minecraft.text.Text.literal("§a[ScS] Настройки HUD сохранены")
                .formatted(net.minecraft.util.Formatting.GREEN),
            false
        )
        
        close()
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) { // ESC
            saveAndClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}


package com.scs.client.hud

import com.scs.client.config.ScsConfig
import net.minecraft.client.MinecraftClient

/**
 * Безопасный обработчик мыши для перетаскивания HUD панелей
 * Использует API Minecraft вместо прямых вызовов GLFW
 */
object HudMouseHandler {
    private var draggingPanel: String? = null
    private var dragStartX = 0.0
    private var dragStartY = 0.0
    private var panelStartX = 0
    private var panelStartY = 0
    private var wasMousePressed = false
    private var lastSaveTime = 0L
    
    /**
     * Обновляет обработку мыши каждый тик (безопасный способ)
     * В режиме редактирования освобождаем мышь для свободного перемещения
     */
    fun update(client: MinecraftClient) {
        try {
            // В режиме редактирования освобождаем мышь, чтобы она могла свободно двигаться
            if (ScsConfig.hudEditMode && client.currentScreen == null) {
                val mouse = client.mouse ?: return
                
                // Освобождаем захват мыши для свободного движения
                // Разблокируем курсор каждый тик, чтобы он оставался свободным
                try {
                    // Разблокируем курсор, если он заблокирован
                    if (mouse.isCursorLocked) {
                        mouse.unlockCursor()
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки разблокировки
                }
                
                val window = client.window
                
                // Получаем позицию мыши через Minecraft API (в координатах окна)
                val mouseX = mouse.x * window.scaledWidth / window.width
                val mouseY = mouse.y * window.scaledHeight / window.height
                
                // Проверяем состояние ЛКМ через опции клиента
                val isMousePressed = client.options.attackKey.isPressed
                
                if (isMousePressed && !wasMousePressed) {
                    // Начали зажимать мышь - проверяем клик по панели
                    handleMouseClick(mouseX, mouseY, 0)
                } else if (isMousePressed && wasMousePressed && draggingPanel != null) {
                    // Перетаскивание - обновляем позицию панели
                    handleMouseDrag(mouseX, mouseY)
                } else if (!isMousePressed && wasMousePressed) {
                    // Отпустили мышь - завершаем перетаскивание
                    handleMouseRelease()
                }
                
                wasMousePressed = isMousePressed
            } else {
                // Выходим из режима редактирования - сбрасываем состояние
                if (draggingPanel != null) {
                    draggingPanel = null
                }
                wasMousePressed = false
            }
        } catch (e: Exception) {
            // Игнорируем ошибки обработки мыши
        }
    }
    
    /**
     * Обрабатывает клик мыши
     */
    private fun handleMouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!ScsConfig.hudEditMode || button != 0) return false
        
        val client = MinecraftClient.getInstance()
        val window = client.window
        val screenWidth = window.scaledWidth
        val screenHeight = window.scaledHeight
        
        // Проверяем клик по основной панели
        if (ScsConfig.showMainPanel) {
            val x = if (ScsConfig.hudX < 0) screenWidth + ScsConfig.hudX else ScsConfig.hudX
            val y = if (ScsConfig.hudY < 0) screenHeight + ScsConfig.hudY else ScsConfig.hudY
            
            // Предполагаемая ширина панели (примерная)
            val panelWidth = 200
            val panelHeight = 100
            
            if (mouseX.toInt() in x..(x + panelWidth) && mouseY.toInt() in y..(y + panelHeight)) {
                draggingPanel = "main"
                dragStartX = mouseX
                dragStartY = mouseY
                panelStartX = ScsConfig.hudX
                panelStartY = ScsConfig.hudY
                return true
            }
        }
        
        // Проверяем клик по онлайн панели
        if (ScsConfig.showOnlinePanel && ScsConfig.enableOnlineStatus) {
            val x = if (ScsConfig.onlinePanelX < 0) screenWidth + ScsConfig.onlinePanelX else ScsConfig.onlinePanelX
            val y = if (ScsConfig.onlinePanelY < 0) screenHeight + ScsConfig.onlinePanelY else ScsConfig.onlinePanelY
            
            val panelWidth = 250
            // Вычисляем реальную высоту панели на основе количества игроков
            val players = com.scs.client.online.OnlineStatusService.onlinePlayers.size
            val panelHeight = if (players == 0) {
                30 // Высота для пустой панели
            } else {
                // Приблизительная высота: заголовок + игроки (по 15px каждый + 10px если есть сервер)
                20 + players * 20
            }.coerceAtMost(200) // Максимальная высота для клика
            
            if (mouseX.toInt() in x..(x + panelWidth) && mouseY.toInt() in y..(y + panelHeight)) {
                draggingPanel = "online"
                dragStartX = mouseX
                dragStartY = mouseY
                panelStartX = ScsConfig.onlinePanelX
                panelStartY = ScsConfig.onlinePanelY
                return true
            }
        }
        
        return false
    }
    
    /**
     * Обрабатывает перетаскивание мыши (вызывается из миксина Screen)
     */
    fun handleMouseDrag(mouseX: Double, mouseY: Double): Boolean {
        if (!ScsConfig.hudEditMode || draggingPanel == null) return false
        
        val client = MinecraftClient.getInstance()
        val window = client.window
        val screenWidth = window.scaledWidth
        val screenHeight = window.scaledHeight
        
        val deltaX = mouseX - dragStartX
        val deltaY = mouseY - dragStartY
        
        when (draggingPanel) {
            "main" -> {
                var newX = panelStartX + deltaX.toInt()
                var newY = panelStartY + deltaY.toInt()
                
                // Преобразуем абсолютные координаты в относительные
                if (newX > screenWidth / 2) {
                    newX = (newX - screenWidth).toInt()
                }
                if (newY > screenHeight / 2) {
                    newY = (newY - screenHeight).toInt()
                }
                
                ScsConfig.hudX = newX.coerceIn(-screenWidth + 200, screenWidth - 200)
                ScsConfig.hudY = newY.coerceIn(-screenHeight + 100, screenHeight - 100)
                
                // Сохраняем конфигурацию при перетаскивании (с задержкой, чтобы не спамить)
                if (System.currentTimeMillis() - lastSaveTime > 500) {
                    ScsConfig.save()
                    lastSaveTime = System.currentTimeMillis()
                }
            }
            "online" -> {
                var newX = panelStartX + deltaX.toInt()
                var newY = panelStartY + deltaY.toInt()
                
                if (newX > screenWidth / 2) {
                    newX = (newX - screenWidth).toInt()
                }
                if (newY > screenHeight / 2) {
                    newY = (newY - screenHeight).toInt()
                }
                
                ScsConfig.onlinePanelX = newX.coerceIn(-screenWidth + 250, screenWidth - 250)
                ScsConfig.onlinePanelY = newY.coerceIn(-screenHeight + 150, screenHeight - 150)
                
                // Сохраняем конфигурацию при перетаскивании (с задержкой, чтобы не спамить)
                if (System.currentTimeMillis() - lastSaveTime > 500) {
                    ScsConfig.save()
                    lastSaveTime = System.currentTimeMillis()
                }
            }
        }
        
        return true
    }
    
    /**
     * Обрабатывает отпускание мыши (вызывается из миксина Screen)
     */
    fun handleMouseRelease(): Boolean {
        if (draggingPanel != null) {
            draggingPanel = null
            return true
        }
        return false
    }
    
    fun isDragging(): Boolean = draggingPanel != null
}


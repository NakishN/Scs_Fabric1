package com.scs.client.hud

import com.scs.client.config.ScsConfig
import net.minecraft.client.MinecraftClient

/**
 * Обработчик перетаскивания HUD панелей в режиме редактирования
 * УСТАРЕВШЕЕ - используется HudMouseHandler вместо прямых вызовов GLFW
 */
@Deprecated("Используйте HudMouseHandler вместо прямых вызовов GLFW")
object HudDragHandler {
    private var draggingPanel: String? = null
    private var dragStartX = 0
    private var dragStartY = 0
    private var panelStartX = 0
    private var panelStartY = 0

    fun onMouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!ScsConfig.hudEditMode || button != 0) return false

        val client = MinecraftClient.getInstance()
        val window = client.window
        val screenWidth = window.scaledWidth
        val screenHeight = window.scaledHeight


        if (ScsConfig.showMainPanel) {
            val x = if (ScsConfig.hudX < 0) screenWidth + ScsConfig.hudX else ScsConfig.hudX
            val y = if (ScsConfig.hudY < 0) screenHeight + ScsConfig.hudY else ScsConfig.hudY


            val panelWidth = 200
            val panelHeight = 100

            if (mouseX.toInt() in x..(x + panelWidth) && mouseY.toInt() in y..(y + panelHeight)) {
                draggingPanel = "main"
                dragStartX = mouseX.toInt()
                dragStartY = mouseY.toInt()
                panelStartX = ScsConfig.hudX
                panelStartY = ScsConfig.hudY
                return true
            }
        }

        return false
    }

    fun onMouseDragged(mouseX: Double, mouseY: Double): Boolean {
        if (!ScsConfig.hudEditMode || draggingPanel == null) return false

        val client = MinecraftClient.getInstance()
        val window = client.window
        val screenWidth = window.scaledWidth
        val screenHeight = window.scaledHeight

        val deltaX = mouseX.toInt() - dragStartX
        val deltaY = mouseY.toInt() - dragStartY

        when (draggingPanel) {
            "main" -> {
                var newX = panelStartX + deltaX
                var newY = panelStartY + deltaY


                if (newX > screenWidth / 2) {
                    newX = newX - screenWidth
                }
                if (newY > screenHeight / 2) {
                    newY = newY - screenHeight
                }

                ScsConfig.hudX = newX.coerceIn(-screenWidth + 200, screenWidth - 200)
                ScsConfig.hudY = newY.coerceIn(-screenHeight + 100, screenHeight - 100)
            }
        }

        return true
    }

    fun onMouseReleased(): Boolean {
        if (draggingPanel != null) {
            draggingPanel = null
            return true
        }
        return false
    }

    fun isDragging(): Boolean = draggingPanel != null
    fun getDraggingPanel(): String? = draggingPanel
}


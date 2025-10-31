package com.scs.client.event

import com.scs.client.ScsClient
import com.scs.client.config.ScsConfig
import com.scs.client.hud.HudRenderer
import com.scs.client.monitor.ChatMonitor
import com.scs.client.shaurma.ShaurmaSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object KeyInputHandler {

    fun onEndTick(client: MinecraftClient) {
        if (client.player == null) return

        // Toggle HUD (F8)
        while (ScsClient.toggleHudKey.wasPressed()) {
            HudRenderer.toggleHud()
        }

        // Show History (F9)
        while (ScsClient.showHistoryKey.wasPressed()) {
            // TODO: Открыть экран истории
            // client.setScreen(ChatHistoryScreen())
            client.player?.sendMessage(
                Text.literal("§e[ScS] История: в разработке. Записей: ${ChatMonitor.entries.size}"),
                false
            )
        }

        // Clear Entries (F10)
        while (ScsClient.clearEntriesKey.wasPressed()) {
            ChatMonitor.clearEntries()
            client.player?.sendMessage(
                Text.literal("§e[ScS] История очищена!"),
                false
            )
        }

        // Shaurma Tap (U)
        if (ScsConfig.enableShaurma) {
            while (ScsClient.shaurmaTapKey.wasPressed()) {
                ShaurmaSystem.onShaurmaTap()
            }

            // Shaurma Menu (Y)
            while (ScsClient.shaurmaMenuKey.wasPressed()) {
                // TODO: Открыть меню шаурмы
                // client.setScreen(ShaurmaMenuScreen())
                client.player?.sendMessage(
                    Text.literal("§6[ScS] Меню шаурмы: в разработке. Всего: ${ShaurmaSystem.shaurmaCount} 🌯"),
                    false
                )
            }
        }
    }
}
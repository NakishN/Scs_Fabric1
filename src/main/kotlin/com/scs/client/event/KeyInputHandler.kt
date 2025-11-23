package com.scs.client.event

import com.scs.client.KeyBindings
import com.scs.client.config.ScsConfig
import com.scs.client.monitor.ChatMonitor
import com.scs.client.shaurma.ShaurmaSystem
import net.minecraft.client.MinecraftClient

object KeyInputHandler {
    private var prevToggleHud = false
    private var prevShowHistory = false
    private var prevClearEntries = false
    private var prevShaurmaTap = false
    private var prevShaurmaMenu = false
    private var prevHudConfig = false

    fun onEndTick(client: MinecraftClient) {

        val currToggleHud = KeyBindings.toggleHudKey.isPressed
        if (currToggleHud && !prevToggleHud) {
            ScsConfig.enableHud = !ScsConfig.enableHud
            ScsConfig.save()
            client.player?.sendMessage(
                net.minecraft.text.Text.literal(
                    if (ScsConfig.enableHud) "§a[HUD] Включен" else "§c[HUD] Выключен"
                ),
                false
            )
        }
        prevToggleHud = currToggleHud


        val currShowHistory = KeyBindings.showHistoryKey.isPressed
        if (currShowHistory && !prevShowHistory) {

            client.setScreen(com.scs.client.gui.ChatHistoryScreen(client.currentScreen))
        }
        prevShowHistory = currShowHistory


        val currClearEntries = KeyBindings.clearEntriesKey.isPressed
        if (currClearEntries && !prevClearEntries) {
            ChatMonitor.clearEntries()
            client.player?.sendMessage(
                net.minecraft.text.Text.literal("§c[ScS] Все записи очищены"),
                false
            )
        }
        prevClearEntries = currClearEntries


        val currShaurmaTap = KeyBindings.shaurmaTapKey.isPressed
        if (currShaurmaTap && !prevShaurmaTap) {
            ShaurmaSystem.onShaurmaTap()
        }
        prevShaurmaTap = currShaurmaTap


        val currShaurmaMenu = KeyBindings.shaurmaMenuKey.isPressed
        if (currShaurmaMenu && !prevShaurmaMenu) {
            val stats = """
                §e=== ШАУРМА СТАТИСТИКА ===
                §6Всего шаурмы: §f${ShaurmaSystem.shaurmaCount}
                §6Всего тапов: §f${ShaurmaSystem.totalTaps}
                §6Среднее: §f${String.format("%.2f", ShaurmaSystem.getAveragePerTap())}
            """.trimIndent()
            client.player?.sendMessage(
                net.minecraft.text.Text.literal(stats),
                false
            )
        }
        prevShaurmaMenu = currShaurmaMenu


        val currHudConfig = KeyBindings.hudConfigKey.isPressed
        if (currHudConfig && !prevHudConfig) {

            client.setScreen(com.scs.client.gui.HudConfigScreen(client.currentScreen))
        }
        prevHudConfig = currHudConfig
    }
}
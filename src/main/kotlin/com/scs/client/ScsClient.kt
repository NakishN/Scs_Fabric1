package com.scs.client

import com.scs.Scs
import com.scs.client.command.CommandHandler
import com.scs.client.command.CommandScheduler
import com.scs.client.config.ScsConfig
import com.scs.client.event.ChatEventHandler
import com.scs.client.event.KeyInputHandler
import com.scs.client.hud.HudDragHandler
import com.scs.client.hud.HudRenderer
import com.scs.client.obfuscation.AntiDeobfuscator
import com.scs.client.online.OnlineStatusService
import com.scs.client.shaurma.ShaurmaSystem
import com.scs.client.whitelist.WhitelistChecker
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback


open class ScsClient : ClientModInitializer {

    override fun onInitializeClient() {
        try {
            AntiDeobfuscator.checkIntegrity()

            ScsConfig.load()

            WhitelistChecker.checkWhitelist()

            registerKeybindings()

            ShaurmaSystem.load()

            registerEventHandlers()

            CommandHandler.register()

            ChatEventHandler.register()


            HudRenderCallback.EVENT.register { drawContext, _ ->
                HudRenderer.render(drawContext, 0.0f)
            }

            ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
                if (client.currentServerEntry != null) {
                    WhitelistChecker.checkWhitelistOnJoin(client)
                }

                OnlineStatusService.start()
            }

            ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
                OnlineStatusService.stop()
            }

            ClientTickEvents.END_CLIENT_TICK.register { client ->
                if (ScsConfig.hudEditMode && client.currentScreen == null && client.mouse != null) {
                    com.scs.client.hud.HudMouseHandler.update(client)
                }
            }

        } catch (e: Exception) {
            throw e
        }
    }

    private fun registerKeybindings() {
        KeyBindings.initialize()

        KeyBindingHelper.registerKeyBinding(KeyBindings.toggleHudKey)
        KeyBindingHelper.registerKeyBinding(KeyBindings.showHistoryKey)
        KeyBindingHelper.registerKeyBinding(KeyBindings.clearEntriesKey)
        KeyBindingHelper.registerKeyBinding(KeyBindings.shaurmaTapKey)
        KeyBindingHelper.registerKeyBinding(KeyBindings.shaurmaMenuKey)
        KeyBindingHelper.registerKeyBinding(KeyBindings.hudConfigKey)
    }

    private fun registerEventHandlers() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            KeyInputHandler.onEndTick(client)

            CommandScheduler.processQueue(client)

            com.scs.client.monitor.CheckSession.checkTimeout()

            OnlineStatusService.tick()
        }
    }
}
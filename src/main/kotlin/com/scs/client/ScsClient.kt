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
            // Проверка защиты от деобфускации
            AntiDeobfuscator.checkIntegrity()
            
            // 0. Загружаем конфигурацию
            ScsConfig.load()
            
            // 0.5. Проверяем вайтлист при загрузке мода (крашит игру, если пользователь не в вайтлисте)
            WhitelistChecker.checkWhitelist()
            
            // 1. Регистрируем горячие клавиши
            registerKeybindings()
            
            // 3. Загружаем данные шаурмы
            ShaurmaSystem.load()

            // 4. Регистрируем обработчики событий
            registerEventHandlers()
            
            // 5. Регистрируем обработчик команд
            CommandHandler.register()
            
            // 6. Регистрируем обработчик чата
            ChatEventHandler.register()
            
            // 7. Сервис онлайн статуса запустится автоматически при подключении к серверу
            // (не запускаем при старте клиента, только при подключении к серверу)
            
            // 8. Регистрируем HUD рендерер
            HudRenderCallback.EVENT.register { drawContext, _ ->
                HudRenderer.render(drawContext, 0.0f)
            }
            
            // 9. Регистрируем обработчики подключения/отключения для онлайн статуса
            ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
                // Дополнительная проверка вайтлиста при подключении к серверу (на случай смены ника)
                if (client.currentServerEntry != null) {
                    WhitelistChecker.checkWhitelistOnJoin(client)
                }
                
                // Перезапускаем сервис при подключении к серверу
                OnlineStatusService.start()
            }
            
            ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
                OnlineStatusService.stop()
            }
            
            // 10. Регистрируем обработчик мыши для перетаскивания HUD (безопасный способ)
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                // Обрабатываем перетаскивание только в режиме редактирования и без экрана
                if (ScsConfig.hudEditMode && client.currentScreen == null && client.mouse != null) {
                    com.scs.client.hud.HudMouseHandler.update(client)
                }
            }

        } catch (e: Exception) {
            throw e
        }
    }

    private fun registerKeybindings() {
        // Инициализируем KeyBindings перед регистрацией
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
            // Обрабатываем очередь команд каждый тик
            CommandScheduler.processQueue(client)
            // Проверяем таймаут проверки
            com.scs.client.monitor.CheckSession.checkTimeout()
            // Обновляем онлайн статус
            OnlineStatusService.tick()
        }
    }
}
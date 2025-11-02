package com.scs.client

import com.scs.Scs
import com.scs.client.command.CommandHandler
import com.scs.client.command.CommandScheduler
import com.scs.client.config.ScsConfig
import com.scs.client.event.ChatEventHandler
import com.scs.client.event.KeyInputHandler
import com.scs.client.hud.HudDragHandler
import com.scs.client.hud.HudRenderer
import com.scs.client.online.OnlineStatusService
import com.scs.client.shaurma.ShaurmaSystem
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback


open class ScsClient : ClientModInitializer {

    override fun onInitializeClient() {
        Scs.LOGGER.info("=== ScS Client Initialization Started ===")

        try {
            // 1. Регистрируем горячие клавиши
            registerKeybindings()
            Scs.LOGGER.info("✓ Keybindings registered")

            // 2. Загружаем конфигурацию
            ScsConfig.load()
            Scs.LOGGER.info("✓ Config loaded")
            
            // 3. Загружаем данные шаурмы
            ShaurmaSystem.load()
            Scs.LOGGER.info("✓ Shaurma system loaded")

            // 4. Регистрируем обработчики событий
            registerEventHandlers()
            Scs.LOGGER.info("✓ Event handlers registered")
            
            // 5. Регистрируем обработчик команд
            CommandHandler.register()
            Scs.LOGGER.info("✓ Command handler registered")
            
            // 6. Регистрируем обработчик чата
            ChatEventHandler.register()
            Scs.LOGGER.info("✓ Chat event handler registered")
            
            // 7. Сервис онлайн статуса запустится автоматически при подключении к серверу
            // (не запускаем при старте клиента, только при подключении к серверу)
            Scs.LOGGER.info("✓ Online status service ready (will start on server connect)")
            
            // 8. Регистрируем HUD рендерер
            HudRenderCallback.EVENT.register { drawContext, _ ->
                HudRenderer.render(drawContext, 0.0f)
            }
            Scs.LOGGER.info("✓ HUD renderer registered")
            
            // 9. Регистрируем обработчики подключения/отключения для онлайн статуса
            ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
                // Перезапускаем сервис при подключении к серверу
                OnlineStatusService.start()
                Scs.LOGGER.info("✓ Online status service started on connect")
            }
            
            ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
                OnlineStatusService.stop()
                Scs.LOGGER.info("✓ Online status service stopped on disconnect")
            }
            
            // 10. Регистрируем обработчик мыши для перетаскивания HUD (безопасный способ)
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                // Обрабатываем перетаскивание только в режиме редактирования и без экрана
                if (ScsConfig.hudEditMode && client.currentScreen == null && client.mouse != null) {
                    com.scs.client.hud.HudMouseHandler.update(client)
                }
            }

            Scs.LOGGER.info("=== ScS Client Initialized Successfully ===")
            Scs.LOGGER.info("Hotkeys: F8=Toggle HUD | F9=History | F10=Clear | U=Shaurma Tap | Y=Menu")

        } catch (e: Exception) {
            Scs.LOGGER.error("Failed to initialize ScS Client", e)
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
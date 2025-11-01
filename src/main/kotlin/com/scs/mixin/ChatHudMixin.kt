package com.scs.mixin

import com.scs.client.monitor.ChatMonitor
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.network.message.MessageSignatureData
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ChatHud::class)
class ChatHudMixin {
    @Inject(
        method = ["addMessage(Lnet/minecraft/class_2561;)V"],
        at = [At("HEAD")]
    )
    private fun onAddMessage(
        message: Text,
        ci: CallbackInfo
    ) {
        try {
            val messageText = message.string
            if (messageText.isNotBlank()) {
                ChatMonitor.processMessage(messageText, "CHAT_HUD")
            }
        } catch (_: Exception) {
        }
    }
}
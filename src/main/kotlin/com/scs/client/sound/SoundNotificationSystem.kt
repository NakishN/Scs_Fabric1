package com.scs.client.sound

import com.scs.Scs
import com.scs.client.config.ScsConfig
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Система звуковых уведомлений для критичных событий
 */
object SoundNotificationSystem {

    /**
     * Проигрывает звук для серьезного нарушения
     */
    fun playSeriousViolationSound() {
        if (!ScsConfig.soundAlerts) return

        try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return
            val world = client.world ?: return
            val pos = player.blockPos


            world.playSound(
                player,
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble(),
                SoundEvents.ENTITY_VILLAGER_NO,
                net.minecraft.sound.SoundCategory.MASTER,
                0.8f,
                0.5f,
                0L
            )
        } catch (e: Exception) {
        }
    }

    /**
     * Проигрывает звук для DupeIP обнаружения
     */
    fun playDupeIPSound() {
        if (!ScsConfig.soundAlerts) return

        try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return
            val world = client.world ?: return
            val pos = player.blockPos


            world.playSound(
                player,
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                net.minecraft.sound.SoundCategory.MASTER,
                0.6f,
                1.2f,
                0L
            )
        } catch (e: Exception) {
        }
    }

    /**
     * Проигрывает звук для обнаружения нарушений античита
     */
    fun playViolationSound(isSerious: Boolean) {
        if (!ScsConfig.soundAlerts) return

        try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return
            val world = client.world ?: return

            if (isSerious) {
                playSeriousViolationSound()
            } else {
                val pos = player.blockPos

                world.playSound(
                    player,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL,
                    net.minecraft.sound.SoundCategory.MASTER,
                    0.5f,
                    1.0f,
                    0L
                )
            }
        } catch (e: Exception) {
        }
    }
}


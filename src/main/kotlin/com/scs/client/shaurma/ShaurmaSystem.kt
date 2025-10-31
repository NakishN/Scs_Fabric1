package com.scs.client.shaurma

import com.scs.ScsMod
import com.scs.client.config.ScsConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.random.Random

object ShaurmaSystem {

    private val savePath: Path = FabricLoader.getInstance()
        .configDir.resolve("scs-shaurma.dat")

    var shaurmaCount: Long = 0
        private set
    var totalTaps: Long = 0
        private set
    private var lastSaveTime: Long = 0

    private val bonusMultipliers = intArrayOf(2, 3, 5, 10)
    private val bonusMessages = arrayOf(
        "ДВОЙНАЯ ШАУРМА! 🌯🌯",
        "ТРОЙНАЯ ШАУРМА! 🌯🌯🌯",
        "МЕГА ШАУРМА! 🌯✨",
        "ЛЕГЕНДАРНАЯ ШАУРМА! 🌯⭐"
    )

    private val tapMessages = arrayOf(
        "Вкусная шаурма! 🌯",
        "Сочная шаурма! 🌯💧",
        "Ароматная шаурма! 🌯🔥",
        "Питательная шаурма! 🌯💪",
        "Свежая шаурма! 🌯🌿",
        "Острая шаурма! 🌯🌶️",
        "Сытная шаурма! 🌯😋"
    )

    fun onShaurmaTap() {
        if (!ScsConfig.enableShaurma) return

        totalTaps++

        var reward = ScsConfig.shaurmaBaseReward
        var message: String

        if (Random.nextInt(100) < ScsConfig.shaurmaBonusChance) {
            // Бонусная шаурма!
            val bonusIndex = Random.nextInt(bonusMultipliers.size)
            reward = ScsConfig.shaurmaBaseReward * bonusMultipliers[bonusIndex]
            message = bonusMessages[bonusIndex]

            if (ScsConfig.shaurmaSounds) {
                playBonusSound()
            }

            ScsMod.LOGGER.info(
                "[ScS] Shaurma BONUS: +{}x{} = {} (total: {})",
                ScsConfig.shaurmaBaseReward,
                bonusMultipliers[bonusIndex],
                reward,
                shaurmaCount + reward
            )
        } else {
            // Обычная шаурма
            message = tapMessages[Random.nextInt(tapMessages.size)]
            if (ScsConfig.shaurmaSounds) {
                playTapSound()
            }
        }

        shaurmaCount += reward

        if (ScsConfig.shaurmaChatMessages) {
            sendShaurmaMessage(message, reward)
        }

        // Сохраняем каждые 10 тапов
        if (totalTaps % 10 == 0L) {
            save()
            if (totalTaps % 50 == 0L) {
                ScsMod.LOGGER.info("[ScS] Shaurma milestone: $totalTaps taps, $shaurmaCount shaurma total")
            }
        }
    }

    private fun sendShaurmaMessage(message: String, reward: Int) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        // Основное сообщение
        val mainMessage = Text.literal("✨ $message ✨").apply {
            style = if (reward > ScsConfig.shaurmaBaseReward) {
                style.withColor(Formatting.GOLD).withBold(true)
            } else {
                style.withColor(Formatting.YELLOW)
            }
        }

        // Сообщение о награде
        val rewardMessage = Text.literal("▶ +$reward шаурмы! Всего: $shaurmaCount 🌯").apply {
            style = style.withColor(Formatting.GREEN).withItalic(true)
        }

        player.sendMessage(mainMessage, false)
        player.sendMessage(rewardMessage, false)

        // Если большой бонус
        if (reward >= 10) {
            val epicMessage = Text.literal("🎉 ЭПИЧЕСКАЯ НАГРАДА! 🎉").apply {
                style = style.withColor(Formatting.LIGHT_PURPLE).withBold(true)
            }
            player.sendMessage(epicMessage, false)
        }
    }

    private fun playTapSound() {
        try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return
            val world = client.world ?: return

            world.playSound(
                player,
                player.blockPos,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                net.minecraft.sound.SoundCategory.MASTER,
                0.3f,
                1.2f + Random.nextFloat() * 0.3f
            )
        } catch (e: Exception) {
            if (totalTaps % 100 == 0L) {
                ScsMod.LOGGER.warn("[ScS] Sound issues detected (logged every 100 taps)")
            }
        }
    }

    private fun playBonusSound() {
        try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return
            val world = client.world ?: return

            world.playSound(
                player,
                player.blockPos,
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                net.minecraft.sound.SoundCategory.MASTER,
                0.5f,
                1.5f
            )

            // Дополнительный звук через задержку
            Thread {
                Thread.sleep(200)
                try {
                    world.playSound(
                        player,
                        player.blockPos,
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sound.SoundCategory.MASTER,
                        0.4f,
                        2.0f
                    )
                } catch (e: Exception) {
                    // Игнорируем
                }
            }.start()
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    fun save() {
        try {
            val data = "$shaurmaCount:$totalTaps:${System.currentTimeMillis()}"
            savePath.writeText(data)
            lastSaveTime = System.currentTimeMillis()
        } catch (e: Exception) {
            ScsMod.LOGGER.error("[ScS] Failed to save shaurma data", e)
        }
    }

    fun load() {
        try {
            if (savePath.exists()) {
                val data = savePath.readText()
                val parts = data.split(":")

                if (parts.size >= 2) {
                    shaurmaCount = parts[0].toLongOrNull() ?: 0
                    totalTaps = parts[1].toLongOrNull() ?: 0
                    if (parts.size >= 3) {
                        lastSaveTime = parts[2].toLongOrNull() ?: 0
                    }

                    ScsMod.LOGGER.info("[ScS] Loaded shaurma data: $shaurmaCount shaurma, $totalTaps taps")
                }
            } else {
                ScsMod.LOGGER.info("[ScS] Starting fresh shaurma session!")
            }
        } catch (e: Exception) {
            ScsMod.LOGGER.error("[ScS] Failed to load shaurma data, starting fresh", e)
            shaurmaCount = 0
            totalTaps = 0
        }
    }

    fun getAveragePerTap(): Double {
        return if (totalTaps > 0) shaurmaCount.toDouble() / totalTaps else 0.0
    }

    fun hasAchievement(achievement: String): Boolean {
        return when (achievement) {
            "first_tap" -> totalTaps >= 1
            "hundred_taps" -> totalTaps >= 100
            "thousand_taps" -> totalTaps >= 1000
            "hundred_shaurma" -> shaurmaCount >= 100
            "thousand_shaurma" -> shaurmaCount >= 1000
            "ten_thousand_shaurma" -> shaurmaCount >= 10000
            else -> false
        }
    }

    fun resetData() {
        val oldShaurma = shaurmaCount
        val oldTaps = totalTaps

        shaurmaCount = 0
        totalTaps = 0
        save()

        ScsMod.LOGGER.info("[ScS] Shaurma data reset: was $oldShaurma shaurma, $oldTaps taps")

        val client = MinecraftClient.getInstance()
        client.player?.sendMessage(
            Text.literal("🔄 Данные шаурмы сброшены!").apply {
                style = style.withColor(Formatting.RED)
            },
            false
        )
    }
}
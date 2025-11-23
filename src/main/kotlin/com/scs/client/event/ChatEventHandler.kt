package com.scs.client.event

import com.scs.client.command.ChatButtonHandler
import com.scs.client.config.ScsConfig
import com.scs.client.monitor.ChatMonitor
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.text.Text
import java.util.regex.Pattern

object ChatEventHandler {




    private val violationPattern = Pattern.compile(
        """\[[^\]]*(?:анти|чит)[^\]]*\]\s*([a-zA-Z0-9_]+)\s*[:：]\s*(.+?)(?:\s*\(([^)]+)\))?(?:\s*#(\d+))?""",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )


    private val violationPattern2 = Pattern.compile(
        """\[[^\]]*(?:анти|чит)[^\]]*\]\s*([a-zA-Z0-9_]+)\s+(.+?)(?:\s*\(([^)]+)\))?(?:\s*#(\d+))?""",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val dupeIPScanPattern = Pattern.compile(
        """Сканирование\s+(\w+).*""",
        Pattern.CASE_INSENSITIVE
    )

    private val dupeIPResultsPattern = Pattern.compile(
        """^([A-Za-z0-9_]+(?:,\s*[A-Za-z0-9_]+)+)$"""
    )

    private var lastScannedPlayer: String? = null
    private var lastDupeIPTime: Long = 0

    fun register() {

        ClientReceiveMessageEvents.MODIFY_GAME.register { message: Text, _ ->
            try {

                val messageText = message.string

                if (messageText.isNotBlank()) {

                    ChatMonitor.processMessage(messageText, "CHAT")


                    if (ScsConfig.enableChatButtons) {
                        return@register modifyMessageWithButtons(message, messageText)
                    }
                }
            } catch (e: Exception) {

            }
            message
        }
    }

    private fun modifyMessageWithButtons(originalMessage: Text, messageText: String): Text {

        val dupeScanMatcher = dupeIPScanPattern.matcher(messageText)
        if (dupeScanMatcher.matches()) {
            val player = dupeScanMatcher.group(1)
            lastScannedPlayer = player
            lastDupeIPTime = System.currentTimeMillis()
            return originalMessage
        }


        if (lastScannedPlayer != null &&
            System.currentTimeMillis() - lastDupeIPTime < 15000) {
            val resultsMatcher = dupeIPResultsPattern.matcher(messageText.trim())
            if (resultsMatcher.matches()) {
                val nicknames = messageText.trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (nicknames.size >= 1) {
                    val dupeIPMessage = ChatButtonHandler.createDupeIPMessage(lastScannedPlayer!!, nicknames)

                    lastScannedPlayer = null
                    return originalMessage.copy().append(Text.literal(" ")).append(dupeIPMessage)
                }
            }
        }


        val violationMatcher = violationPattern.matcher(messageText)
        if (violationMatcher.find()) {
            val playerName = violationMatcher.group(1)
            val violationText = violationMatcher.group(2) ?: ""
            val detectionType = if (violationMatcher.groupCount() > 2 && violationMatcher.group(3) != null) violationMatcher.group(3) else null

            if (playerName != null && violationText.isNotEmpty() && playerName.length in 3..16) {
                val buttonMessage = ChatButtonHandler.createViolationMessage(
                    playerName,
                    violationText.trim(),
                    detectionType
                )


                return originalMessage.copy().append(Text.literal(" ")).append(buttonMessage)
            }
        }


        val violationMatcher2 = violationPattern2.matcher(messageText)
        if (violationMatcher2.find()) {
            val playerName = violationMatcher2.group(1)
            val violationText = violationMatcher2.group(2) ?: ""
            val detectionType = if (violationMatcher2.groupCount() > 2 && violationMatcher2.group(3) != null) violationMatcher2.group(3) else null

            if (playerName != null && violationText.isNotEmpty() && playerName.length in 3..16) {
                val buttonMessage = ChatButtonHandler.createViolationMessage(
                    playerName,
                    violationText.trim(),
                    detectionType
                )


                return originalMessage.copy().append(Text.literal(" ")).append(buttonMessage)
            }
        }


        return originalMessage
    }
}


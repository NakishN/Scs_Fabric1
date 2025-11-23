package com.scs

import com.scs.client.obfuscation.AntiDeobfuscator
import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Главный класс мода ScS Enhanced для Fabric
 *
 * ScS Enhanced - Advanced Anti-Cheat Monitor + Shaurma Clicker + DupeIP Integration
 *
 * @author nakish_
 * @version 2.0.0-fabric
 */
open class Scs : ClientModInitializer {

    companion object {
        const val MOD_ID = "scs"
        const val MOD_NAME = "ScS Enhanced"
        const val VERSION = "2.0.0-fabric"

        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    }

    override fun onInitializeClient() {
        try {
            AntiDeobfuscator.checkIntegrity()



        } catch (e: Exception) {
            throw RuntimeException("ScS mod initialization failed", e)
        }
    }
}
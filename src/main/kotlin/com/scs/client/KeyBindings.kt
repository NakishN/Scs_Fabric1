package com.scs.client

import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object KeyBindings {
    lateinit var toggleHudKey: KeyBinding
    lateinit var showHistoryKey: KeyBinding
    lateinit var clearEntriesKey: KeyBinding
    lateinit var shaurmaTapKey: KeyBinding
    lateinit var shaurmaMenuKey: KeyBinding
    lateinit var hudConfigKey: KeyBinding

    fun initialize() {
        toggleHudKey = KeyBinding(
            "key.scs.toggle_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "category.scs"
        )
        showHistoryKey = KeyBinding(
            "key.scs.show_history",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "category.scs"
        )
        clearEntriesKey = KeyBinding(
            "key.scs.clear_entries",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F10,
            "category.scs"
        )
        shaurmaTapKey = KeyBinding(
            "key.scs.shaurma_tap",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "category.scs"
        )
        shaurmaMenuKey = KeyBinding(
            "key.scs.shaurma_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "category.scs"
        )
        hudConfigKey = KeyBinding(
            "key.scs.hud_config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.scs"
        )
    }
}
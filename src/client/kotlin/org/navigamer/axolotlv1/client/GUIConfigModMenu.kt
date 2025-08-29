package org.navigamer.axolotlv1.client

import net.minecraft.client.gui.screen.Screen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

/** Mod Menu entrypoint */
class GUIConfigModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent: Screen? ->
            GUIConfig.createConfigScreen(parent)
        }
    }
}

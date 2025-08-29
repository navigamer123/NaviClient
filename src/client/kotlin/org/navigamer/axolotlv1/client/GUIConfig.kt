package org.navigamer.axolotlv1.client

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.LivingEntity
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import java.io.File
import java.io.FileReader
import java.io.FileWriter

@Environment(EnvType.CLIENT)
object GUIConfig : ModMenuApi {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File(MinecraftClient.getInstance().runDirectory, "config/navi_config.json")
    var mobEnabled: MutableMap<String, Boolean> = mutableMapOf()

    // Config values
    var enabledMobs: MutableList<String> = mutableListOf("minecraft:axolotl")
    var mobColors: MutableMap<String, List<Float>> = mutableMapOf(
        "minecraft:axolotl" to listOf(0f, 1f, 0f) // default green
    )

    /** Loads config from JSON file */
    fun loadConfig() {
        if (configFile.exists()) {
            FileReader(configFile).use { reader ->
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = gson.fromJson(reader, type)
                (data["enabledMobs"] as? List<*>)?.let {
                    enabledMobs = it.filterIsInstance<String>().toMutableList()
                }
                (data["mobColors"] as? Map<*, *>)?.let {
                    mobColors.clear()
                    it.forEach { (key, value) ->
                        val floats = (value as? List<*>)?.mapNotNull { it.toString().toFloatOrNull() } ?: listOf(0f, 1f, 0f)
                        mobColors[key.toString()] = floats
                    }
                }
            }
        } else {
            saveConfig() // create default
        }
    }

    /** Saves config to JSON file */
    fun saveConfig() {
        val data = mapOf(
            "enabledMobs" to enabledMobs,
            "mobColors" to mobColors
        )
        configFile.parentFile.mkdirs()
        FileWriter(configFile).use { writer ->
            gson.toJson(data, writer)
        }
    }

    /** Creates the Cloth Config screen */
    fun createConfigScreen(parent: Screen?): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.of("Navi Config"))
            .setSavingRunnable { saveConfig() }

        val entryBuilder = builder.entryBuilder()
        val category: ConfigCategory = builder.getOrCreateCategory(Text.of("General"))

        // Multi-select mob list
        val allMobs = Registries.ENTITY_TYPE.stream()
            .filter { LivingEntity::class.java.isAssignableFrom(it.javaClass) }
            .map { Registries.ENTITY_TYPE.getId(it).toString() }
            .toList()

        category.addEntry(
            entryBuilder.startStrList(
                Text.of("Mobs to render beacon on"),
                enabledMobs
            )
                .setDefaultValue(mutableListOf("minecraft:axolotl"))
                .setSaveConsumer { selectedMobs: MutableList<String> ->
                    enabledMobs = selectedMobs
                    // ensure each mob has a color
                    selectedMobs.forEach { mob ->
                        if (!mobColors.containsKey(mob)) mobColors[mob] = listOf(0f, 1f, 0f)
                    }
                }
                .setTooltip(Text.of("Available mobs: ${allMobs.joinToString()}"))
                .build()
        )
        // Convert float RGB (0f-1f) to int hex
        fun rgbToInt(r: Float, g: Float, b: Float): Int {
            val rr = (r * 255).toInt() and 0xFF
            val gg = (g * 255).toInt() and 0xFF
            val bb = (b * 255).toInt() and 0xFF
            return (rr shl 16) or (gg shl 8) or bb
        }

        // Convert int hex to float RGB
        fun intToRgb(color: Int): List<Float> {
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            return listOf(r, g, b)
        }

        // Per-mob color pickers
        enabledMobs.forEach { mob ->
            // Default render value: true
            val isEnabled = GUIConfig.mobEnabled[mob] ?: true

            // Boolean toggle entry
            category.addEntry(
                entryBuilder.startBooleanToggle(Text.of("Render $mob"), isEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer { newValue: Boolean ->
                        GUIConfig.mobEnabled[mob] = newValue
                    }
                    .build()
            )

            // Color picker entry (only if enabled)
            val currentIntColor = ((GUIConfig.mobColors[mob]?.get(0)?.times(255)?.toInt() ?: 0) shl 16) or
                    ((GUIConfig.mobColors[mob]?.get(1)?.times(255)?.toInt() ?: 255) shl 8) or
                    (GUIConfig.mobColors[mob]?.get(2)?.times(255)?.toInt() ?: 0)

            category.addEntry(
                entryBuilder.startColorField(Text.of("Color for $mob"), currentIntColor)
                    .setDefaultValue(currentIntColor)
                    .setSaveConsumer { newColor: Int ->
                        val r = ((newColor shr 16) and 0xFF) / 255f
                        val g = ((newColor shr 8) and 0xFF) / 255f
                        val b = (newColor and 0xFF) / 255f
                        GUIConfig.mobColors[mob] = listOf(r, g, b)
                    }
                    .build()
            )
        }

        return builder.build()
    }

    /** Registers /navi command */
    fun registerCommand(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommandManager.literal("navi")
                .executes { _: CommandContext<FabricClientCommandSource> ->
                    MinecraftClient.getInstance().setScreen(createConfigScreen(null))
                    1
                }
        )
    }

    /** Mod Menu integration */
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent: Screen -> createConfigScreen(parent) }
    }
}

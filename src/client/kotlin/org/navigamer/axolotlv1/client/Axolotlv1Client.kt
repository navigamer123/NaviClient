package org.navigamer.axolotlv1.client


import net.fabricmc.api.ClientModInitializer

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents

import net.minecraft.client.MinecraftClient

import net.minecraft.client.render.LightmapTextureManager

import net.minecraft.client.render.RenderLayer

import net.minecraft.client.util.math.MatrixStack

import net.minecraft.entity.decoration.ArmorStandEntity

import net.minecraft.entity.passive.AxolotlEntity

import net.minecraft.util.Identifier

import net.minecraft.util.math.Vec3d

import org.joml.Vector3f

import org.lwjgl.opengl.GL11
import java.util.UUID

import net.minecraft.registry.Registries
import net.minecraft.entity.EntityType

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.entity.LivingEntity
import net.minecraft.text.Text
import org.navigamer.axolotlv1.client.GUIConfig
import kotlin.let

class Axolotlv1Client : ClientModInitializer {

    private var tickCounter: Long = 0

    val lastColorMap = mutableMapOf<UUID, String?>()
    val currentColorMap = mutableMapOf<UUID, String?>()
    val lastColorMapAxi = mutableMapOf<UUID, String?>()
    var mobColors: MutableMap<String, List<Float>> = mutableMapOf()

    override fun onInitializeClient() {
        GUIConfig.loadConfig()
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            GUIConfig.registerCommand(dispatcher)
        }

        WorldRenderEvents.LAST.register { context ->

            val client = MinecraftClient.getInstance()
            val world = client.world
            val camera = client.gameRenderer.camera

            if (world != null) {
                val cameraPos = camera.pos
                val armorStands = world.entities.filterIsInstance<ArmorStandEntity>()
                val player = client.player

                // Get the list of mobs selected in your GUI config
                val selectedMobs = GUIConfig.enabledMobs.toSet()

                for (entity in world.entities) {
                    val entityId = Registries.ENTITY_TYPE.getId(entity.type).toString()
                    if (!selectedMobs.contains(entityId)) continue

                    val livingEntity = entity as? LivingEntity ?: continue
                    val entityPos = livingEntity.pos

                    // Find nearest armor stand to this entity
                    val nearestArmorStand = armorStands.minByOrNull { it.pos.distanceTo(entityPos) }

                    // Default beam color
                    val color = GUIConfig.mobColors[entityId] ?: listOf(0f, 1f, 1f)

                    var r = color[0]
                    var g = color[1]
                    var b = color[2]

                    val effectiveArmorStand =
                        if (nearestArmorStand != null && nearestArmorStand.pos.distanceTo(entityPos) <= 2.0) {
                            nearestArmorStand
                        } else {
                            player?.pos?.distanceTo(entityPos)?.let {
                                if (it <= 7) {
                                    if (lastColorMap[entity.uuid] == "yellow") {
                                        r = 0.0f; g = 1.0f; b = 0.0f
                                    }
                                }
                            }
                            null
                        }

                    if (effectiveArmorStand != null) {
                        val sibling = effectiveArmorStand.name.getSiblings().getOrNull(0)
                        val style = sibling?.getStyle()?.getColor()?.name
                        val currentColor = style
                        val previousColor: String? = lastColorMap[entity.uuid]

                        if (previousColor == "red" && currentColor == "yellow") {
                            println("${effectiveArmorStand.name.string} went from RED → YELLOW")
                        } else if (previousColor == "yellow" && currentColor != "yellow") {
                            println("${effectiveArmorStand.name.string} went from YELLOW → NULL")
                        }

                        if (previousColor == "yellow" && currentColor != "yellow") {
                            r = 0.0f; g = 1.0f; b = 0.0f
                        } else if (currentColor == "red") {
                            r = 1.0f; g = 0.0f; b = 0.0f
                        } else if (currentColor == "yellow") {
                            r = 1.0f; g = 1.0f; b = 0.0f
                        } else {
                            r = color[0]
                            g = color[1]
                            b = color[2]
                        }

                        lastColorMap[entity.uuid] = currentColor
                    }

                    // Render the beam
                    val matrixStack = context.matrixStack() ?: return@register
                    matrixStack.push()
                    matrixStack.translate(
                        entityPos.x - cameraPos.x,
                        entityPos.y - cameraPos.y,
                        entityPos.z - cameraPos.z
                    )
                    if (GUIConfig.mobEnabled[entityId] == true) {
                        renderBeam(matrixStack, livingEntity, cameraPos, r, g, b)
                    }
                    matrixStack.pop()
                }
            }
        }
    }



    private fun renderBeam(

        matrices: MatrixStack, entity: LivingEntity, camera: Vec3d?,

        r: Float, g: Float, b: Float

    ) {

// Calculate the beam's starting position, slightly above the axolotl's head.

        val pos = entity.getPos().subtract(camera).add(0.0, 0.5, 0.0)

        val texture = Identifier.of("minecraft:textures/entity/beacon_beam.png")


// Define the color for the beam (bright cyan).

        val r = r

        val g = g

        val b = b

        val alpha = 1f


// Get the vertex consumer for rendering the beacon beam texture.

        val consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()

// Use a custom render layer to make the beam visible through walls.

        val vertexConsumer = consumers.getBuffer(RenderLayer.getBeaconBeam(texture, true))


// Push a new matrix state for rendering transformations.

        matrices.push()

        matrices.translate(pos.x, pos.y, pos.z)


// Get the current transformation matrix.

        val modelMatrix = matrices.peek().getPositionMatrix()



        GL11.glDisable(GL11.GL_DEPTH_TEST)


// Define the normal vector and lightmap value required by the vertex format.

// The normal points straight up.

        val normal = Vector3f(0.0f, 1.0f, 0.0f)

// The lightmap value ensures the beam is fully lit, unaffected by world

// lighting.

        val lightmap =

            (LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE or LightmapTextureManager.MAX_SKY_LIGHT_COORDINATE)


        data class Vec3(val x: Float, val y: Float, val z: Float)


// Now you can use this class in your code.

// No import is needed since you are defining it in the same file or project.


        val width = 0.7f

        val beamHeight = 150.0f


//// Draw the square prism (4 quads, 16 vertices).


// Define the four corners of the base square

        val p1 = Vec3(width, 0.0f, -width)

        val p2 = Vec3(width, 0.0f, width)

        val p3 = Vec3(-width, 0.0f, width)

        val p4 = Vec3(-width, 0.0f, -width)


// Define the four corners of the top square

        val p1_top = Vec3(width, beamHeight, -width)

        val p2_top = Vec3(width, beamHeight, width)

        val p3_top = Vec3(-width, beamHeight, width)

        val p4_top = Vec3(-width, beamHeight, -width)


// Side 1: (p1 -> p2 -> p2_top -> p1_top)

// Vertices in counter-clockwise order for proper face culling and normals

        vertexConsumer.vertex(modelMatrix, p1.x, p1.y, p1.z).color(r, g, b, alpha).texture(0f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p1_top.x, p1_top.y, p1_top.z).color(r, g, b, alpha).texture(0f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p2_top.x, p2_top.y, p2_top.z).color(r, g, b, alpha).texture(1f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p2.x, p2.y, p2.z).color(r, g, b, alpha).texture(1f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)


// Side 2: (p2 -> p3 -> p3_top -> p2_top)

        vertexConsumer.vertex(modelMatrix, p2.x, p2.y, p2.z).color(r, g, b, alpha).texture(0f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p2_top.x, p2_top.y, p2_top.z).color(r, g, b, alpha).texture(0f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p3_top.x, p3_top.y, p3_top.z).color(r, g, b, alpha).texture(1f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p3.x, p3.y, p3.z).color(r, g, b, alpha).texture(1f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)


// Side 3: (p3 -> p4 -> p4_top -> p3_top)

        vertexConsumer.vertex(modelMatrix, p3.x, p3.y, p3.z).color(r, g, b, alpha).texture(0f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p3_top.x, p3_top.y, p3_top.z).color(r, g, b, alpha).texture(0f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p4_top.x, p4_top.y, p4_top.z).color(r, g, b, alpha).texture(1f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p4.x, p4.y, p4.z).color(r, g, b, alpha).texture(1f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)


// Side 4: (p4 -> p1 -> p1_top -> p4_top)

        vertexConsumer.vertex(modelMatrix, p4.x, p4.y, p4.z).color(r, g, b, alpha).texture(0f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p4_top.x, p4_top.y, p4_top.z).color(r, g, b, alpha).texture(0f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p1_top.x, p1_top.y, p1_top.z).color(r, g, b, alpha).texture(1f, 1f)

            .light(lightmap).normal(normal.x, normal.y, normal.z)

        vertexConsumer.vertex(modelMatrix, p1.x, p1.y, p1.z).color(r, g, b, alpha).texture(1f, 0f).light(lightmap)

            .normal(normal.x, normal.y, normal.z)


// Flushes the vertex consumer to render the beam.

        consumers.draw()



        GL11.glEnable(GL11.GL_DEPTH_TEST)


// Restore the previous matrix state.

        matrices.pop()

    }

}
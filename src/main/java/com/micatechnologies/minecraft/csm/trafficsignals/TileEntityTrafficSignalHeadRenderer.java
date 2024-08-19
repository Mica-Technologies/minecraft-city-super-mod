package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.ReadableColor;


public class TileEntityTrafficSignalHeadRenderer extends
    TileEntitySpecialRenderer<TileEntityTrafficSignalHead> {

  private static final ResourceLocation BULB_TEXTURE =
      new ResourceLocation("modid:textures/blocks/trafficsignal_bulbs.png");

  @Override
  public void render(TileEntityTrafficSignalHead te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    // Gather block state information
    EnumFacing facing = te.getWorld().getBlockState(te.getPos())
        .getValue(AbstractBlockControllableSignalHead.FACING);
    int bulbColor = te.getWorld().getBlockState(te.getPos())
        .getValue(AbstractBlockControllableSignalHead.COLOR);

    // Gather tile entity information
    TrafficSignalBodyColor bodyColor = te.getBodyPaintColor(); // Sides, top, bottom
    TrafficSignalBodyColor doorColor = te.getDoorPaintColor(); // Front
    TrafficSignalBodyColor visorColor = te.getVisorPaintColor(); // Not yet implemented
    TrafficSignalBodyTilt bodyTilt = te.getBodyTilt(); // Body tilt angle
    TrafficSignalVisorType visorType = te.getVisorType(); // Visor type
    DirectionSixteen bodyDirection =
        AbstractBlockControllableSignalHead.getTiltedFacing(bodyTilt, facing);

    // Push OpenGL transformation matrix.
    GL11.glPushMatrix();

    // Translate to the center of the block.
    GL11.glTranslated(x, y, z);

    // Rotate based on the facing direction.
    float rotationAngle = bodyDirection.getRotation();
    GL11.glRotatef(rotationAngle, 0, 1, 0);

    // Render the signal body.
    GL11.glScaled(0.0625, 0.0625, 0.0625); // Scale down to 1/16th size.
    GlStateManager.disableCull(); // Disable culling for visibility.
    GlStateManager.disableLighting(); // Disable lighting for color rendering.

    renderSignalSection(te, bulbColor, bodyColor, doorColor, visorColor, visorType, 1);
    renderSignalSection(te, bulbColor, bodyColor, doorColor, visorColor, visorType, 0);
    renderSignalSection(te, bulbColor, bodyColor, doorColor, visorColor, visorType, -1);

    GlStateManager.enableLighting(); // Enable lighting.
    GlStateManager.enableCull(); // Enable culling.

    GL11.glPopMatrix(); // Pop transformation matrix.
  }

  private void renderSignalSection(TileEntityTrafficSignalHead te, int bulbColor,
      TrafficSignalBodyColor bodyColor,
      TrafficSignalBodyColor doorColor, TrafficSignalBodyColor visorColor,
      TrafficSignalVisorType visorType, int index) {
    renderSignalBody(te, bodyColor, index);
    renderFrontDoor(te, doorColor, index);
    renderSignalBulb(te, bulbColor, index);
    renderSignalVisor(te, visorColor, visorType, index);
  }

  /**
   * Renders the traffic signal body. Coordinates are based on the "golden coordinates" provided.
   */
  private void renderSignalBody(TileEntityTrafficSignalHead te, TrafficSignalBodyColor bodyColor,
      int index) {
    float x = 2, y = 0, z = 11;
    float width = 12, height = 12, depth = 5;

    y += (index * 12); // Move up based on index

    RenderHelper.drawCuboidColored(x, y, z, width, height, depth, bodyColor.getRed(),
        bodyColor.getGreen(), bodyColor.getBlue());  // Render cube
  }

  /**
   * Renders the front door of the traffic signal. This sits on the front face of the signal body.
   */
  private void renderFrontDoor(TileEntityTrafficSignalHead te, TrafficSignalBodyColor doorColor,
      int index) {
    float x = 2, y = 0, z = 10.5f;
    float width = 12, height = 12, depth = 0.5f;

    y += (index * 12); // Move up based on index

    RenderHelper.drawCuboidColored(x, y, z, width, height, depth, doorColor.getRed(),
        doorColor.getGreen(),
        doorColor.getBlue()); // Render cube
  }

  /**
   * Renders the traffic signal bulb. Bulbs are positioned at specific heights on the front face.
   */
  private void renderSignalBulb(TileEntityTrafficSignalHead te, int bulbColor, int index) {
    // To be implemented
  }

  private static final Map<TrafficSignalVisorType, IBakedModel> VISOR_MODEL_CACHE = new HashMap<>();

  /**
   * Renders the visor of the traffic signal. For simplicity, a rectangle is used.
   */
  private void renderSignalVisor(TileEntityTrafficSignalHead te, TrafficSignalBodyColor visorColor,
      TrafficSignalVisorType visorType, int index) {
    // Load/cache model if not already loaded
    IBakedModel visorModel;
    if (!VISOR_MODEL_CACHE.containsKey(visorType)) {
      // Load model
      IModel modelOrMissing = ModelLoaderRegistry.getModelOrMissing(visorType.getModelLocation());
      visorModel = modelOrMissing.bake(modelOrMissing.getDefaultState(), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());
      VISOR_MODEL_CACHE.put(visorType, visorModel);
    } else {
      visorModel = VISOR_MODEL_CACHE.get(visorType);
    }

    // Render model
     RenderHelper.drawModelColored(visorModel, visorColor.getRed(), visorColor.getGreen(),
        visorColor.getBlue());
  }
}

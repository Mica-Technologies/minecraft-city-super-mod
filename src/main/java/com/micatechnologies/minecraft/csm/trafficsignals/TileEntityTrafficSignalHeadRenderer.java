package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;


public class TileEntityTrafficSignalHeadRenderer extends
    TileEntitySpecialRenderer<TileEntityTrafficSignalHead> {

  private static final ResourceLocation BULB_TEXTURE =
      new ResourceLocation("modid:textures/blocks/trafficsignal_bulbs.png");

  private static final List<Box> TUNNEL_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f})
  );


  private static final List<Box> NONE_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 10.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 10.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 10.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 10.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 10.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 10.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 10.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 10.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 10.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 10.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 10.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 10.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 10.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 10.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 10.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 10.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 10.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 10.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 10.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 10.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 10.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 10.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 10.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 10.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 10.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 10.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 10.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 10.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 10.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 10.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 10.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 10.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 10.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 10.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 10.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 10.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 10.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 10.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 10.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 10.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 10.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 10.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 10.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 10.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 10.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f})
  );



  private static final List<Box> VERTICAL_LOUVERED_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{3.15f, 4.10f, 2.00f}, new float[]{3.20f, 8.10f, 11.00f}),
      new Box(new float[]{12.80f, 4.10f, 2.00f}, new float[]{12.85f, 8.10f, 11.00f}),
      new Box(new float[]{11.80f, 2.50f, 2.00f}, new float[]{11.85f, 9.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.90f, 2.00f}, new float[]{10.85f, 10.30f, 11.00f}),
      new Box(new float[]{9.80f, 1.30f, 2.00f}, new float[]{9.85f, 10.90f, 11.00f}),
      new Box(new float[]{8.70f, 0.90f, 2.00f}, new float[]{8.75f, 11.30f, 11.00f}),
      new Box(new float[]{7.25f, 0.90f, 2.00f}, new float[]{7.30f, 11.30f, 11.00f}),
      new Box(new float[]{6.15f, 1.30f, 2.00f}, new float[]{6.20f, 10.90f, 11.00f}),
      new Box(new float[]{5.15f, 1.90f, 2.00f}, new float[]{5.20f, 10.30f, 11.00f}),
      new Box(new float[]{4.15f, 2.50f, 2.00f}, new float[]{4.20f, 9.70f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f})
  );

  private static final List<Box> HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{6.00f, 10.80f, 2.00f}, new float[]{10.00f, 10.85f, 11.00f}),
      new Box(new float[]{4.30f, 9.80f, 2.00f}, new float[]{11.70f, 9.85f, 11.00f}),
      new Box(new float[]{3.50f, 8.80f, 2.00f}, new float[]{12.50f, 8.85f, 11.00f}),
      new Box(new float[]{3.10f, 7.80f, 2.00f}, new float[]{12.90f, 7.85f, 11.00f}),
      new Box(new float[]{2.80f, 6.80f, 2.00f}, new float[]{13.20f, 6.85f, 11.00f}),
      new Box(new float[]{2.80f, 5.80f, 2.00f}, new float[]{13.20f, 5.85f, 11.00f}),
      new Box(new float[]{2.90f, 4.80f, 2.00f}, new float[]{13.10f, 4.85f, 11.00f}),
      new Box(new float[]{3.30f, 3.80f, 2.00f}, new float[]{12.70f, 3.85f, 11.00f}),
      new Box(new float[]{4.00f, 2.80f, 2.00f}, new float[]{12.00f, 2.85f, 11.00f}),
      new Box(new float[]{5.20f, 1.80f, 2.00f}, new float[]{10.80f, 1.85f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f})
  );

  private static final List<Box> BOTH_LOUVERED_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{6.00f, 10.80f, 2.00f}, new float[]{10.00f, 10.85f, 11.00f}),
      new Box(new float[]{4.30f, 9.80f, 2.00f}, new float[]{11.70f, 9.85f, 11.00f}),
      new Box(new float[]{3.50f, 8.80f, 2.00f}, new float[]{12.50f, 8.85f, 11.00f}),
      new Box(new float[]{3.10f, 7.80f, 2.00f}, new float[]{12.90f, 7.85f, 11.00f}),
      new Box(new float[]{2.80f, 6.80f, 2.00f}, new float[]{13.20f, 6.85f, 11.00f}),
      new Box(new float[]{2.80f, 5.80f, 2.00f}, new float[]{13.20f, 5.85f, 11.00f}),
      new Box(new float[]{2.90f, 4.80f, 2.00f}, new float[]{13.10f, 4.85f, 11.00f}),
      new Box(new float[]{3.30f, 3.80f, 2.00f}, new float[]{12.70f, 3.85f, 11.00f}),
      new Box(new float[]{4.00f, 2.80f, 2.00f}, new float[]{12.00f, 2.85f, 11.00f}),
      new Box(new float[]{5.20f, 1.80f, 2.00f}, new float[]{10.80f, 1.85f, 11.00f}),
      new Box(new float[]{3.15f, 4.10f, 2.00f}, new float[]{3.20f, 8.10f, 11.00f}),
      new Box(new float[]{4.15f, 2.50f, 2.00f}, new float[]{4.20f, 9.70f, 11.00f}),
      new Box(new float[]{5.15f, 1.90f, 2.00f}, new float[]{5.20f, 10.30f, 11.00f}),
      new Box(new float[]{6.15f, 1.30f, 2.00f}, new float[]{6.20f, 10.90f, 11.00f}),
      new Box(new float[]{7.25f, 0.90f, 2.00f}, new float[]{7.30f, 11.30f, 11.00f}),
      new Box(new float[]{8.70f, 0.90f, 2.00f}, new float[]{8.75f, 11.30f, 11.00f}),
      new Box(new float[]{9.80f, 1.30f, 2.00f}, new float[]{9.85f, 10.90f, 11.00f}),
      new Box(new float[]{10.80f, 1.90f, 2.00f}, new float[]{10.85f, 10.30f, 11.00f}),
      new Box(new float[]{11.80f, 2.50f, 2.00f}, new float[]{11.85f, 9.70f, 11.00f}),
      new Box(new float[]{12.80f, 4.10f, 2.00f}, new float[]{12.85f, 8.10f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 10.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 10.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 10.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 10.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 10.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 10.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f})
  );


  private static final List<Box> CIRCLE_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 2.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 2.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 2.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 2.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 2.00f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 2.00f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 2.00f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 2.00f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 2.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 2.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 2.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 2.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 2.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 2.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 2.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 2.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 2.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 2.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 2.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 2.00f}, new float[]{10.80f, 1.70f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 2.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 2.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 2.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 2.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 2.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 2.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 2.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 2.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 2.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f})
  );




  private static final List<Box> CAP_VISOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.90f, 8.10f, 2.00f}, new float[]{3.30f, 8.50f, 11.00f}),
      new Box(new float[]{2.90f, 3.70f, 7.00f}, new float[]{3.30f, 4.10f, 11.00f}),
      new Box(new float[]{12.70f, 8.10f, 2.00f}, new float[]{13.10f, 8.50f, 11.00f}),
      new Box(new float[]{12.70f, 3.70f, 7.00f}, new float[]{13.10f, 4.10f, 11.00f}),
      new Box(new float[]{3.10f, 8.50f, 2.00f}, new float[]{3.50f, 8.90f, 11.00f}),
      new Box(new float[]{3.10f, 3.30f, 7.00f}, new float[]{3.50f, 3.70f, 11.00f}),
      new Box(new float[]{12.50f, 8.50f, 2.00f}, new float[]{12.90f, 8.90f, 11.00f}),
      new Box(new float[]{12.50f, 3.30f, 7.00f}, new float[]{12.90f, 3.70f, 11.00f}),
      new Box(new float[]{3.30f, 8.90f, 2.00f}, new float[]{3.70f, 9.30f, 11.00f}),
      new Box(new float[]{3.30f, 2.90f, 7.50f}, new float[]{3.70f, 3.30f, 11.00f}),
      new Box(new float[]{12.30f, 8.90f, 2.00f}, new float[]{12.70f, 9.30f, 11.00f}),
      new Box(new float[]{12.30f, 2.90f, 7.50f}, new float[]{12.70f, 3.30f, 11.00f}),
      new Box(new float[]{3.60f, 9.30f, 2.00f}, new float[]{4.00f, 9.70f, 11.00f}),
      new Box(new float[]{3.60f, 2.50f, 7.50f}, new float[]{4.00f, 2.90f, 11.00f}),
      new Box(new float[]{12.00f, 9.30f, 2.00f}, new float[]{12.40f, 9.70f, 11.00f}),
      new Box(new float[]{12.00f, 2.50f, 7.50f}, new float[]{12.40f, 2.90f, 11.00f}),
      new Box(new float[]{3.90f, 9.70f, 2.00f}, new float[]{4.30f, 10.10f, 11.00f}),
      new Box(new float[]{3.90f, 2.10f, 8.00f}, new float[]{4.30f, 2.50f, 11.00f}),
      new Box(new float[]{11.70f, 9.70f, 2.00f}, new float[]{12.10f, 10.10f, 11.00f}),
      new Box(new float[]{11.70f, 2.10f, 8.00f}, new float[]{12.10f, 2.50f, 11.00f}),
      new Box(new float[]{4.20f, 9.90f, 2.00f}, new float[]{4.60f, 10.30f, 11.00f}),
      new Box(new float[]{4.20f, 1.90f, 8.00f}, new float[]{4.60f, 2.30f, 11.00f}),
      new Box(new float[]{11.40f, 9.90f, 2.00f}, new float[]{11.80f, 10.30f, 11.00f}),
      new Box(new float[]{11.40f, 1.90f, 8.00f}, new float[]{11.80f, 2.30f, 11.00f}),
      new Box(new float[]{2.70f, 7.70f, 2.00f}, new float[]{3.10f, 8.10f, 11.00f}),
      new Box(new float[]{2.70f, 4.10f, 5.00f}, new float[]{3.10f, 4.50f, 11.00f}),
      new Box(new float[]{12.90f, 7.70f, 2.00f}, new float[]{13.30f, 8.10f, 11.00f}),
      new Box(new float[]{12.90f, 4.10f, 5.00f}, new float[]{13.30f, 4.50f, 11.00f}),
      new Box(new float[]{6.00f, 10.90f, 2.00f}, new float[]{6.40f, 11.30f, 11.00f}),
      new Box(new float[]{9.60f, 10.90f, 2.00f}, new float[]{10.00f, 11.30f, 11.00f}),
      new Box(new float[]{5.60f, 10.70f, 2.00f}, new float[]{6.00f, 11.10f, 11.00f}),
      new Box(new float[]{10.00f, 10.70f, 2.00f}, new float[]{10.40f, 11.10f, 11.00f}),
      new Box(new float[]{5.20f, 10.50f, 2.00f}, new float[]{5.60f, 10.90f, 11.00f}),
      new Box(new float[]{10.40f, 10.50f, 2.00f}, new float[]{10.80f, 10.90f, 11.00f}),
      new Box(new float[]{4.80f, 10.30f, 2.00f}, new float[]{5.20f, 10.70f, 11.00f}),
      new Box(new float[]{4.80f, 1.50f, 8.00f}, new float[]{5.20f, 1.90f, 11.00f}),
      new Box(new float[]{10.80f, 10.30f, 2.00f}, new float[]{11.20f, 10.70f, 11.00f}),
      new Box(new float[]{10.80f, 1.50f, 8.00f}, new float[]{11.20f, 1.90f, 11.00f}),
      new Box(new float[]{4.50f, 10.10f, 2.00f}, new float[]{4.90f, 10.50f, 11.00f}),
      new Box(new float[]{4.50f, 1.70f, 8.00f}, new float[]{4.90f, 2.10f, 11.00f}),
      new Box(new float[]{11.10f, 10.10f, 2.00f}, new float[]{11.50f, 10.50f, 11.00f}),
      new Box(new float[]{11.10f, 1.70f, 8.00f}, new float[]{11.50f, 2.10f, 11.00f}),
      new Box(new float[]{2.50f, 7.30f, 2.00f}, new float[]{2.90f, 7.70f, 11.00f}),
      new Box(new float[]{2.50f, 4.50f, 5.00f}, new float[]{2.90f, 4.90f, 11.00f}),
      new Box(new float[]{13.10f, 7.30f, 2.00f}, new float[]{13.50f, 7.70f, 11.00f}),
      new Box(new float[]{13.10f, 4.50f, 5.00f}, new float[]{13.50f, 4.90f, 11.00f}),
      new Box(new float[]{6.40f, 11.10f, 2.00f}, new float[]{6.80f, 11.50f, 11.00f}),
      new Box(new float[]{9.20f, 11.10f, 2.00f}, new float[]{9.60f, 11.50f, 11.00f}),
      new Box(new float[]{2.40f, 6.10f, 2.00f}, new float[]{2.80f, 7.30f, 11.00f}),
      new Box(new float[]{13.20f, 4.90f, 3.00f}, new float[]{13.60f, 6.10f, 11.00f}),
      new Box(new float[]{2.40f, 4.90f, 3.00f}, new float[]{2.80f, 6.10f, 11.00f}),
      new Box(new float[]{13.20f, 6.10f, 2.00f}, new float[]{13.60f, 7.30f, 11.00f}),
      new Box(new float[]{6.80f, 11.30f, 2.00f}, new float[]{9.20f, 11.70f, 11.00f}),
      new Box(new float[]{5.20f, 1.30f, 10.00f}, new float[]{5.60f, 1.70f, 11.00f}),
      new Box(new float[]{5.60f, 1.10f, 10.00f}, new float[]{6.00f, 1.50f, 11.00f}),
      new Box(new float[]{6.00f, 0.90f, 10.00f}, new float[]{6.40f, 1.30f, 11.00f}),
      new Box(new float[]{6.40f, 0.70f, 10.00f}, new float[]{6.80f, 1.10f, 11.00f}),
      new Box(new float[]{6.80f, 0.50f, 10.00f}, new float[]{9.20f, 0.90f, 11.00f}),
      new Box(new float[]{9.60f, 0.90f, 10.00f}, new float[]{10.00f, 1.30f, 11.00f}),
      new Box(new float[]{9.20f, 0.70f, 10.00f}, new float[]{9.60f, 1.10f, 11.00f}),
      new Box(new float[]{10.00f, 1.10f, 10.00f}, new float[]{10.40f, 1.50f, 11.00f}),
      new Box(new float[]{10.40f, 1.30f, 10.00f}, new float[]{10.80f, 1.70f, 11.00f})
  );

  private static final List<Box> SIGNAL_BODY_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{4.00f, 0.00f, 15.80f}, new float[]{12.00f, 12.00f, 16.00f}),
      new Box(new float[]{3.80f, 0.00f, 15.60f}, new float[]{12.20f, 12.00f, 15.80f}),
      new Box(new float[]{3.60f, 0.00f, 15.40f}, new float[]{12.40f, 12.00f, 15.60f}),
      new Box(new float[]{3.40f, 0.00f, 15.20f}, new float[]{12.60f, 12.00f, 15.40f}),
      new Box(new float[]{3.20f, 0.00f, 15.00f}, new float[]{12.80f, 12.00f, 15.20f}),
      new Box(new float[]{3.00f, 0.00f, 14.80f}, new float[]{13.00f, 12.00f, 15.00f}),
      new Box(new float[]{2.80f, 0.00f, 14.60f}, new float[]{13.20f, 12.00f, 14.80f}),
      new Box(new float[]{2.60f, 0.00f, 14.40f}, new float[]{13.40f, 12.00f, 14.60f}),
      new Box(new float[]{2.40f, 0.00f, 14.20f}, new float[]{13.60f, 12.00f, 14.40f}),
      new Box(new float[]{2.20f, 0.00f, 14.00f}, new float[]{13.80f, 12.00f, 14.20f}),
      new Box(new float[]{2.00f, 0.00f, 11.00f}, new float[]{14.00f, 12.00f, 14.00f}),
      new Box(new float[]{1.80f, 1.20f, 10.80f}, new float[]{2.40f, 1.60f, 11.50f}),
      new Box(new float[]{1.80f, 10.20f, 10.80f}, new float[]{2.40f, 10.60f, 11.50f})
  );

  private static final List<Box> SIGNAL_DOOR_VERTEX_DATA = Arrays.asList(
      new Box(new float[]{2.00f, 0.00f, 10.75f}, new float[]{14.00f, 12.00f, 11.00f})
  );

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

    // 1. Translate to the block position in world space
    GL11.glTranslated(x, y, z);

    // 2. Scale down to 1/16th size (model coordinates to block coordinates)
    GL11.glScaled(0.0625, 0.0625, 0.0625);

    // 3. Translate to the block center (8, 8, 8)
    GL11.glTranslated(8, 8, 8);

    // 4. Rotate based on the facing direction (around the block center)
    float rotationAngle = bodyDirection.getRotation();
    GL11.glRotatef(rotationAngle, 0, 1, 0);

    // 5. Translate back from block center
    GL11.glTranslated(-8, -8, -8);

    // --- Compensation for tilt: shift slightly left/right for visual alignment ---
    // 1 model unit = 1/16 block, so shift by ±2 for tilt, ±4 for angle
    int tiltOffset = 0;
    if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.RIGHT_ANGLE) {
      tiltOffset = -4;
    } else if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.RIGHT_TILT) {
      tiltOffset = -2;
    } else if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.LEFT_TILT) {
      tiltOffset = 2;
    } else if (bodyTilt == com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt.LEFT_ANGLE) {
      tiltOffset = 4;
    }

    // Reverse the offset for SOUTH facing
    if (facing == net.minecraft.util.EnumFacing.SOUTH) {
      tiltOffset = -tiltOffset;
    }

    if (tiltOffset != 0) {
      GL11.glTranslated(tiltOffset, 0, 0);
    }

    // Now, rendering the signal body at (2, 0, 11) will always keep it at the correct offset
    // relative to the block, regardless of facing.

    GlStateManager.disableCull(); // Disable culling for visibility.
    GlStateManager.disableLighting(); // Disable lighting for color rendering.

    renderSignalSection(te, bulbColor, bodyColor, doorColor, visorColor, visorType, 1);
    renderSignalSection(te, bulbColor, bodyColor, doorColor, visorColor, visorType, 0);
    renderSignalSection(te, bulbColor, bodyColor, doorColor, visorColor, visorType, -1);

    renderSignalBulb(te, bulbColor, 1);
    renderSignalBulb(te, bulbColor, 0);
    renderSignalBulb(te, bulbColor, -1);

    GlStateManager.enableLighting(); // Enable lighting.
    GlStateManager.enableCull(); // Enable culling.

    GL11.glPopMatrix(); // Pop transformation matrix.
  }


  private void renderSignalSection(TileEntityTrafficSignalHead te, int bulbColor,
      TrafficSignalBodyColor bodyColor,
      TrafficSignalBodyColor doorColor, TrafficSignalBodyColor visorColor,
      TrafficSignalVisorType visorType, int index) {
    // Disable textures and set baseline color
    GlStateManager.disableTexture2D();
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

    renderSignalBody(te, bodyColor, index);
    renderFrontDoor(te, doorColor, index);
    renderSignalVisor(te, visorColor, visorType, index);

    // Restore textures and reset color
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    GlStateManager.enableTexture2D();
  }

  /**
   * Renders the traffic signal body. Coordinates are based on the "golden coordinates" provided.
   */
  private void renderSignalBody(TileEntityTrafficSignalHead te, TrafficSignalBodyColor bodyColor,
      int index) {
    // Render the body using the vertex data
    float yOffset = index * 12.0f;
    RenderHelper.drawBoxes(SIGNAL_BODY_VERTEX_DATA, bodyColor.getRed(), bodyColor.getGreen(),
        bodyColor.getBlue(), 1.0f,0.0f,yOffset,0.0f);
  }

  /**
   * Renders the front door of the traffic signal. This sits on the front face of the signal body.
   */
  private void renderFrontDoor(TileEntityTrafficSignalHead te, TrafficSignalBodyColor doorColor,
      int index) {
    // float x = 2, y = 0, z = 10.5f;
    // float width = 12, height = 12, depth = 0.5f;
    //
    // y += (index * 12); // Move up based on index
    //
    // RenderHelper.drawCuboidColoredbySize(x, y, z, width, height, depth, doorColor.getRed(),
    //     doorColor.getGreen(),
    //     doorColor.getBlue(),1.0f); // Render cube

    float yOffset = index * 12.0f;
    RenderHelper.drawBoxes(SIGNAL_DOOR_VERTEX_DATA, doorColor.getRed(), doorColor.getGreen(),
        doorColor.getBlue(), 1.0f,0.0f,yOffset,0.0f);
  }

  /**
   * Renders the traffic signal bulb. Bulbs are positioned at specific heights on the front face.
   */
  private void renderSignalBulb(TileEntityTrafficSignalHead te, int bulbColor, int index) {
    // Map bulbColor to texture name (just the base name, no extension or path)
    String[] bulbTextures = {
      "trafficsignals/lights/iled_red",      // 0
      "trafficsignals/lights/iled_red_off",   // 1
      "trafficsignals/lights/iled_yellow",   // 2
      "trafficsignals/lights/iled_yellow_off",// 3
      "trafficsignals/lights/iled_green",    // 4
      "trafficsignals/lights/iled_green_off"  // 5
      // Add more if needed
    };
    if (bulbColor < 0 || bulbColor >= bulbTextures.length) return;

    String tex = bulbTextures[bulbColor];
    ResourceLocation texLoc = new ResourceLocation("csm", "textures/blocks/" + tex + ".png");

    // Always bind the correct texture before rendering
    Minecraft.getMinecraft().getTextureManager().bindTexture(texLoc);

    // Reset color to white before rendering the bulb
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

    // Bulb quad parameters: cover the entire 12x12 front face of the section
    float x = 2f;
    float y = (index * 12f);
    float z = 10.4f; // just in front of the door
    float size = 12f;

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

    buffer.pos(x,      y,      z).tex(0, 0).endVertex();
    buffer.pos(x,      y+size, z).tex(0, 1).endVertex();
    buffer.pos(x+size, y+size, z).tex(1, 1).endVertex();
    buffer.pos(x+size, y,      z).tex(1, 0).endVertex();

    tessellator.draw();
  }

  /**
   * Renders the visor of the traffic signal using the baked model and applies color tinting.
   */
  private void renderSignalVisor(TileEntityTrafficSignalHead te, TrafficSignalBodyColor visorColor,
      TrafficSignalVisorType visorType, int index) {

    float colorAlpha = 1.0f; // Full opacity
    float xOffset = 0.0f; // No X offset
    float yOffset = (index * 12.0f);
    float zOffset = 0.0f; // No Z offset

    if (visorType == TrafficSignalVisorType.CIRCLE) {
      RenderHelper.drawBoxes(CIRCLE_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.TUNNEL) {
      RenderHelper.drawBoxes(TUNNEL_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.CUTAWAY) {
      RenderHelper.drawBoxes(CAP_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.VERTICAL_LOUVERED) {
      RenderHelper.drawBoxes(VERTICAL_LOUVERED_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    }else if (visorType == TrafficSignalVisorType.HORIZONTAL_LOUVERED) {
      RenderHelper.drawBoxes(HORIZONTAL_LOUVERED_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    }else if (visorType == TrafficSignalVisorType.BOTH_LOUVERED) {
      RenderHelper.drawBoxes(BOTH_LOUVERED_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    } else if (visorType == TrafficSignalVisorType.NONE) {
      RenderHelper.drawBoxes(NONE_VISOR_VERTEX_DATA, visorColor.getRed(), visorColor.getGreen(), visorColor.getBlue(),colorAlpha, xOffset,yOffset,zOffset);
    }
  }


}

package com.micatechnologies.minecraft.csm.signage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The outline of the board a screen is about to build, drawn in the world while the screen is
 * open, so the player sees where a 15 by 8 board will go before pressing Done.
 */
@SideOnly(Side.CLIENT)
public final class AdBoardPreview {

  private static AxisAlignedBB box;

  private AdBoardPreview() {
  }

  /** Registers the drawing handler; once, from the client proxy. */
  static void register() {
    MinecraftForge.EVENT_BUS.register(new AdBoardPreview.Handler());
  }

  /** Shows the outline of a board of this size round {@code controller}. */
  static void show(BlockPos controller, EnumFacing facing, int controllerColumn, int width,
      int height) {
    BlockPos a = AdBoards.cell(controller, facing, controllerColumn, 0, 0);
    BlockPos b = AdBoards.cell(controller, facing, controllerColumn, width - 1, height - 1);
    box = new AxisAlignedBB(a).union(new AxisAlignedBB(b)).grow(0.01);
  }

  static void hide() {
    box = null;
  }

  /** The event handler; an instance, since the bus wants one for non-static methods. */
  public static final class Handler {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
      AxisAlignedBB outline = box;
      Entity view = Minecraft.getMinecraft().getRenderViewEntity();
      if (outline == null || view == null) {
        return;
      }
      float t = event.getPartialTicks();
      double cx = view.lastTickPosX + (view.posX - view.lastTickPosX) * t;
      double cy = view.lastTickPosY + (view.posY - view.lastTickPosY) * t;
      double cz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * t;
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
          GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
          GlStateManager.DestFactor.ZERO);
      GlStateManager.glLineWidth(3.0F);
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      RenderGlobal.drawSelectionBoundingBox(outline.offset(-cx, -cy, -cz), 1.0F, 0.85F, 0.1F,
          0.9F);
      GlStateManager.enableDepth();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
    }
  }
}

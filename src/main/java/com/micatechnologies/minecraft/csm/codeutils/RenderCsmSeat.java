package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws an {@link EntityCsmSeat}: nothing. The seat is the block's own model; only its rider shows.
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class RenderCsmSeat extends Render<EntityCsmSeat> {

  /**
   * Constructs a {@link RenderCsmSeat}.
   *
   * @param renderManager the render manager
   *
   * @since 1.0
   */
  public RenderCsmSeat(RenderManager renderManager) {
    super(renderManager);
  }

  @Override
  public void doRender(@Nonnull EntityCsmSeat entity, double x, double y, double z,
      float entityYaw, float partialTicks) {
  }

  @Override
  public boolean shouldRender(@Nonnull EntityCsmSeat livingEntity,
      @Nonnull net.minecraft.client.renderer.culling.ICamera camera, double camX, double camY,
      double camZ) {
    return false;
  }

  @Nullable
  @Override
  protected ResourceLocation getEntityTexture(@Nonnull EntityCsmSeat entity) {
    return null;
  }
}

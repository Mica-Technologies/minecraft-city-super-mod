package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmConfig;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.ICsmRoadSurfaceAware;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * TESR that renders a double-flash strobe on traffic beacon blocks. The pattern is two rapid
 * blinks per one-second cycle, matching common MUTCD Type B flashing beacon cadence.
 */
@SideOnly(Side.CLIENT)
public class TileEntityTrafficBeaconRenderer
    extends TileEntitySpecialRenderer<AbstractTileEntity> {

  private static final long CYCLE_MS = 1000L;
  private static final long FLASH1_START = 0L;
  private static final long FLASH1_END = 75L;
  private static final long FADE1_END = 125L;
  private static final long FLASH2_START = 250L;
  private static final long FLASH2_END = 325L;
  private static final long FADE2_END = 375L;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  @Override
  public void render(AbstractTileEntity te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (!CsmConfig.isStrobeEffectEnabled()) return;
    if (te.getWorld() == null) return;

    IBlockState state = te.getWorld().getBlockState(te.getPos());
    Block block = state.getBlock();
    if (!(block instanceof ITrafficBeaconBlock)) return;

    ITrafficBeaconBlock beacon = (ITrafficBeaconBlock) block;

    // POWERED is respected when the block has it and ignored when it does not: the redstone
    // beacons gate on it, while a work zone warning light runs on its own and has no such
    // property. Requiring the property outright would silently render nothing for the latter.
    if (state.getPropertyKeys().contains(AbstractPoweredBlockRotatableNSEWUD.POWERED)
        && !state.getValue(AbstractPoweredBlockRotatableNSEWUD.POWERED)) {
      return;
    }

    long offset = (te instanceof TileEntityTrafficBeacon)
        ? ((TileEntityTrafficBeacon) te).getStrobeOffset() : 0L;
    long gameMillis = CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + offset;
    long cycle = beacon.getBeaconCycleMillis();
    float intensity = cycle > 0L
        ? computePulseIntensity(gameMillis, cycle, beacon.getBeaconPulseMillis(),
            beacon.getBeaconFadeMillis())
        : computeIntensity(gameMillis);
    if (intensity <= 0f) return;

    // Facing comes from whichever rotation property the block actually carries. The full
    // six-way beacons use one, the horizontal-only work zone devices another, and a device with
    // no rotation at all is drawn unrotated rather than skipped.
    EnumFacing facing = EnumFacing.NORTH;
    if (state.getPropertyKeys().contains(AbstractPoweredBlockRotatableNSEWUD.FACING)) {
      facing = state.getValue(AbstractPoweredBlockRotatableNSEWUD.FACING);
    } else if (state.getPropertyKeys().contains(AbstractBlockRotatableNSEW.FACING)) {
      facing = state.getValue(AbstractBlockRotatableNSEW.FACING);
    }

    float[] from = beacon.getBeaconLensFrom();
    float[] to = beacon.getBeaconLensTo();
    float r = beacon.getBeaconColorR();
    float g = beacon.getBeaconColorG();
    float b = beacon.getBeaconColorB();

    float minX = from[0] / 16f - 0.5f;
    float minY = from[1] / 16f - 0.5f;
    float minZ = from[2] / 16f - 0.5f;
    float maxX = to[0] / 16f - 0.5f;
    float maxY = to[1] / 16f - 0.5f;
    float maxZ = to[2] / 16f - 0.5f;

    // A device that settles onto the road under it is DRAWN lower than its own cell, but this
    // renderer works in world space and knows nothing about that offset. Without adding it here
    // the lens flashes at the height the block was placed at while the device it belongs to sits
    // lower down.
    double settle = (block instanceof ICsmRoadSurfaceAware)
        ? ((ICsmRoadSurfaceAware) block).getRoadSurfaceOffset(te.getWorld(), te.getPos())
        : 0.0;

    GlStateManager.pushMatrix();
    GlStateManager.translate((float) x + 0.5f, (float) (y + settle) + 0.5f, (float) z + 0.5f);
    applyFacingRotation(facing);

    // Bind a 1x1 white pixel texture instead of disableTexture2D — shaders ignore
    // disableTexture2D and sample whatever was last bound. Fullbright lightmap is baked
    // per-vertex via the BLOCK vertex format.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);
    GlStateManager.disableLighting();

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();

    // Core glow: all 6 faces of the beacon lens, slightly offset outward
    float off = 0.005f;
    drawBox(buf, tessellator, minX - off, minY - off, minZ - off,
        maxX + off, maxY + off, maxZ + off, r, g, b, 1.0f * intensity);

    // Inner halo: 50% padding around the lens
    float padX = (maxX - minX) * 0.5f;
    float padY = (maxY - minY) * 0.5f;
    float padZ = (maxZ - minZ) * 0.5f;
    drawBox(buf, tessellator, minX - padX, minY - padY, minZ - padZ,
        maxX + padX, maxY + padY, maxZ + padZ, r, g, b, 0.35f * intensity);

    // Outer halo: larger, more transparent
    drawBox(buf, tessellator, minX - padX * 2f, minY - padY * 2f, minZ - padZ * 2f,
        maxX + padX * 2f, maxY + padY * 2f, maxZ + padZ * 2f, r, g, b, 0.12f * intensity);

    GlStateManager.depthMask(true);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    // Restore standard alpha blend before disabling blend so the next TESR that enables
    // blend without setting its own func doesn't inherit our additive (SRC_ALPHA, ONE) mode.
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /**
   * One short pulse per cycle, with a brief fade out.
   *
   * <p>The caller has already added the beacon's own random offset to {@code gameMillis}, which
   * is what puts a row of these out of step with one another the way a row of real warning
   * lights is. Nothing here needs to know that has happened.</p>
   *
   * @param gameMillis  the beacon's own clock, in milliseconds
   * @param cycleMillis the length of one full cycle
   * @param pulseMillis how long the beacon is at full brightness
   * @param fadeMillis  how long it takes to fade out afterwards
   *
   * @return the brightness, 0 to 1
   */
  private static float computePulseIntensity(long gameMillis, long cycleMillis, long pulseMillis,
      long fadeMillis) {
    long t = Math.floorMod(gameMillis, cycleMillis);
    if (t < pulseMillis) {
      return 1.0f;
    }
    if (fadeMillis > 0L && t < pulseMillis + fadeMillis) {
      return 1.0f - (float) (t - pulseMillis) / fadeMillis;
    }
    return 0f;
  }

  private static float computeIntensity(long gameMillis) {
    long t = gameMillis % CYCLE_MS;
    // Flash 1
    if (t >= FLASH1_START && t < FLASH1_END) return 1.0f;
    if (t >= FLASH1_END && t < FADE1_END)
      return 1.0f - (float) (t - FLASH1_END) / (FADE1_END - FLASH1_END);
    // Flash 2
    if (t >= FLASH2_START && t < FLASH2_END) return 1.0f;
    if (t >= FLASH2_END && t < FADE2_END)
      return 1.0f - (float) (t - FLASH2_END) / (FADE2_END - FLASH2_END);
    return 0f;
  }

  private static void drawBox(BufferBuilder buf, Tessellator tess,
      float x1, float y1, float z1, float x2, float y2, float z2,
      float r, float g, float b, float a) {
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    // -Z face
    emit(buf, x1, y1, z1, r, g, b, a);
    emit(buf, x2, y1, z1, r, g, b, a);
    emit(buf, x2, y2, z1, r, g, b, a);
    emit(buf, x1, y2, z1, r, g, b, a);
    // +Z face
    emit(buf, x2, y1, z2, r, g, b, a);
    emit(buf, x1, y1, z2, r, g, b, a);
    emit(buf, x1, y2, z2, r, g, b, a);
    emit(buf, x2, y2, z2, r, g, b, a);
    // -X face
    emit(buf, x1, y1, z2, r, g, b, a);
    emit(buf, x1, y1, z1, r, g, b, a);
    emit(buf, x1, y2, z1, r, g, b, a);
    emit(buf, x1, y2, z2, r, g, b, a);
    // +X face
    emit(buf, x2, y1, z1, r, g, b, a);
    emit(buf, x2, y1, z2, r, g, b, a);
    emit(buf, x2, y2, z2, r, g, b, a);
    emit(buf, x2, y2, z1, r, g, b, a);
    // -Y face
    emit(buf, x1, y1, z2, r, g, b, a);
    emit(buf, x2, y1, z2, r, g, b, a);
    emit(buf, x2, y1, z1, r, g, b, a);
    emit(buf, x1, y1, z1, r, g, b, a);
    // +Y face
    emit(buf, x1, y2, z1, r, g, b, a);
    emit(buf, x2, y2, z1, r, g, b, a);
    emit(buf, x2, y2, z2, r, g, b, a);
    emit(buf, x1, y2, z2, r, g, b, a);
    tess.draw();
  }

  private static void emit(BufferBuilder buf, float x, float y, float z,
      float r, float g, float b, float a) {
    buf.pos(x, y, z).color(r, g, b, a).tex(0.5f, 0.5f)
        .lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
  }

  private static void applyFacingRotation(EnumFacing facing) {
    switch (facing) {
      case NORTH:
        break;
      case SOUTH:
        GlStateManager.rotate(180f, 0f, 1f, 0f);
        break;
      case EAST:
        GlStateManager.rotate(-90f, 0f, 1f, 0f);
        break;
      case WEST:
        GlStateManager.rotate(90f, 0f, 1f, 0f);
        break;
      case UP:
        GlStateManager.rotate(90f, 1f, 0f, 0f);
        break;
      case DOWN:
        GlStateManager.rotate(-90f, 1f, 0f, 0f);
        break;
    }
  }
}

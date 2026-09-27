package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nonnull;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The tile entity of a tall building beacon. It holds nothing and never ticks: it exists so the
 * flash can be drawn by {@link TileEntityObstructionBeaconRenderer}, at full brightness and with
 * a glow, which a baked model cannot be without OptiFine.
 *
 * <p>An obstruction light is there to be seen from a long way off, so it draws out to twice the
 * mod's long-range distance, and its box takes in the glow around it.</p>
 *
 * @since 2026.9
 */
public class TileEntityObstructionBeacon extends AbstractTileEntity {

  /** How far away the flash is drawn, in blocks. */
  static final double RENDER_DISTANCE = LONG_RANGE_RENDER_DISTANCE * 2;

  /** How far the glow reaches past the block, in blocks. */
  static final double GLOW_REACH = 4.0;

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    return RENDER_DISTANCE * RENDER_DISTANCE;
  }

  @Override
  @Nonnull
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos).grow(GLOW_REACH);
  }

  /** No baked model reads this tile entity. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}

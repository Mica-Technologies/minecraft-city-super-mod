package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Minimal tile entity for fire alarm strobe blocks. Stores no data and does not tick.
 * Its sole purpose is to enable TESR attachment for the visual strobe flash effect
 * rendered by {@link TileEntityFireAlarmStrobeRenderer}.
 */
public class TileEntityFireAlarmStrobe extends AbstractTileEntity {

  @Override
  public void readNBT(NBTTagCompound compound) {
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    return compound;
  }

  /**
   * Returns a render bounding box covering everything the flash draws: the lens quad, and the
   * pools of light {@link StrobeSurfaceProjection} throws onto surfaces up to
   * {@link StrobeSurfaceProjection#MAX_DISTANCE} away. Overriding this switches vanilla from
   * {@code INFINITE_EXTENT_AABB} (which disables frustum culling) to real frustum culling —
   * strobes outside the view skip the TESR render entirely. The box has to reach as far as the
   * light does, though: sized to the block alone, a pool on the far wall would wink out as soon
   * as the device itself left the screen.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    double reach = StrobeSurfaceProjection.MAX_DISTANCE + 1.0;
    return new AxisAlignedBB(
        pos.getX() - reach, pos.getY() - reach, pos.getZ() - reach,
        pos.getX() + 1.0 + reach, pos.getY() + 1.0 + reach, pos.getZ() + 1.0 + reach);
  }
}

package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A door that is swinging: exists only on the door's upper half, only for the few ticks a swing
 * takes, and holds only which way it is going and the world tick it started. It never ticks -- a
 * scheduled block tick on the door ends the swing ({@link BlockBuildingDoor#updateTick}), and the
 * tile entity goes with the state that asked for it ({@link #shouldRefresh}).
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityDoorSwing extends AbstractTileEntity {

  private boolean opening;
  private long startTick;

  void begin(boolean opening, long now) {
    this.opening = opening;
    this.startTick = now;
    markDirtySync(world, pos, true);
  }

  public boolean isOpening() {
    return opening;
  }

  /**
   * How far the swing has got, 0 to 1; 1 before the client has heard when it started.
   *
   * @param partialTicks the partial tick
   *
   * @return the progress
   *
   * @since 1.0
   */
  public double progress(float partialTicks) {
    if (startTick == 0L) {
      return 1.0;
    }
    double t = (world.getTotalWorldTime() - startTick + partialTicks)
        / BlockBuildingDoor.SWING_TICKS;
    return Math.max(0.0, Math.min(1.0, t));
  }

  @Override
  public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState,
      @Nonnull IBlockState newState) {
    return oldState.getBlock() != newState.getBlock()
        || !newState.getValue(BlockBuildingDoor.SWING);
  }

  private static final String KEY_OPENING = "o";
  private static final String KEY_START = "t";

  @Override
  public void readNBT(NBTTagCompound compound) {
    opening = compound.getBoolean(KEY_OPENING);
    startTick = compound.getLong(KEY_START);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setBoolean(KEY_OPENING, opening);
    compound.setLong(KEY_START, startTick);
    return compound;
  }

  /** Both halves of the door, and the cell it swings through. */
  @Override
  @SideOnly(Side.CLIENT)
  @Nonnull
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos.down(), pos.up()).grow(1.0);
  }
}

package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A door that is swinging: exists only on a client, only on the door's upper half, and only for
 * the few ticks a swing takes, and holds only which way it is going and the world tick it started.
 * Its presence IS the door's {@code swing} state ({@link BlockBuildingDoor#getActualState}), which
 * the models draw nothing for while its renderer draws the leaf turning.
 *
 * <p>A block event starts it ({@link BlockBuildingDoor#eventReceived}), and it takes itself away
 * once the swing is over, as a moving piston's does -- so a client that misses the end of a swing
 * cannot be left with one, and nothing about it is saved or sent. The server makes none: it knows
 * a door is swinging by the upper half's pending tick.</p>
 *
 * <p>An older version kept one on the server as well, saved with the chunk, and marked the swing
 * in the bit that now says the door is reversed. One loaded from such a save is put right
 * ({@link #putRightLegacySwing}): the bit is cleared, so the door is not turned round, and the tile
 * entity goes.</p>
 *
 * @version 1.1
 * @since 2026.9
 */
public class TileEntityDoorSwing extends AbstractTileEntity implements ITickable {

  private boolean opening;
  private long startTick;

  void begin(boolean opening, long now) {
    this.opening = opening;
    this.startTick = now;
  }

  public boolean isOpening() {
    return opening;
  }

  /**
   * How far the swing has got, 0 to 1; 1 before it has been told when it started.
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

  /**
   * On a client, takes the tile entity away once the swing is over, and has the door's baked
   * models drawn again at once. On the server there is only ever one left by an older version.
   *
   * @since 1.1
   */
  @Override
  public void update() {
    if (world == null) {
      return;
    }
    if (!world.isRemote) {
      putRightLegacySwing(world, pos);
      return;
    }
    if (startTick == 0L || world.getTotalWorldTime() - startTick >= BlockBuildingDoor.SWING_TICKS) {
      world.removeTileEntity(pos);
      redraw(world, pos);
    }
  }

  /**
   * A client's door has started or stopped swinging: rebuild both halves' chunk section now rather
   * than on a worker thread, so the baked models and the renderer hand over in the same frame.
   *
   * @param world    the client's world
   * @param upperPos the door's upper half
   *
   * @since 1.1
   */
  static void redraw(World world, BlockPos upperPos) {
    IBlockState upper = world.getBlockState(upperPos);
    IBlockState lower = world.getBlockState(upperPos.down());
    world.notifyBlockUpdate(upperPos, upper, upper, 8);
    world.notifyBlockUpdate(upperPos.down(), lower, lower, 8);
  }

  /**
   * Puts right a door saved mid-swing by an older version, server side: that version kept a
   * swing's tile entity on the server, saved it with the chunk, and said "swinging" with the upper
   * half's bit 4, which now says the door is reversed. So a door with one of these on the server
   * came from such a save; its bit 4 is cleared, so it is not turned round, and the tile entity
   * goes. Called from the tile entity's first tick and from the upper half's end-of-swing tick
   * that the save brought back with it, whichever comes first.
   *
   * @param world    the server's world
   * @param upperPos the door's upper half
   *
   * @since 1.1
   */
  static void putRightLegacySwing(World world, BlockPos upperPos) {
    TileEntity te = world.getTileEntity(upperPos);
    if (!(te instanceof TileEntityDoorSwing) || world.isRemote) {
      return;
    }
    world.removeTileEntity(upperPos);
    IBlockState upper = world.getBlockState(upperPos);
    if (upper.getBlock() instanceof BlockBuildingDoor
        && upper.getValue(BlockBuildingDoor.HALF) == BlockBuildingDoor.Half.UPPER
        && upper.getValue(BlockBuildingDoor.REVERSED)) {
      world.setBlockState(upperPos, upper.withProperty(BlockBuildingDoor.REVERSED, false), 3);
    }
  }

  /** Only a change of block takes it away; the door's own state changing mid-swing does not. */
  @Override
  public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState,
      @Nonnull IBlockState newState) {
    return oldState.getBlock() != newState.getBlock();
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

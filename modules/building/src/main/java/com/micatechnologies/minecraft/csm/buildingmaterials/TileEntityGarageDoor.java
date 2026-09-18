package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A garage door in motion: exists only on the door's anchor block, only while the door moves.
 *
 * <p>It holds what the move needs -- the door's size, which way it is going, and the world tick it
 * started on -- and the renderer draws the travelling curtain or panels from those and the world
 * time, so the server and every client agree without sending positions. On the server it ends the
 * move when the time is up, setting every block of the door to its new state; that state asks for
 * no tile entity, and {@link #shouldRefresh} lets this one go with it. See
 * {@link BlockGarageDoor}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityGarageDoor extends AbstractTickableTileEntity {

  /** Blocks a second: a real door is about a fifth of that, too slow to stand and wait for. */
  static final double SPEED = 1.0;
  /**
   * The sectional door's track, in blocks: the radius of the bend from the vertical to the
   * horizontal, which starts at the top of the opening.
   */
  static final double BEND = 0.375;
  /**
   * The door's mid-plane, z in the north-facing model frame: just behind the wall, a pixel and a
   * half past the opening's inside face (z 0). SHARED with gen_garage_doors.PLANE_Z.
   */
  static final double PLANE = -1.5 / 16.0;

  private int width = 1;
  private int height = 1;
  private boolean opening = true;
  private long startTick;

  /**
   * Starts a move. Called on the server as the door goes into its moving state.
   *
   * @param width   the door's width in blocks
   * @param height  its height
   * @param opening whether it is opening
   * @param now     the world tick
   *
   * @since 1.0
   */
  void start(int width, int height, boolean opening, long now) {
    this.width = width;
    this.height = height;
    this.opening = opening;
    this.startTick = now;
    markDirtySync(world, pos, true);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isOpening() {
    return opening;
  }

  /**
   * How far the door has to travel, in blocks: a roll-up's curtain up out of the opening, a
   * sectional door's bottom edge up past the opening and round the bend.
   *
   * @return the travel
   *
   * @since 1.0
   */
  public double travel() {
    if (getBlockType() instanceof BlockGarageDoor
        && ((BlockGarageDoor) getBlockType()).kind() == BlockGarageDoor.Kind.SECTIONAL) {
      return height + Math.PI * BEND / 2;
    }
    return height;
  }

  /**
   * The move's duration in ticks.
   *
   * @return the duration
   *
   * @since 1.0
   */
  public long duration() {
    return Math.max(10L, Math.round(travel() / SPEED * 20.0));
  }

  /**
   * How far through the move the door is, 0 to 1.
   *
   * @param partialTicks the partial tick
   *
   * @return the progress
   *
   * @since 1.0
   */
  public double progress(float partialTicks) {
    if (startTick == 0L) {
      return 0.0; // not yet synced
    }
    double t = (world.getTotalWorldTime() - startTick) + partialTicks;
    return Math.max(0.0, Math.min(1.0, t / duration()));
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public long getTickRate() {
    return 1L;
  }

  @Override
  public void onTick() {
    if (world.getTotalWorldTime() - startTick >= duration()) {
      IBlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof BlockGarageDoor) {
        ((BlockGarageDoor) state.getBlock()).finish(world, pos,
            state.getValue(BlockGarageDoor.FACING), width, height, opening);
      }
    }
  }

  /**
   * Goes when the block stops moving (or is no longer a door).
   *
   * @since 1.0
   */
  @Override
  public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState,
      @Nonnull IBlockState newState) {
    return oldState.getBlock() != newState.getBlock()
        || newState.getValue(BlockGarageDoor.MOTION) != BlockGarageDoor.Motion.ANCHOR;
  }

  // NBT keys, short as every CSM tile entity's are.
  private static final String KEY_W = "w";
  private static final String KEY_H = "h";
  private static final String KEY_OPENING = "o";
  private static final String KEY_START = "t";

  @Override
  public void readNBT(NBTTagCompound compound) {
    width = Math.max(1, Math.min(16, compound.getInteger(KEY_W)));
    height = Math.max(1, Math.min(16, compound.getInteger(KEY_H)));
    opening = compound.getBoolean(KEY_OPENING);
    startTick = compound.getLong(KEY_START);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_W, width);
    compound.setInteger(KEY_H, height);
    compound.setBoolean(KEY_OPENING, opening);
    compound.setLong(KEY_START, startTick);
    return compound;
  }

  /**
   * The whole door and the ceiling a sectional door runs back under.
   *
   * @since 1.0
   */
  @Override
  @SideOnly(Side.CLIENT)
  @Nonnull
  public AxisAlignedBB getRenderBoundingBox() {
    IBlockState state = world.getBlockState(pos);
    EnumFacing f = state.getBlock() instanceof BlockGarageDoor
        ? state.getValue(BlockGarageDoor.FACING) : EnumFacing.NORTH;
    BlockPos far = pos.offset(f.rotateY(), width).up(height + 1);
    return new AxisAlignedBB(pos, far).union(new AxisAlignedBB(pos.offset(f, height + 1)))
        .grow(1.0);
  }
}

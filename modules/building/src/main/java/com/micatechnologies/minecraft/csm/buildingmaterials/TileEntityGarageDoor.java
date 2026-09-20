package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A garage door that is not at rest: exists only on the door's anchor block, only while the door
 * is moving or stopped part-way.
 *
 * <p>Where the door is is one number, its <em>position</em>, 0 closed to 1 open. The tile entity
 * holds the position at the last change, the direction since then (+1 opening, -1 closing, 0
 * stopped) and the world tick of that change, so the position at any moment is worked out from the
 * world time -- the server and every client agree without sending positions, and the renderer
 * draws the door there. A command (open, close, stop, or a toggle, which stops a moving door and
 * reverses a stopped one, as a one-button opener does) sets a new starting point. On the server,
 * when the position reaches the end it is heading for, every block of the door is set to rest open
 * or closed; that state asks for no tile entity, and {@link #shouldRefresh} lets this one go with
 * it. See {@link BlockGarageDoor}.</p>
 *
 * @version 1.2
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
  /** The position at {@link #startTick}. */
  private double from;
  /** +1 opening, -1 closing, 0 stopped. */
  private int dir;
  /** The last direction it moved in, which a toggle of a stopped door reverses. */
  private int lastDir = 1;
  private long startTick;

  /**
   * Starts the door moving from rest. Called on the server as the door leaves its resting state.
   *
   * @param width  the door's width in blocks
   * @param height its height
   * @param from   where it starts: 0 closed, 1 open
   * @param dir    +1 to open, -1 to close
   * @param now    the world tick
   *
   * @since 1.1
   */
  void begin(int width, int height, double from, int dir, long now) {
    this.width = width;
    this.height = height;
    this.from = from;
    this.dir = dir;
    this.lastDir = dir;
    this.startTick = now;
    markDirtySync(world, pos, true);
  }

  /**
   * Obeys a command given while the door is not at rest.
   *
   * @param command what was asked
   * @param now     the world tick
   *
   * @since 1.1
   */
  void command(BlockGarageDoor.Command command, long now) {
    double at = positionAt(now);
    int next;
    switch (command) {
      case OPEN:
        next = 1;
        break;
      case CLOSE:
        next = -1;
        break;
      case STOP:
        next = 0;
        break;
      default:
        next = dir != 0 ? 0 : -lastDir;
        break;
    }
    if (next == dir) {
      return;
    }
    from = at;
    dir = next;
    if (next != 0) {
      lastDir = next;
    }
    startTick = now;
    markDirtySync(world, pos, true);
    world.playSound(null, pos, next == 0 ? SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE
        : SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, 0.6F);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /**
   * Which way a move ended by breaking the anchor should go: the way it was going, or, stopped,
   * the nearer end.
   *
   * @return whether the door should end up open
   *
   * @since 1.1
   */
  boolean endsOpen() {
    if (dir != 0) {
      return dir > 0;
    }
    return from >= 0.5;
  }

  /**
   * How far the door has to travel, in blocks, from closed to open: a roll-up's curtain up out of
   * the opening, a sectional door's bottom edge up past the opening and round the bend.
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
   * The time a full move takes, in ticks.
   *
   * @return the duration
   *
   * @since 1.0
   */
  public long duration() {
    return Math.max(10L, Math.round(travel() / SPEED * 20.0));
  }

  private double positionAt(double tick) {
    return positionAt(from, dir, startTick, duration(), tick);
  }

  /**
   * The position a door has reached: where it was at {@code startTick}, plus how far it has gone
   * since at {@code dir}, held inside 0..1.
   *
   * <p>{@code tick} is a {@code double} and has to be built as one. A world's total time plus a
   * partial tick, written {@code long + float}, is a {@code float} in Java, and a float holds a
   * whole tick only up to 16,777,216 of them -- ten days of a world's life. Past that the sum
   * moves in steps: 256 ticks at a time on a server a few years old, so the door a client drew
   * sat still for thirteen seconds and then jumped, while the server, which has no partial tick
   * and so never left {@code long}, ran the real door correctly underneath it.</p>
   *
   * @param from      the position at {@code startTick}
   * @param dir       +1 opening, -1 closing, 0 stopped
   * @param startTick the world tick the move began on
   * @param duration  the ticks a full move takes
   * @param tick      the world tick to answer for, partial tick included
   *
   * @return the position, 0 closed to 1 open
   *
   * @since 1.2
   */
  static double positionAt(double from, int dir, long startTick, long duration, double tick) {
    double p = from + dir * (tick - startTick) / duration;
    return Math.max(0.0, Math.min(1.0, p));
  }

  /**
   * Where the door is, 0 closed to 1 open.
   *
   * @param partialTicks the partial tick
   *
   * @return the position
   *
   * @since 1.1
   */
  public double position(float partialTicks) {
    if (startTick == 0L) {
      return from; // not yet synced
    }
    return positionAt(clock(world.getTotalWorldTime(), partialTicks));
  }

  /**
   * The moment a frame is drawn for, in ticks: the world's total time and the partial tick, as a
   * {@code double}. Every renderer that places something by world time takes its clock from here
   * rather than writing the sum out, because the sum written out is the bug
   * {@link #positionAt(double, int, long, long, double)} describes.
   *
   * @param worldTime    the world's total time
   * @param partialTicks the partial tick
   *
   * @return the two together, exact for any world a {@code long} can count
   *
   * @since 1.2
   */
  static double clock(long worldTime, float partialTicks) {
    return (double) worldTime + partialTicks;
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return dir == 0;
  }

  @Override
  public long getTickRate() {
    return 1L;
  }

  @Override
  public void onTick() {
    double p = positionAt(world.getTotalWorldTime());
    if ((dir > 0 && p >= 1.0) || (dir < 0 && p <= 0.0)) {
      IBlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof BlockGarageDoor) {
        ((BlockGarageDoor) state.getBlock()).finish(world, pos,
            state.getValue(BlockGarageDoor.FACING), width, height, dir > 0);
      }
    }
  }

  /**
   * Goes when the door comes to rest (or the block is no longer a door).
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
  private static final String KEY_FROM = "p";
  private static final String KEY_DIR = "d";
  private static final String KEY_LAST_DIR = "l";
  private static final String KEY_START = "t";

  @Override
  public void readNBT(NBTTagCompound compound) {
    width = Math.max(1, Math.min(16, compound.getInteger(KEY_W)));
    height = Math.max(1, Math.min(16, compound.getInteger(KEY_H)));
    double f = compound.getDouble(KEY_FROM);
    from = Double.isFinite(f) ? Math.max(0.0, Math.min(1.0, f)) : 0.0;
    dir = Integer.signum(compound.getInteger(KEY_DIR));
    lastDir = compound.getInteger(KEY_LAST_DIR) < 0 ? -1 : 1;
    startTick = compound.getLong(KEY_START);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_W, width);
    compound.setInteger(KEY_H, height);
    compound.setDouble(KEY_FROM, from);
    compound.setInteger(KEY_DIR, dir);
    compound.setInteger(KEY_LAST_DIR, lastDir);
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

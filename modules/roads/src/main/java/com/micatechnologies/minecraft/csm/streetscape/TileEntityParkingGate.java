package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A parking gate's arm and its rules (#302).
 *
 * <p>The server decides, every five ticks, whether the arm should be up, and keeps the answer in
 * {@link #isOpen}, sent to the client in a tile entity packet when it changes: redstone always
 * lifts it; in {@link Mode#AUTO} anything at
 * the gate (a player, a mob, a vehicle) lifts it; a ticket issued or paid at a machine beside it
 * ({@link #grant}) lifts it in any mode. Once up it stays up while the lane is occupied and for
 * {@link #HOLD_TICKS} after it clears. The client walks the arm's angle toward {@code open} for
 * {@link TileEntityParkingGateRenderer}, as the railroad crossing gate does: visual state, never
 * saved, set straight to its target on the first tick after load.</p>
 *
 * <p>{@code open} was a block property until 2026-10. No model read it, and as a block change
 * every open and close rebuilt the client's chunk section, twice for each car; as tile entity data
 * it is one small packet, and {@link #getBakedModelKey} keeps that from rebuilding anything. A
 * world saved before has no {@code op} here and loads with the arm down, then the server decides
 * within five ticks, as it always did after a load (the hold times are not saved, so the old bit
 * was only ever kept by redstone).</p>
 *
 * <p>Saved: {@code md} the mode, {@code al} the arm's length in blocks (3 to 6), {@code op} whether
 * the arm is up (only when it is), so the client's first look at a chunk has it.</p>
 *
 * @since 2026.10
 */
public class TileEntityParkingGate extends AbstractTickableTileEntity {

  /** How the gate decides to lift. */
  public enum Mode {
    /** Lifts for anything at the gate. */
    AUTO("Automatic: lifts for anyone at the gate"),
    /** Lifts only for a ticket issued or paid at a machine beside it. */
    TICKET("Ticket: lifts for a ticket issued or paid beside it"),
    /** Lifts only when powered. */
    REDSTONE("Redstone only");

    public final String label;

    Mode(String label) {
      this.label = label;
    }
  }

  public static final float RAISED = 85F;
  public static final float LOWERED = 0F;
  private static final float RAISE_DEGREES_PER_TICK = RAISED / 30F;   // 1.5 s
  private static final float LOWER_DEGREES_PER_TICK = RAISED / 40F;   // 2 s
  public static final int MIN_LENGTH = 3;
  public static final int MAX_LENGTH = 6;
  /** How long the arm stays up after the lane clears, in ticks. */
  static final long HOLD_TICKS = 60L;
  /** How long a ticket keeps the arm up for a car that has not yet reached it, in ticks. */
  static final long GRANT_TICKS = 200L;

  private Mode mode = Mode.AUTO;
  private int armLength = 4;

  // server: until when the arm is held up (world time)
  private long holdUntil = Long.MIN_VALUE;
  private long grantUntil = Long.MIN_VALUE;

  // server: the answer; client: what the server last said
  private boolean open;

  // client: the arm's angle
  private float angle = LOWERED;
  private float prevAngle = LOWERED;
  private boolean settled;
  // client: what the renderer needs from the world, read once a tick rather than every frame
  private EnumFacing armDirection;
  private int armLight;

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
    markDirtySync(world, pos, true);
  }

  public int getArmLength() {
    return armLength;
  }

  public void setArmLength(int armLength) {
    this.armLength = Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, armLength));
    markDirtySync(world, pos, true);
  }

  /** Whether the server wants the arm up: on the client, what it last said. */
  public boolean isOpen() {
    return open;
  }

  /** Lifts the arm for a ticket issued or paid at a machine beside the gate. */
  public void grant() {
    if (world != null) {
      grantUntil = world.getTotalWorldTime() + GRANT_TICKS;
      holdUntil = grantUntil;
    }
  }

  @Override
  public boolean doClientTick() {
    return true;
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
    if (world == null) {
      return;
    }
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockParkingGate)) {
      return;
    }
    if (world.isRemote) {
      readView(state);
      animate(open);
      return;
    }
    long now = world.getTotalWorldTime();
    if (now % 5 != 0) {
      return;
    }
    boolean occupied = mode != Mode.REDSTONE && occupied(state);
    if ((mode == Mode.AUTO && occupied) || (occupied && now < grantUntil + HOLD_TICKS)) {
      holdUntil = Math.max(holdUntil, now + HOLD_TICKS);
    }
    boolean up = world.isBlockPowered(pos) || now < holdUntil;
    if (up != open) {
      open = up;
      markDirtySync(world, pos, true);
    }
  }

  /** Reads the arm's direction and the light it is drawn at, for the renderer. Client only. */
  private void readView(IBlockState state) {
    armDirection = BlockParkingGate.armDirection(state);
    armLight = world.getCombinedLight(pos.up(), 0);
  }

  /**
   * The way the arm reaches, as of this tick, or null before the first client tick (then the
   * renderer reads it itself, once).
   */
  @SideOnly(Side.CLIENT)
  public EnumFacing getArmDirection() {
    if (armDirection == null && world != null) {
      IBlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof BlockParkingGate) {
        readView(state);
      }
    }
    return armDirection;
  }

  /** The combined light above the cabinet, as of this tick: the arm is drawn at it. */
  @SideOnly(Side.CLIENT)
  public int getArmLight() {
    return armLight;
  }

  private void animate(boolean open) {
    float target = open ? RAISED : LOWERED;
    if (!settled) {
      angle = prevAngle = target;
      settled = true;
      return;
    }
    prevAngle = angle;
    if (angle < target) {
      angle = Math.min(target, angle + RAISE_DEGREES_PER_TICK);
    } else if (angle > target) {
      angle = Math.max(target, angle - LOWER_DEGREES_PER_TICK);
    }
  }

  /** The lane the arm closes: its cells, a block either side of the arm, two high. */
  AxisAlignedBB lane(IBlockState state) {
    EnumFacing along = BlockParkingGate.armDirection(state);
    EnumFacing across = along.rotateY();
    AxisAlignedBB cells = new AxisAlignedBB(pos.offset(along))
        .union(new AxisAlignedBB(pos.offset(along, armLength)));
    return cells.grow(Math.abs(across.getXOffset()), 0, Math.abs(across.getZOffset()))
        .expand(0, 1, 0);
  }

  private boolean occupied(IBlockState state) {
    List<Entity> found = world.getEntitiesWithinAABB(Entity.class, lane(state),
        e -> e instanceof EntityLivingBase || !(e instanceof EntityItem
            || e instanceof EntityXPOrb || e instanceof EntityArrow));
    return !found.isEmpty();
  }

  @SideOnly(Side.CLIENT)
  public float getRenderAngle(float partialTicks) {
    return prevAngle + (angle - prevAngle) * partialTicks;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(pos).grow(armLength + 1);
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(96.0 * 96.0);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    int m = compound.getByte("md");
    mode = m >= 0 && m < Mode.values().length ? Mode.values()[m] : Mode.AUTO;
    armLength = compound.hasKey("al")
        ? Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, compound.getByte("al"))) : 4;
    // absent in a world saved while open was a block property, and whenever the arm is down
    open = compound.getBoolean("op");
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (mode != Mode.AUTO) {
      compound.setByte("md", (byte) mode.ordinal());
    }
    compound.setByte("al", (byte) armLength);
    if (open) {
      compound.setBoolean("op", true);
    }
    return compound;
  }

  /** The arm's length and whether it is up are drawn by the renderer; no baked model reads either. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}

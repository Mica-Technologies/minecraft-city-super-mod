package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficEntitySelectors;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A radar speed feedback sign: the "YOUR SPEED 32" board that reads an approaching vehicle and
 * shows its speed back to it.
 *
 * <p>Speed comes from <em>differencing positions between samples</em>, not from an entity's
 * {@code motionX}/{@code motionZ}. Those fields are not maintained server-side for players, so
 * reading them would return zero for exactly the entity this block most needs to measure.</p>
 *
 * <p>The conversion to mph is the one the SUM speed HUD uses, deliberately: a block is a metre
 * and a tick is 1/20 s, so blocks per second is metres per second and mph follows from
 * {@value #MPS_TO_MPH}. A sign and that HUD reading different numbers for the same journey would
 * be the kind of discrepancy nobody ever tracks down.</p>
 *
 * <p>Read that way nothing in Minecraft moves at road speed — a sprinting player covers
 * {@value #SPRINT_BLOCKS_PER_SECOND} blocks per second, which is about 12.5 mph, so a realistically posted 25 mph zone is never exceeded. The multiplier setting is the
 * escape hatch: it defaults to 1x, true to the HUD, and scales up for anyone who would rather
 * their traffic behaved like traffic.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntityRadarSpeedSign extends AbstractTickableTileEntity {

  /** Ticks between position samples. Five a second is far finer than the display needs. */
  public static final int SAMPLE_INTERVAL_TICKS = 4;

  /**
   * How long a reading stays up after the zone empties. Without a hold the number would vanish
   * the instant a driver passed the sign, which is precisely when they look at it. Three
   * seconds is what the real boards give you, and two turned out to be genuinely too quick to
   * read while moving.
   */
  public static final int HOLD_TICKS = 60;

  /** A sprinting player: 4.317 blocks/s walking, times the 1.3 sprint multiplier. */
  public static final float SPRINT_BLOCKS_PER_SECOND = 5.612f;

  /**
   * Exact metres-per-second to miles-per-hour factor, a block being one metre. Copied from
   * {@code HudFormat} in the SUM mod so a sign and that mod's speed HUD agree to the digit.
   */
  public static final double MPS_TO_MPH = 2.2369362920544;

  /**
   * Above this a sample is a teleport rather than travel, and is discarded. It is close to 90
   * mph at 1x and well past anything reachable on foot, in a cart or under elytra, so nothing
   * legitimate is lost.
   */
  private static final double TELEPORT_BLOCKS_PER_SECOND = 40.0;

  /**
   * Multipliers applied on top of the true conversion. 1x is the SUM HUD's own number; the rest
   * exist because a world built to road proportions rather than player proportions wants a
   * sprint to read like a car, and there is no single right answer to which.
   */
  public static final double[] MULTIPLIERS = {1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0};
  public static final String[] MULTIPLIER_NAMES =
      {"1x (true)", "1.5x", "2x", "2.5x", "3x", "4x", "5x"};
  private static final int DEFAULT_MULTIPLIER_INDEX = 0;

  /** How far over the posted speed the board gives up on a number and says SLOW DOWN. */
  public static final int SLOW_DOWN_MARGIN = 15;

  public static final int MIN_SPEED = 5;
  public static final int MAX_SPEED = 75;
  public static final int SPEED_STEP = 5;

  /** Panel sizes, matching the school zone assembly so the two can share a pole. */
  public static final float[] SCALES = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
  public static final String[] SCALE_NAMES = {"75%", "100%", "125%", "150%", "200%"};

  /** Default scan zone: this far out from the face, this far to either side of the lane. */
  private static final int DEFAULT_RANGE = 12;
  private static final double DEFAULT_HALF_WIDTH = 1.5;
  private static final double DEFAULT_BELOW = 1.0;
  private static final double DEFAULT_ABOVE = 3.0;

  private static final String KEY_SPEED = "spd";
  private static final String KEY_MULTIPLIER = "mul";
  private static final String KEY_SCALE = "scl";
  private static final String KEY_HEADER = "hdr";
  private static final String KEY_FACE_COLOR = "face";
  private static final String KEY_ZONE_1 = "z1";
  private static final String KEY_ZONE_2 = "z2";
  private static final String KEY_READING = "rdg";

  private int postedSpeed = 25;
  private int multiplierIndex = DEFAULT_MULTIPLIER_INDEX;
  private int scaleIndex = 1;
  private boolean showHeader = false;
  private MutcdSignFaceColor faceColor = MutcdSignFaceColor.YELLOW;

  private BlockPos zoneCorner1 = null;
  private BlockPos zoneCorner2 = null;

  /** The number on the board right now; zero means the readout is dark. */
  private int reading = 0;

  // Server-side bookkeeping. Deliberately not saved: a reading is a live measurement, and a
  // sign that came back from a world reload still showing someone's speed would be lying.
  private transient int holdTicks = 0;
  private transient Map<UUID, Vec3d> lastPositions = new HashMap<>();
  private transient boolean lastPowered = false;

  public int getPostedSpeed() {
    return postedSpeed;
  }

  public int getMultiplierIndex() {
    return multiplierIndex;
  }

  public int getScaleIndex() {
    return scaleIndex;
  }

  public float getScale() {
    return SCALES[clamp(scaleIndex, 0, SCALES.length - 1)];
  }

  public boolean isShowHeader() {
    return showHeader;
  }

  public MutcdSignFaceColor getFaceColor() {
    return faceColor;
  }

  public boolean hasCustomZone() {
    return zoneCorner1 != null && zoneCorner2 != null;
  }

  /**
   * The number the board is showing.
   *
   * @return the speed in mph, or {@code 0} when the readout is dark
   */
  public int getReading() {
    return reading;
  }

  /** Whether the reading is over the posted speed, which is what makes the digits flash. */
  public boolean isOverLimit() {
    return reading > postedSpeed;
  }

  /**
   * Whether the reading is far enough over that the board stops rewarding it with a number.
   *
   * @return {@code true} when SLOW DOWN should be shown instead of the speed
   */
  public boolean isSlowDown() {
    return reading > postedSpeed + SLOW_DOWN_MARGIN;
  }

  public void setPostedSpeed(int speed) {
    this.postedSpeed = clamp(speed, MIN_SPEED, MAX_SPEED);
    sync();
  }

  public void setMultiplierIndex(int index) {
    this.multiplierIndex = clamp(index, 0, MULTIPLIERS.length - 1);
    sync();
  }

  public void setScaleIndex(int index) {
    this.scaleIndex = clamp(index, 0, SCALES.length - 1);
    sync();
  }

  public void setShowHeader(boolean showHeader) {
    this.showHeader = showHeader;
    sync();
  }

  public void setFaceColor(MutcdSignFaceColor faceColor) {
    this.faceColor = faceColor == null ? MutcdSignFaceColor.YELLOW : faceColor;
    sync();
  }

  /**
   * Sets the scan zone explicitly, as the sensor zone tool does.
   *
   * @param corner1 one corner of the zone
   * @param corner2 the opposite corner
   *
   * @return {@code true} if this replaced a zone that had already been set
   */
  public boolean setZoneCorners(BlockPos corner1, BlockPos corner2) {
    boolean replaced = hasCustomZone();
    this.zoneCorner1 = corner1;
    this.zoneCorner2 = corner2;
    sync();
    return replaced;
  }

  /** Drops a custom zone, putting the sign back on the default box in front of its face. */
  public void clearZoneCorners() {
    zoneCorner1 = null;
    zoneCorner2 = null;
    sync();
  }

  /**
   * The box the radar looks in.
   *
   * <p>Falls back to a lane-shaped box reaching {@value #DEFAULT_RANGE} blocks out from the
   * direction the sign faces, so a freshly placed sign works without anyone having to go and
   * find the zone tool first.</p>
   *
   * @return the scan zone in world coordinates, or {@code null} if there is no world or the
   *     block state has no facing yet
   */
  public AxisAlignedBB getScanZone() {
    if (world == null) {
      return null;
    }
    if (hasCustomZone()) {
      return new AxisAlignedBB(zoneCorner1, zoneCorner2);
    }

    EnumFacing facing;
    try {
      facing = world.getBlockState(pos).getValue(BlockHorizontal.FACING);
    } catch (IllegalArgumentException notThisBlockAnyMore) {
      return null;
    }
    return defaultScanZone(pos, facing);
  }

  /**
   * The default lane-shaped zone in front of a sign. Package-visible and static so the facing
   * arithmetic can be tested without a world — {@code EnumFacing.byHorizontalIndex(0)} is SOUTH,
   * and getting that backwards would point every zone the wrong way.
   *
   * @param pos    the sign's position
   * @param facing the direction the sign faces
   *
   * @return the zone
   */
  static AxisAlignedBB defaultScanZone(BlockPos pos, EnumFacing facing) {
    BlockPos far = pos.offset(facing, DEFAULT_RANGE);
    // Widen across the lane only. Padding the long axis too would push the zone out behind the
    // sign, where a vehicle has already gone past it.
    boolean alongX = facing.getAxis() == EnumFacing.Axis.X;
    double padX = alongX ? 0.0 : DEFAULT_HALF_WIDTH;
    double padZ = alongX ? DEFAULT_HALF_WIDTH : 0.0;
    return new AxisAlignedBB(
        Math.min(pos.getX(), far.getX()) - padX,
        pos.getY() - DEFAULT_BELOW,
        Math.min(pos.getZ(), far.getZ()) - padZ,
        Math.max(pos.getX(), far.getX()) + 1.0 + padX,
        pos.getY() + DEFAULT_ABOVE,
        Math.max(pos.getZ(), far.getZ()) + 1.0 + padZ);
  }

  /**
   * Converts travel speed into the number on the board.
   *
   * <p>At the default 1x this is exactly what the SUM speed HUD would print for the same
   * movement, rounded to a whole number the way a real board shows it.</p>
   *
   * @param blocksPerSecond measured horizontal speed
   * @param multiplierIndex which multiplier setting is in force
   *
   * @return the speed in mph, rounded
   */
  public static int toDisplayedSpeed(double blocksPerSecond, int multiplierIndex) {
    double multiplier = MULTIPLIERS[clamp(multiplierIndex, 0, MULTIPLIERS.length - 1)];
    return (int) Math.round(blocksPerSecond * MPS_TO_MPH * multiplier);
  }

  @Override
  public long getTickRate() {
    return SAMPLE_INTERVAL_TICKS;
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
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }
    AxisAlignedBB zone = getScanZone();
    if (zone == null) {
      return;
    }

    double elapsedSeconds = SAMPLE_INTERVAL_TICKS / 20.0;
    double fastest = 0.0;
    Map<UUID, Vec3d> seen = new HashMap<>();

    for (Entity entity :
        world.getEntitiesWithinAABB(Entity.class, zone, TrafficEntitySelectors.VEHICLE)) {
      Vec3d now = new Vec3d(entity.posX, entity.posY, entity.posZ);
      seen.put(entity.getUniqueID(), now);
      Vec3d before = lastPositions.get(entity.getUniqueID());
      if (before == null) {
        continue;
      }
      // Horizontal only, or a fall past the sign registers as speeding. This is the same
      // quantity the SUM HUD reads from posX - prevPosX, just averaged over the sample window
      // rather than taken from a single tick, which steadies the number on the board.
      double dx = now.x - before.x;
      double dz = now.z - before.z;
      double blocksPerSecond = Math.sqrt(dx * dx + dz * dz) / elapsedSeconds;
      if (blocksPerSecond > TELEPORT_BLOCKS_PER_SECOND) {
        continue;
      }
      if (blocksPerSecond > fastest) {
        fastest = blocksPerSecond;
      }
    }
    // Replacing the map prunes it: anything not seen this sample is gone, so a despawn or an
    // unloaded chunk cannot leak entries.
    lastPositions = seen;

    int previous = reading;
    int measured = toDisplayedSpeed(fastest, multiplierIndex);
    if (measured > 0) {
      reading = measured;
      holdTicks = HOLD_TICKS;
    } else if (holdTicks > 0) {
      holdTicks -= SAMPLE_INTERVAL_TICKS;
      if (holdTicks <= 0) {
        reading = 0;
      }
    } else {
      reading = 0;
    }

    if (reading != previous) {
      markDirtySync(world, pos, true);
    }

    boolean powered = isOverLimit();
    if (powered != lastPowered) {
      lastPowered = powered;
      world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
    }
  }

  private void sync() {
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  private static int clamp(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    // hasKey throughout, because zero is a legal value for several of these and an absent tag
    // has to mean "unconfigured" rather than "set to zero".
    postedSpeed = compound.hasKey(KEY_SPEED)
        ? clamp(compound.getInteger(KEY_SPEED), MIN_SPEED, MAX_SPEED)
        : 25;
    multiplierIndex = compound.hasKey(KEY_MULTIPLIER)
        ? clamp(compound.getInteger(KEY_MULTIPLIER), 0, MULTIPLIERS.length - 1)
        : DEFAULT_MULTIPLIER_INDEX;
    scaleIndex = clamp(compound.getInteger(KEY_SCALE), 0, SCALES.length - 1);
    showHeader = compound.getBoolean(KEY_HEADER);
    faceColor = MutcdSignFaceColor.fromNBT(compound.getInteger(KEY_FACE_COLOR));
    zoneCorner1 = compound.hasKey(KEY_ZONE_1)
        ? BlockPos.fromLong(compound.getLong(KEY_ZONE_1)) : null;
    zoneCorner2 = compound.hasKey(KEY_ZONE_2)
        ? BlockPos.fromLong(compound.getLong(KEY_ZONE_2)) : null;
    // Read so a sync packet carries the live number to the client. A world load has no key and
    // so correctly comes back dark.
    reading = compound.getInteger(KEY_READING);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_SPEED, postedSpeed);
    compound.setInteger(KEY_MULTIPLIER, multiplierIndex);
    compound.setInteger(KEY_SCALE, scaleIndex);
    compound.setBoolean(KEY_HEADER, showHeader);
    compound.setInteger(KEY_FACE_COLOR, faceColor.ordinal());
    if (zoneCorner1 != null && zoneCorner2 != null) {
      compound.setLong(KEY_ZONE_1, zoneCorner1.toLong());
      compound.setLong(KEY_ZONE_2, zoneCorner2.toLong());
    }
    compound.setInteger(KEY_READING, reading);
    return compound;
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return LONG_RANGE_RENDER_DISTANCE_SQUARED;
  }

  /**
   * The board is drawn taller than its own cell once the header panel is fitted and the scale
   * turned up, so the render box has to cover what is actually drawn or the whole thing vanishes
   * when the block's own cell leaves the frustum.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 2, pos.getY() - 1, pos.getZ() - 2,
        pos.getX() + 3, pos.getY() + 7, pos.getZ() + 3);
  }
}

package com.micatechnologies.minecraft.csm.parks.zipline;

import com.micatechnologies.minecraft.csm.CsmConfig;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A zipline trolley with its rider: rolls down the cable from a launch anchor to its landing
 * anchor and sets the rider down there.
 *
 * <p>The server decides where it is: it steps the ride ({@link ZiplineRide}) each tick and syncs
 * the distance along the line and the speed. A client moves it along the same line itself from
 * those, easing towards the server's distance rather than taking the tracker's position packets, so
 * a rider does not rubber-band. Both work out the line from the two anchors' positions, sent once
 * when the trolley spawns.</p>
 *
 * <p>It lives only for a ride: never saved, gone when its rider is, and the rider cannot sneak off
 * until the brake zone (sneaking over a drop would be a fall). Riders on one line keep
 * {@link ZiplineRide#SPACING} apart.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class EntityZiplineTrolley extends Entity implements IEntityAdditionalSpawnData {

  /** CSM entity network id; Core's seat is 0. */
  public static final int NETWORK_ID = 1;
  /** Registry path: {@code csm:zipline_trolley}. */
  public static final String NAME = "zipline_trolley";

  private static final DataParameter<Float> DISTANCE =
      EntityDataManager.createKey(EntityZiplineTrolley.class, DataSerializers.FLOAT);
  private static final DataParameter<Float> SPEED =
      EntityDataManager.createKey(EntityZiplineTrolley.class, DataSerializers.FLOAT);

  /** The server's trolleys riding now, for spacing; a trolley leaves it as it dies. */
  private static final Set<EntityZiplineTrolley> RIDING =
      Collections.newSetFromMap(new WeakHashMap<>());

  @Nullable
  private ZiplineLine line;
  /** Distance along the line, here: the server's own, or a client's eased copy. */
  private double distance;
  private double speed;
  /** Set when the ride ends, so the rider may get off. */
  private boolean releasing;

  public EntityZiplineTrolley(World world) {
    super(world);
    setSize(0.4F, 0.4F);
    noClip = true;
    setNoGravity(true);
  }

  // region Starting a ride

  /**
   * Clips a player onto a launch anchor's line, if it has one and the start is clear.
   *
   * @return whether they are riding
   *
   * @since 1.0
   */
  public static boolean ride(World world, BlockPos launch, EntityPlayer player) {
    if (world.isRemote || player.isRiding()) {
      return false;
    }
    TileEntityZiplineAnchor anchor = (TileEntityZiplineAnchor) Ziplines.anchorAt(world, launch);
    ZiplineLine line = anchor == null ? null : anchor.line();
    if (line == null) {
      player.sendStatusMessage(new TextComponentTranslation("csm.zipline.no_line"), true);
      return false;
    }
    for (EntityZiplineTrolley t : riding()) {
      if (t.world == world && t.line != null && t.line.sameAs(line.launch(), line.landing())
          && t.distance - line.startDistance() < ZiplineRide.SPACING) {
        player.sendStatusMessage(new TextComponentTranslation("csm.zipline.wait"), true);
        return false;
      }
    }
    EntityZiplineTrolley t = new EntityZiplineTrolley(world);
    t.line = line;
    t.distance = line.startDistance();
    t.speed = CsmConfig.getZiplineMinSpeed();
    t.place();
    world.spawnEntity(t);
    player.startRiding(t, true);
    RIDING.add(t);
    return true;
  }

  private static Set<EntityZiplineTrolley> riding() {
    RIDING.removeIf(t -> t.isDead);
    return RIDING;
  }

  // endregion

  // region Ticking

  @Override
  protected void entityInit() {
    dataManager.register(DISTANCE, 0F);
    dataManager.register(SPEED, 0F);
  }

  @Override
  public void onUpdate() {
    super.onUpdate();
    if (line == null) {
      setDead();
      return;
    }
    if (world.isRemote) {
      clientStep();
      return;
    }
    Entity rider = rider();
    if (rider == null) {
      if (ticksExisted > 1) {
        setDead();
      }
      return;
    }
    rider.fallDistance = 0;
    double stop = line.stopDistance();
    speed = ZiplineRide.next(speed, line.slope(distance), stop - distance,
        distance >= line.brakeDistance(), gapAhead(), CsmConfig.getZiplineMinSpeed(),
        CsmConfig.getZiplineMaxSpeed());
    distance = Math.min(stop, distance + speed * ZiplineRide.TICK);
    dataManager.set(DISTANCE, (float) distance);
    dataManager.set(SPEED, (float) speed);
    place();
    if (distance >= stop - 1.0e-6) {
      finish(rider);
    }
  }

  /** How far ahead the next rider on this line is, or infinity. */
  private double gapAhead() {
    double best = Double.POSITIVE_INFINITY;
    for (EntityZiplineTrolley t : riding()) {
      if (t != this && t.world == world && t.line != null
          && t.line.sameAs(line.launch(), line.landing()) && t.distance > distance) {
        best = Math.min(best, t.distance - distance);
      }
    }
    return best;
  }

  /** A client: run on at the synced speed, and ease towards the server's distance. */
  private void clientStep() {
    double target = dataManager.get(DISTANCE);
    double v = dataManager.get(SPEED);
    distance += v * ZiplineRide.TICK;
    double error = target - distance;
    if (Math.abs(error) > 3.0) {
      distance = target;
    } else {
      distance += error * 0.3;
    }
    distance = Math.max(0, Math.min(line.length(), distance));
    place();
  }

  /** Puts the trolley on the cable at its distance, facing along the line. */
  private void place() {
    Vec3d p = line.point(distance);
    setPosition(p.x, p.y - height / 2, p.z);
    rotationYaw = line.yaw();
  }

  /** The ride is over: the rider is set down on the landing, facing on. */
  private void finish(Entity rider) {
    Vec3d spot = Ziplines.landingSpot(world, line);
    releasing = true;
    rider.dismountRidingEntity();
    float yaw = line.yaw();
    if (spot == null) {
      // The landing went since the line was strung: let them down where they are.
      spot = new Vec3d(rider.posX, rider.posY, rider.posZ);
    }
    if (rider instanceof EntityPlayerMP) {
      ((EntityPlayerMP) rider).connection.setPlayerLocation(spot.x, spot.y, spot.z, yaw,
          rider.rotationPitch);
    } else {
      rider.setLocationAndAngles(spot.x, spot.y, spot.z, yaw, rider.rotationPitch);
    }
    rider.fallDistance = 0;
    setDead();
  }

  @Override
  public void setDead() {
    super.setDead();
    RIDING.remove(this);
  }

  // endregion

  // region Riding

  /** The rider hangs from the bar below the trolley. */
  @Override
  public double getMountedYOffset() {
    return height / 2 - ZiplineLine.RIDER_DROP;
  }

  /** Hanging, not sitting. */
  @Override
  public boolean shouldRiderSit() {
    return false;
  }

  /**
   * The rider, or null. Not {@code getControllingPassenger}: nobody steers a trolley, and a
   * controlling player's client would send the vehicle's moves, which a dedicated server takes
   * (and kicks for "floating a vehicle too long" without allow-flight). With no controller the
   * server alone moves it.
   */
  @Nullable
  private Entity rider() {
    return getPassengers().isEmpty() ? null : getPassengers().get(0);
  }

  @Override
  protected boolean canBeRidden(Entity entity) {
    return entity instanceof EntityLivingBase;
  }

  @Override
  protected boolean canFitPassenger(Entity passenger) {
    return getPassengers().isEmpty();
  }

  /**
   * Whether a rider may get off now: at the end of the ride, in the brake zone (sneaking off over
   * the landing), or because they are no longer a live player sneaking (death, a teleport, logging
   * out). Asked by {@link ZiplineEvents}.
   *
   * @since 1.0
   */
  boolean mayDismount(Entity rider) {
    if (releasing || isDead || line == null || !rider.isEntityAlive() || !rider.isSneaking()) {
      return true;
    }
    return distance >= line.brakeDistance();
  }

  /** The tracker's position packets are ignored: a client places the trolley from the line. */
  @Override
  @SideOnly(Side.CLIENT)
  public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch,
      int posRotationIncrements, boolean teleport) {
    if (line == null) {
      setPosition(x, y, z);
    }
  }

  // endregion

  // region Persistence and sync

  /** Never saved: a ride does not survive a restart, and an empty trolley must not be left. */
  @Override
  public boolean writeToNBTOptional(NBTTagCompound compound) {
    return false;
  }

  @Override
  protected void readEntityFromNBT(NBTTagCompound compound) {
  }

  @Override
  protected void writeEntityToNBT(NBTTagCompound compound) {
  }

  @Override
  public void writeSpawnData(ByteBuf buffer) {
    buffer.writeLong(line == null ? 0L : line.launch().toLong());
    buffer.writeLong(line == null ? 0L : line.landing().toLong());
    buffer.writeFloat((float) distance);
    buffer.writeFloat((float) speed);
  }

  @Override
  public void readSpawnData(ByteBuf buffer) {
    BlockPos launch = BlockPos.fromLong(buffer.readLong());
    BlockPos landing = BlockPos.fromLong(buffer.readLong());
    line = ZiplineLine.of(launch, landing);
    distance = buffer.readFloat();
    speed = buffer.readFloat();
    place();
    prevPosX = posX;
    prevPosY = posY;
    prevPosZ = posZ;
  }

  @Nullable
  public ZiplineLine getLine() {
    return line;
  }

  // endregion

  @Override
  public boolean canBeCollidedWith() {
    return false;
  }

  @Override
  public boolean canBePushed() {
    return false;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean isInRangeToRenderDist(double distance) {
    return distance < 128.0 * 128.0;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return getEntityBoundingBox().grow(0.5, ZiplineLine.RIDER_DROP, 0.5);
  }
}

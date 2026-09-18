package com.micatechnologies.minecraft.csm.codeutils;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * An invisible seat a player sits on, for blocks that can be sat in or on -- the first is the
 * construction site's portable toilet.
 *
 * <p>It uses the game's own riding: the player mounts the seat, sits in the riding pose, and
 * dismounts by sneaking. A seat carries one rider, so a block that uses it is one person at a
 * time. It lives only while it has a rider and the block it belongs to is still there; it is never
 * saved with a rider, so a world never reloads with an empty seat left behind.</p>
 *
 * <p>When the rider gets off, the game puts them in the first free space it finds around the seat,
 * which from inside a portable toilet could be out through the back wall. The seat moves them on
 * its next update to where the block says they leave (its door), after the game has placed
 * them.</p>
 *
 * <p>Core registers it ({@code csm:seat}), so a module never touches a Forge registry; a module's
 * block calls {@link #sit}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class EntityCsmSeat extends Entity {

  /** The block the seat belongs to; it goes when that block does. Not saved. */
  @Nullable
  private BlockPos anchor;
  @Nullable
  private Block anchorBlock;
  /** Where a rider is put when they get off. */
  private double exitX;
  private double exitY;
  private double exitZ;
  /** A rider who has just got off and is still to be moved to the exit. */
  @Nullable
  private Entity leaving;
  /** How high above the seat's own position the rider sits; synced, since the client places the
   *  rider too. */
  private static final DataParameter<Float> RIDER_OFFSET =
      EntityDataManager.createKey(EntityCsmSeat.class, DataSerializers.FLOAT);

  /**
   * Constructs an {@link EntityCsmSeat}, as the game does when it loads or syncs one.
   *
   * @param worldIn the world
   *
   * @since 1.0
   */
  public EntityCsmSeat(World worldIn) {
    super(worldIn);
    setSize(0.1F, 0.1F);
    noClip = true;
    setNoGravity(true);
    setInvisible(true);
  }

  /**
   * Sits {@code player} in the block at {@code anchor}: a seat at {@code (x, y, z)}, the rider
   * facing {@code facing}, and let out at {@code (exitX, exitY, exitZ)}. Refused, with a status
   * message, if the block's seat is taken. Call on the server.
   *
   * @param world       the world
   * @param anchor      the block being sat in
   * @param x           the seat's x
   * @param y           the seat's y
   * @param z           the seat's z
   * @param riderOffset how far above {@code y} the rider sits
   * @param facing      the way the rider faces
   * @param exitX       where the rider leaves, x
   * @param exitY       where the rider leaves, y
   * @param exitZ       where the rider leaves, z
   * @param player      the player
   *
   * @return whether the player sat down
   *
   * @since 1.0
   */
  public static boolean sit(World world, BlockPos anchor, double x, double y, double z,
      double riderOffset, EnumFacing facing, double exitX, double exitY, double exitZ,
      EntityPlayer player) {
    List<EntityCsmSeat> taken = world.getEntitiesWithinAABB(EntityCsmSeat.class,
        new AxisAlignedBB(anchor));
    if (!taken.isEmpty()) {
      player.sendStatusMessage(new TextComponentTranslation("gui.csm.seat.taken"), true);
      return false;
    }
    if (player.isRiding()) {
      return false;
    }
    EntityCsmSeat seat = new EntityCsmSeat(world);
    seat.anchor = anchor.toImmutable();
    seat.anchorBlock = world.getBlockState(anchor).getBlock();
    seat.getDataManager().set(RIDER_OFFSET, (float) riderOffset);
    seat.exitX = exitX;
    seat.exitY = exitY;
    seat.exitZ = exitZ;
    seat.setPositionAndRotation(x, y, z, facing.getHorizontalAngle(), 0F);
    world.spawnEntity(seat);
    player.rotationYaw = facing.getHorizontalAngle();
    player.startRiding(seat);
    return true;
  }

  @Override
  protected void entityInit() {
    dataManager.register(RIDER_OFFSET, 0F);
  }

  @Override
  public double getMountedYOffset() {
    return dataManager.get(RIDER_OFFSET);
  }

  @Override
  protected boolean canBeRidden(@Nonnull Entity entityIn) {
    return true;
  }

  @Override
  protected boolean canFitPassenger(@Nonnull Entity passenger) {
    return getPassengers().isEmpty();
  }

  @Override
  protected void removePassenger(@Nonnull Entity passenger) {
    super.removePassenger(passenger);
    if (!world.isRemote) {
      leaving = passenger;
    }
  }

  /**
   * Lets out a rider who has just got off, then goes; goes too if its block has.
   *
   * @since 1.0
   */
  @Override
  public void onUpdate() {
    super.onUpdate();
    if (world.isRemote) {
      return;
    }
    if (leaving != null) {
      leaving.setPositionAndUpdate(exitX, exitY, exitZ);
      leaving = null;
    }
    boolean blockGone = anchor == null || world.getBlockState(anchor).getBlock() != anchorBlock;
    if (blockGone || getPassengers().isEmpty()) {
      removePassengers();
      setDead();
    }
  }

  @Override
  public boolean canBeCollidedWith() {
    return false;
  }

  @Override
  public boolean canBePushed() {
    return false;
  }

  /**
   * Never saved: a seat exists only while someone sits in it.
   *
   * @since 1.0
   */
  @Override
  public boolean writeToNBTOptional(@Nonnull NBTTagCompound compound) {
    return false;
  }

  @Override
  protected void readEntityFromNBT(@Nonnull NBTTagCompound compound) {
  }

  @Override
  protected void writeEntityToNBT(@Nonnull NBTTagCompound compound) {
  }
}

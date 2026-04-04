package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * Tile entity for overheight detection sensors. Two sensors are paired on opposite sides of a road
 * to form a detection barrier. The detection zone spans the full volume between the paired sensors,
 * from their shared Y level upward by {@link #DETECTION_HEIGHT} blocks. Any entity whose bounding
 * box height exceeds {@link #MIN_ENTITY_HEIGHT} and intersects the zone is considered an
 * overheight detection. This mod-agnostic approach works with vanilla entities and modded vehicles
 * (e.g. Immersive Vehicles/MTS) without any compile-time dependencies.
 *
 * @version 1.0
 * @see TileEntityTrafficSignalController
 * @since 2024.1.0
 */
public class TileEntityOverheightDetectionSensor extends AbstractTileEntity {

  /**
   * The number of blocks above the sensor Y level that the detection zone extends.
   *
   * @since 1.0
   */
  public static final int DETECTION_HEIGHT = 8;

  /**
   * The minimum bounding box height (in blocks) for an entity to be considered relevant.
   * Filters out item drops, XP orbs, arrows, and other small entities.
   *
   * @since 1.0
   */
  public static final double MIN_ENTITY_HEIGHT = 1.0;

  /**
   * NBT key for the partner sensor's position.
   *
   * @since 1.0
   */
  private static final String PARTNER_POS_KEY = "partnerPos";

  /**
   * The position of the paired partner sensor, or null if not yet paired.
   *
   * @since 1.0
   */
  private BlockPos partnerPos;

  /**
   * Processes the reading of this tile entity's NBT data.
   *
   * @param compound the NBT tag compound to read from
   *
   * @since 1.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    partnerPos = SerializationUtils.getBlockPosFromNBTOrNull(compound, PARTNER_POS_KEY);
  }

  /**
   * Writes this tile entity's NBT data to the supplied compound.
   *
   * @param compound the NBT tag compound to write to
   *
   * @return the compound with this tile entity's data written
   *
   * @since 1.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, PARTNER_POS_KEY, partnerPos);
    return compound;
  }

  /**
   * Gets the partner sensor's position.
   *
   * @return the partner position, or null if not paired
   *
   * @since 1.0
   */
  public BlockPos getPartnerPos() {
    return partnerPos;
  }

  /**
   * Sets the partner sensor's position and marks this tile entity dirty.
   *
   * @param partnerPos the partner sensor's position, or null to unpair
   *
   * @since 1.0
   */
  public void setPartnerPos(BlockPos partnerPos) {
    this.partnerPos = partnerPos;
    markDirtySync(getWorld(), getPos());
  }

  /**
   * Returns true if this sensor is paired with another sensor.
   *
   * @return true if paired
   *
   * @since 1.0
   */
  public boolean isPaired() {
    return partnerPos != null;
  }

  /**
   * The number of blocks to expand the detection zone horizontally beyond the sensor positions.
   * This ensures reliable detection even at sprint speed (~2.8 blocks per 0.5s tick) by making
   * the zone thick enough that a fast-moving entity cannot pass through between ticks.
   *
   * @since 1.0
   */
  private static final int HORIZONTAL_PADDING = 1;

  /**
   * Computes the detection zone AABB between this sensor and its partner. The zone spans the full
   * rectangular volume between the two sensor positions, expanded by {@link #HORIZONTAL_PADDING}
   * blocks in each horizontal direction for reliable detection. The vertical range starts 1 block
   * above the minimum sensor Y (to align with the camera portion of the sensor model) and extends
   * {@link #DETECTION_HEIGHT} blocks upward.
   *
   * @return the detection zone AABB, or null if not paired
   *
   * @since 1.0
   */
  public AxisAlignedBB getDetectionZone() {
    if (partnerPos == null) {
      return null;
    }
    BlockPos thisPos = getPos();
    int minX = Math.min(thisPos.getX(), partnerPos.getX()) - HORIZONTAL_PADDING;
    int minY = Math.min(thisPos.getY(), partnerPos.getY()) + 1;
    int minZ = Math.min(thisPos.getZ(), partnerPos.getZ()) - HORIZONTAL_PADDING;
    int maxX = Math.max(thisPos.getX(), partnerPos.getX()) + 1 + HORIZONTAL_PADDING;
    int maxZ = Math.max(thisPos.getZ(), partnerPos.getZ()) + 1 + HORIZONTAL_PADDING;
    int maxY = minY + DETECTION_HEIGHT;
    return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
  }

  /**
   * Scans the detection zone for overheight entities. An entity is considered overheight if its
   * bounding box intersects the detection zone and its bounding box height exceeds
   * {@link #MIN_ENTITY_HEIGHT}. This approach is fully mod-agnostic — it detects vanilla players,
   * villagers, and any modded entity (e.g. Immersive Vehicles) with a proper bounding box.
   *
   * @return the number of overheight entities detected, or 0 if not paired or world is unavailable
   *
   * @since 1.0
   */
  public int scanForOverheightEntities() {
    if (world == null || partnerPos == null) {
      return 0;
    }

    // Verify partner still exists and is also an overheight sensor
    TileEntity partnerTE = world.getTileEntity(partnerPos);
    if (!(partnerTE instanceof TileEntityOverheightDetectionSensor)) {
      return 0;
    }

    AxisAlignedBB detectionZone = getDetectionZone();
    if (detectionZone == null) {
      return 0;
    }

    int count = 0;
    List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, detectionZone);
    for (Entity entity : entities) {
      AxisAlignedBB bb = entity.getEntityBoundingBox();
      double entityHeight = bb.maxY - bb.minY;
      if (entityHeight > MIN_ENTITY_HEIGHT) {
        count++;
      }
    }
    return count;
  }
}

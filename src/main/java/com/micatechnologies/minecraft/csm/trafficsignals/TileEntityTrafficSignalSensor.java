package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.SerializationUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The {@link TileEntityTrafficSignalSensor} is a {@link TileEntity} which is paired with an
 * {@link AbstractBlockTrafficSignalSensor} implementation. The
 * {@link TileEntityTrafficSignalSensor} is used to detect entities within a specified region
 * (defined by two corner positions) and report the number of entities detected to the paired
 * {@link TileEntityTrafficSignalController}.
 *
 * @version 2.0
 * @see AbstractBlockTrafficSignalSensor
 * @see TileEntityTrafficSignalController
 * @see TileEntity
 * @since 2023.2.0
 */
public class TileEntityTrafficSignalSensor extends AbstractTileEntity {

  /**
   * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s first scan
   * corner from NBT data.
   *
   * @since 1.0
   */
  private static final String SCAN_CORNER_1_KEY = "blockPos1";

  /**
   * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s second scan
   * corner from NBT data.
   *
   * @since 1.0
   */
  private static final String SCAN_CORNER_2_KEY = "blockPos2";

  /**
   * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s first left turn
   * lane scan corner from NBT data.
   *
   * @since 1.0
   */
  private static final String LEFT_SCAN_CORNER_1_KEY = "leftBlockPos1";

  /**
   * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s second left turn
   * lane scan corner from NBT data.
   *
   * @since 1.0
   */
  private static final String LEFT_SCAN_CORNER_2_KEY = "leftBlockPos2";

  /**
   * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s first protected
   * lane scan corner from NBT data.
   *
   * @since 1.0
   */
  private static final String PROTECTED_SCAN_CORNER_1_KEY = "protectedBlockPos1";

  /**
   * The key for storing and retrieving the {@link TileEntityTrafficSignalSensor}'s second protected
   * lane scan corner from NBT data.
   *
   * @since 1.0
   */
  private static final String PROTECTED_SCAN_CORNER_2_KEY = "protectedBlockPos2";

  /**
   * The {@link TileEntityTrafficSignalSensor}'s first scan corner.
   *
   * @since 1.0
   */
  private BlockPos scanCorner1;

  /**
   * The {@link TileEntityTrafficSignalSensor}'s second scan corner.
   *
   * @since 1.0
   */
  private BlockPos scanCorner2;

  /**
   * The {@link TileEntityTrafficSignalSensor}'s first left turn lane scan corner.
   *
   * @since 1.0
   */
  private BlockPos leftScanCorner1;

  /**
   * The {@link TileEntityTrafficSignalSensor}'s second left turn lane scan corner.
   *
   * @since 1.0
   */
  private BlockPos leftScanCorner2;

  /**
   * The {@link TileEntityTrafficSignalSensor}'s first protected lane scan corner.
   *
   * @since 1.0
   */
  private BlockPos protectedScanCorner1;

  /**
   * The {@link TileEntityTrafficSignalSensor}'s second protected lane scan corner.
   *
   * @since 1.0
   */
  private BlockPos protectedScanCorner2;

  /**
   * Processes the reading of the {@link TileEntityTrafficSignalSensor}'s NBT data from the supplied
   * NBT tag compound.
   *
   * @param compound the NBT tag compound to read the {@link TileEntityTrafficSignalSensor}'s NBT
   *                 data from
   *
   * @since 2.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {

    // Read the first corner from NBT
    scanCorner1 = SerializationUtils.getBlockPosFromNBTOrNull(compound, SCAN_CORNER_1_KEY);

    // Read the second corner from NBT
    scanCorner2 = SerializationUtils.getBlockPosFromNBTOrNull(compound, SCAN_CORNER_2_KEY);

    // Read the first left turn corner from NBT
    leftScanCorner1 = SerializationUtils.getBlockPosFromNBTOrNull(compound, LEFT_SCAN_CORNER_1_KEY);

    // Read the second left turn corner from NBT
    leftScanCorner2 = SerializationUtils.getBlockPosFromNBTOrNull(compound, LEFT_SCAN_CORNER_2_KEY);

    // Read the first protected lane corner from NBT
    protectedScanCorner1 =
        SerializationUtils.getBlockPosFromNBTOrNull(compound, PROTECTED_SCAN_CORNER_1_KEY);

    // Read the second protected lane corner from NBT
    protectedScanCorner2 =
        SerializationUtils.getBlockPosFromNBTOrNull(compound, PROTECTED_SCAN_CORNER_2_KEY);
  }

  /**
   * Returns the specified NBT tag compound with the {@link TileEntityTrafficSignalSensor}'s NBT
   * data.
   *
   * @param compound the NBT tag compound to write the {@link TileEntityTrafficSignalSensor}'s NBT
   *                 data to
   *
   * @return the NBT tag compound with the {@link TileEntityTrafficSignalSensor}'s NBT data
   *
   * @since 2.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {

    // Write the first corner to NBT
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, SCAN_CORNER_1_KEY, scanCorner1);

    // Write the second corner to NBT
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, SCAN_CORNER_2_KEY, scanCorner2);

    // Write the first left turn lane corner to NBT
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, LEFT_SCAN_CORNER_1_KEY,
        leftScanCorner1);

    // Write the second left turn lane corner to NBT
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, LEFT_SCAN_CORNER_2_KEY,
        leftScanCorner2);

    // Write the first protected lane corner to NBT
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, PROTECTED_SCAN_CORNER_1_KEY,
        protectedScanCorner1);

    // Write the second protected lane corner to NBT
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, PROTECTED_SCAN_CORNER_2_KEY,
        protectedScanCorner2);

    // Return the NBT tag compound
    return compound;
  }

  /**
   * Sets the two corners of the {@link TileEntityTrafficSignalSensor}'s scan region.
   *
   * @param blockPos1 the first corner of the {@link TileEntityTrafficSignalSensor}'s scan region
   * @param blockPos2 the second corner of the {@link TileEntityTrafficSignalSensor}'s scan region
   *
   * @return true if previously set corners were overwritten, false otherwise
   *
   * @since 1.0
   */
  public boolean setScanCorners(BlockPos blockPos1, BlockPos blockPos2) {
    boolean overwroteExisting = scanCorner1 != null && scanCorner2 != null;
    scanCorner1 = blockPos1;
    scanCorner2 = blockPos2;
    markDirtySync(getWorld(), getPos());
    return overwroteExisting;
  }

  /**
   * Sets the two corners of the {@link TileEntityTrafficSignalSensor}'s left turn lane scan
   * region.
   *
   * @param blockPos1 the first corner of the {@link TileEntityTrafficSignalSensor}'s left turn lane
   *                  scan region
   * @param blockPos2 the second corner of the {@link TileEntityTrafficSignalSensor}'s left turn
   *                  lane scan region
   *
   * @return true if previously set corners were overwritten, false otherwise
   *
   * @since 1.0
   */
  public boolean setLeftScanCorners(BlockPos blockPos1, BlockPos blockPos2) {
    boolean overwroteExisting = leftScanCorner1 != null && leftScanCorner2 != null;
    leftScanCorner1 = blockPos1;
    leftScanCorner2 = blockPos2;
    markDirtySync(getWorld(), getPos());
    return overwroteExisting;
  }

  /**
   * Sets the two corners of the {@link TileEntityTrafficSignalSensor}'s protected lane scan
   * region.
   *
   * @param blockPos1 the first corner of the {@link TileEntityTrafficSignalSensor}'s protected lane
   *                  scan region
   * @param blockPos2 the second corner of the {@link TileEntityTrafficSignalSensor}'s protected
   *                  lane scan region
   *
   * @return true if previously set corners were overwritten, false otherwise
   *
   * @since 1.0
   */
  public boolean setProtectedScanCorners(BlockPos blockPos1, BlockPos blockPos2) {
    boolean overwroteExisting = protectedScanCorner1 != null && protectedScanCorner2 != null;
    protectedScanCorner1 = blockPos1;
    protectedScanCorner2 = blockPos2;
    markDirtySync(getWorld(), getPos());
    return overwroteExisting;
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s scan region and
   * returns the number of entities found. Eligible entities are {@link EntityVillager} and
   * {@link EntityPlayer}.
   *
   * @return the number of eligible entities found within the
   *     {@link TileEntityTrafficSignalSensor}'s scan region
   *
   * @since 1.0
   */
  public int scanEntities() {
    return scanCornersForEntities(scanCorner1, scanCorner2);
  }

  /**
   * Scans for eligible entities within the scan region defined by the specified corners, and
   * returns the number of entities found. Eligible entities are {@link EntityVillager} and
   * {@link EntityPlayer}.
   *
   * @param corner1 the first corner of the scan region
   * @param corner2 the second corner of the scan region
   *
   * @return the number of eligible entities found within the scan region
   *
   * @since 1.0
   */
  private int scanCornersForEntities(BlockPos corner1, BlockPos corner2) {
    int count = 0;
    if (world != null && corner1 != null && corner2 != null) {
      AxisAlignedBB scanRange = new AxisAlignedBB(corner1, corner2);
      List<Entity> entitiesWithinAABBExcludingEntity =
          world.getEntitiesWithinAABBExcludingEntity(null,
              scanRange);
      for (Entity entity : entitiesWithinAABBExcludingEntity) {
        if (entity instanceof EntityVillager || entity instanceof EntityPlayer) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s left turn lane
   * scan region and returns the number of entities found. Eligible entities are
   * {@link EntityVillager} and {@link EntityPlayer}.
   *
   * @return the number of eligible entities found within the
   *     {@link TileEntityTrafficSignalSensor}'s left turn lane scan region
   *
   * @since 1.0
   */
  public int scanLeftEntities() {
    return scanCornersForEntities(leftScanCorner1, leftScanCorner2);
  }

  /**
   * Scans for eligible entities within the {@link TileEntityTrafficSignalSensor}'s protected lane
   * scan region and returns the number of entities found. Eligible entities are
   * {@link EntityVillager} and {@link EntityPlayer}.
   *
   * @return the number of eligible entities found within the
   *     {@link TileEntityTrafficSignalSensor}'s protected lane scan region
   *
   * @since 1.0
   */
  public int scanProtectedEntities() {
    return scanCornersForEntities(protectedScanCorner1, protectedScanCorner2);
  }
}

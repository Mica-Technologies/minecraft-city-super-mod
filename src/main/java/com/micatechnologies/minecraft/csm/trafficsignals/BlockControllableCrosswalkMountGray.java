package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Gray-colored crosswalk signal block with a rear pole mount. Supports TESR-rendered countdown
 * overlay on its north face. A cosmetic variant with a gray housing color.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableCrosswalkMountGray extends AbstractBlockControllableCrosswalkSignal
    implements ICsmRetiringBlock {

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllablecrosswalkmountgray";
  }

  @Override
  public float getCountdownZOffset() {
    return 0.125f; // rear model: north face at Z=6/16
  }

  @Override
  public float getCountdownYCenter() {
    return 0.625f; // rear model: element Y 2-18, center at 10/16
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return The bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
    return new AxisAlignedBB(0.000000, 0.000000, 0.375000, 1.000000, 1.250000, 1.375000);
  }

  @Override
  public String getReplacementBlockId() {
    return "controllablecrosswalksingle";
  }

  @Override
  public void configureReplacement(World world, BlockPos pos, NBTTagCompound oldTileEntityNBT) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityCrosswalkSignalNew) {
      TileEntityCrosswalkSignalNew newTe = (TileEntityCrosswalkSignalNew) te;
      newTe.setMountType(CrosswalkMountType.REAR);
      newTe.setBodyColor(TrafficSignalBodyColor.BATTLESHIP_GRAY);
      if (oldTileEntityNBT != null && oldTileEntityNBT.hasKey("learnedClearanceTicks")) {
        newTe.setLearnedClearanceTicks(oldTileEntityNBT.getInteger("learnedClearanceTicks"));
      }
    }
  }
}

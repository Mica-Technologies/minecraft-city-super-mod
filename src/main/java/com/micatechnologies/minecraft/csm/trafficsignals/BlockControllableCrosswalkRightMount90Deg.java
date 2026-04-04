package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockControllableCrosswalkRightMount90Deg
    extends AbstractBlockControllableCrosswalkSignal
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
    return "controllablecrosswalkrightmount90deg";
  }

  @Override
  public float getCountdownZOffset() {
    return -1; // 45-degree angled model: countdown overlay not supported
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
        return new AxisAlignedBB(-0.812500, 0.000000, 0.250000, 1.000000, 1.250000, 0.750000);
    }

  @Override
  public String getReplacementBlockId() {
    return "controllablecrosswalksinglenew";
  }

  @Override
  public void configureReplacement(World world, BlockPos pos, NBTTagCompound oldTileEntityNBT) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityCrosswalkSignalNew) {
      TileEntityCrosswalkSignalNew newTe = (TileEntityCrosswalkSignalNew) te;
      newTe.setMountType(CrosswalkMountType.RIGHT);
      newTe.setBodyTilt(TrafficSignalBodyTilt.RIGHT_ANGLE);
      if (oldTileEntityNBT != null && oldTileEntityNBT.hasKey("learnedClearanceTicks")) {
        newTe.setLearnedClearanceTicks(oldTileEntityNBT.getInteger("learnedClearanceTicks"));
      }
    }
  }
}

package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.CrosswalkMountType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Legacy crosswalk signal block (retiring). Replaced by mount-specific crosswalk signal variants
 * that support proper bounding boxes and TESR countdown rendering.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableCrosswalk extends AbstractBlockControllableCrosswalkSignal
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
    return "controllablecrosswalk";
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
        return new AxisAlignedBB(0.000000, 0.062500, 0.062500, 1.000000, 1.062500, 0.562500);
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
      newTe.setMountType(CrosswalkMountType.BASE);
      if (oldTileEntityNBT != null && oldTileEntityNBT.hasKey("learnedClearanceTicks")) {
        newTe.setLearnedClearanceTicks(oldTileEntityNBT.getInteger("learnedClearanceTicks"));
      }
    }
  }
}

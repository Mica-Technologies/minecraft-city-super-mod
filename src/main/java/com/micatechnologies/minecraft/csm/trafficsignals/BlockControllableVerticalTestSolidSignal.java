package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockControllableVerticalTestSolidSignal extends AbstractBlockControllableSignalHead {

  public BlockControllableVerticalTestSolidSignal() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.THROUGH;
  }

  @Override
  public boolean doesFlash() {
    return true;
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
    return new AxisAlignedBB(0.125000, -0.750000, 0.125000, 0.875000, 1.500000, 1.000000);
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllableverticaltestsolidsignal";
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CUTAWAY, TrafficSignalBulbStyle.INCANDESCENT, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.RED,false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CUTAWAY, TrafficSignalBulbStyle.INCANDESCENT, TrafficSignalBulbType.UP_RIGHT,
            TrafficSignalBulbColor.YELLOW,false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.YELLOW,TrafficSignalBodyColor.YELLOW,TrafficSignalBodyColor.YELLOW,
            TrafficSignalVisorType.CUTAWAY, TrafficSignalBulbStyle.INCANDESCENT, TrafficSignalBulbType.TRANSIT_RIGHT,
            TrafficSignalBulbColor.GREEN,true)
    };
    return infos;
  }
}

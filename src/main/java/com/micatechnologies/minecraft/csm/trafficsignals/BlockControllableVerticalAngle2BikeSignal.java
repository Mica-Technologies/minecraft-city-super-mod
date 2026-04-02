package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class BlockControllableVerticalAngle2BikeSignal extends AbstractBlockControllableSignalHead implements
    ICsmRetiringBlock {

  public BlockControllableVerticalAngle2BikeSignal() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.PROTECTED;
  }

  @Override
  public boolean doesFlash() {
    return true;
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
    return "controllableverticalangle2bikesignal";
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    return new TrafficSignalSectionInfo[] {
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
    };
  }

  @Override
  public String getReplacementBlockId() {
    return "controllableverticalbikesignal";
  }

  @Override
  public void configureReplacement(net.minecraft.world.World world,
      net.minecraft.util.math.BlockPos pos, NBTTagCompound oldTileEntityNBT) {
    ICsmRetiringBlock.super.configureReplacement(world, pos, oldTileEntityNBT);
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityTrafficSignalHead) {
      ((TileEntityTrafficSignalHead) te).setBodyTilt(TrafficSignalBodyTilt.RIGHT_ANGLE);
    }
  }
}

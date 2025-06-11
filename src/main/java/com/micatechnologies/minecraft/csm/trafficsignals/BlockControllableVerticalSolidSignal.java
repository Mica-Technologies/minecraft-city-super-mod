package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
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

public class BlockControllableVerticalSolidSignal extends AbstractBlockControllableSignalHead {

  public BlockControllableVerticalSolidSignal() {
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
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllableverticalsolidsignal";
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    System.out.println("BlockControllableVerticalSolidSignal.getDefaultTrafficSignalSectionInfo() called");
    TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.TUNNEL, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.TUNNEL, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.TUNNEL, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
    };
    System.out.println("BlockControllableVerticalSolidSignal.getDefaultTrafficSignalSectionInfo() created " + infos.length + " sections");
    return infos;
  }
}

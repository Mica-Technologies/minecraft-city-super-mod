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

public class BlockControllableSingleSolidSignalYellowGray extends AbstractBlockControllableSignalHead implements
    ICsmRetiringBlock {

  public BlockControllableSingleSolidSignalYellowGray() {
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
    return "controllablesinglesolidsignalyellowgray";
  }

  @Override
  public boolean shouldLightAllSections(int colorState) {
    return colorState == 0 || colorState == 1;
  }

  @Override
  public float getSignalYOffset() {
    return 2.0f;
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false)
    };
    return infos;
  }

  @Override
  public String getReplacementBlockId() {
    return "controllablesinglesolidsignalyellow";
  }
}

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

public class BlockControllableVerticalHybridLeftAddOnSignalGray
    extends AbstractBlockControllableSignalHead implements
    ICsmRetiringBlock {

  public BlockControllableVerticalHybridLeftAddOnSignalGray() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.LEFT;
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
    return "controllableverticalhybridleftaddonsignalgray";
  }

  @Override
  public float getSignalYOffset() {
    return -7.9f;
  }

  @Override
  public TrafficSignalBulbStyle getEnforcedBulbStyle() {
    return TrafficSignalBulbStyle.LED_DOTTED;
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    return new TrafficSignalSectionInfo[] {
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CUTAWAY, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
    };
  }

  @Override
  public String getReplacementBlockId() {
    return "controllableverticalhybridleftaddonsignal";
  }
}

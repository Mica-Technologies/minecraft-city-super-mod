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

public class BlockControllableVerticalSolidSignalLED1288Inch
    extends AbstractBlockControllableSignalHead {

  public BlockControllableVerticalSolidSignalLED1288Inch() {
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
    return "controllableverticalsolidsignalled1288inch";
  }

  @Override
  public float[] getSectionYPositions(int sectionCount) {
    return new float[] {4.0f, -6.0f, -14.0f};
  }

  @Override
  public int[] getSectionSizes(int sectionCount) {
    return new int[] {12, 8, 8};
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    return new TrafficSignalSectionInfo[] {
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CUTAWAY, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CUTAWAY, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CUTAWAY, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
    };
  }
}

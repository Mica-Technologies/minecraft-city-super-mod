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

public class BlockControllableHorizontalBikeSignal extends AbstractBlockControllableSignalHead {

  public BlockControllableHorizontalBikeSignal() {
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
    return "controllablehorizontalbikesignal";
  }

  @Override
  public boolean isHorizontal() {
    return true;
  }

  @Override
  public float[] getSectionYPositions(int sectionCount) {
    return new float[] {0.0f, 0.0f, 0.0f};
  }

  @Override
  public float[] getSectionXPositions(int sectionCount) {
    return new float[] {12.0f, 0.0f, -12.0f};
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
}

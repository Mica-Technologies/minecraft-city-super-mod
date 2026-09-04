package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractBlockControllableCrosswalkAccessory
    extends AbstractBlockControllableSignal {

  public AbstractBlockControllableCrosswalkAccessory(Material p_i45394_1_) {
    super(p_i45394_1_);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.PEDESTRIAN_ACCESSORY;
  }

  @Override
  public boolean doesFlash() {
    return false;
  }

}
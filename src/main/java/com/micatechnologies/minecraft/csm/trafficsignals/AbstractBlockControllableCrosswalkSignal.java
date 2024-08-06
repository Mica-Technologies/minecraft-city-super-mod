package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractBlockControllableCrosswalkSignal extends AbstractBlockControllableSignal {

  public AbstractBlockControllableCrosswalkSignal() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.PEDESTRIAN;
  }

  @Override
  public boolean doesFlash() {
    return true;
  }
}

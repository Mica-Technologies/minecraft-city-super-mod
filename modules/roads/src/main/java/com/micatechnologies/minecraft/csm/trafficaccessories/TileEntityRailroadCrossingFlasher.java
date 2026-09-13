package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.RoadsSounds;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

/**
 * Rings the crossing bell while the flasher is powered. The bell sound is one second of
 * strikes, so playing it once a second from the server keeps it continuous for everyone in
 * range without a client-side moving sound; the lamps need nothing from here, their wig-wag
 * being in the texture.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntityRailroadCrossingFlasher extends AbstractTickableTileEntity {

  private static final long BELL_PERIOD_TICKS = 20L;
  private static final float BELL_VOLUME = 0.9F;

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public long getTickRate() {
    return BELL_PERIOD_TICKS;
  }

  @Override
  public void onTick() {
    if (!AbstractBlockRailroadCrossing.isActive(getWorld(), getPos())) {
      return;
    }
    SoundEvent bell = RoadsSounds.RAILROAD_CROSSING_BELL.getSoundEvent();
    if (bell != null) {
      getWorld().playSound(null, getPos(), bell, SoundCategory.BLOCKS, BELL_VOLUME, 1.0F);
    }
  }
}

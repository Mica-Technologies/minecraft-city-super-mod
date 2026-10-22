package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * What a station service gate keeps for its fire alarm release (#301): the panel it follows and
 * whether the alarm opened it, so a reset closes only a gate the alarm opened. Nothing ticks: the
 * gate looks once a second by a scheduled block update ({@link BlockStationGate}).
 *
 * <p>Short NBT keys: {@code fp} the panel, {@code ao} opened by the alarm.</p>
 *
 * @since 2026.10
 */
public class TileEntityStationGate extends AbstractTileEntity {

  @Nullable
  private BlockPos alarmPanel;
  private boolean alarmOpened;

  @Override
  public void readNBT(NBTTagCompound compound) {
    alarmPanel = compound.hasKey("fp") ? BlockPos.fromLong(compound.getLong("fp")) : null;
    alarmOpened = compound.getBoolean("ao");
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (alarmPanel != null) {
      compound.setLong("fp", alarmPanel.toLong());
    }
    if (alarmOpened) {
      compound.setBoolean("ao", true);
    }
    return compound;
  }

  @Nullable
  public BlockPos getAlarmPanel() {
    return alarmPanel;
  }

  /** @return {@code true} when it was not following that panel already */
  public boolean setAlarmPanel(BlockPos panel) {
    if (panel.equals(alarmPanel)) {
      return false;
    }
    alarmPanel = panel.toImmutable();
    markDirty();
    return true;
  }

  public boolean isAlarmOpened() {
    return alarmOpened;
  }

  public void setAlarmOpened(boolean alarmOpened) {
    this.alarmOpened = alarmOpened;
    markDirty();
  }

  /** Nothing a model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}

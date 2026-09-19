package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A custom door's settings, on its lower half. Data only: no renderer is registered for it, so it
 * costs nothing a frame (a chunk lists only the tile entities that have one) -- the door is drawn by
 * its baked model, which reads these settings through the block's extended state.
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityCustomDoor extends AbstractTileEntity {

  private CustomDoorSettings settings = CustomDoorSettings.DEFAULT;

  /** When the proximity sensor last saw someone, in world ticks. Not saved: a reload forgets. */
  private transient long lastSeen = Long.MIN_VALUE / 2;

  long getLastSeen() {
    return lastSeen;
  }

  void setLastSeen(long lastSeen) {
    this.lastSeen = lastSeen;
  }

  public CustomDoorSettings getSettings() {
    return settings;
  }

  /**
   * Sets the settings, and has the door redrawn with them.
   *
   * @param settings the settings
   *
   * @since 1.0
   */
  public void setSettings(CustomDoorSettings settings) {
    this.settings = settings;
    if (world != null) {
      markDirtySync(world, pos, true);
      armSensor();
    }
  }

  /** A door put down, or read back from a save, with its sensor on starts looking. */
  @Override
  public void onLoad() {
    armSensor();
  }

  /**
   * Starts a door's proximity sensor, if it has one: the block's scheduled tick looks every few
   * ticks and schedules the next look itself (see {@link BlockCustomDoor#updateTick}). Scheduling
   * one already pending does nothing, so this is safe to call whenever.
   */
  private void armSensor() {
    if (world != null && !world.isRemote && settings.proximity()) {
      world.scheduleUpdate(pos, world.getBlockState(pos).getBlock(),
          BlockCustomDoor.SENSE_TICKS);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    CustomDoorSettings read = CustomDoorSettings.read(compound);
    boolean changed = !read.equals(settings);
    settings = read;
    if (changed && world != null && world.isRemote) {
      world.markBlockRangeForRenderUpdate(pos, pos.up());
    } else if (changed) {
      // Settings written straight in (/blockdata) may have turned the sensor on.
      armSensor();
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    settings.write(compound);
    return compound;
  }

  @Override
  public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState,
      IBlockState newState) {
    return oldState.getBlock() != newState.getBlock();
  }
}

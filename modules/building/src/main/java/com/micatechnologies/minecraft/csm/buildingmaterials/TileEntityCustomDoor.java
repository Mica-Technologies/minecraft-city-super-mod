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
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    CustomDoorSettings read = CustomDoorSettings.read(compound);
    boolean changed = !read.equals(settings);
    settings = read;
    if (changed && world != null && world.isRemote) {
      world.markBlockRangeForRenderUpdate(pos, pos.up());
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

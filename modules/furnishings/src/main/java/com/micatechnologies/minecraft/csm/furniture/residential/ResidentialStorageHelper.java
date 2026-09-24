package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.Csm;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * What every {@link IResidentialStorage} block does with its slots, whatever it extends: open
 * them for a player, drop them when it is broken, and give a comparator their fill.
 *
 * @since 2026.9
 */
public final class ResidentialStorageHelper {

  private ResidentialStorageHelper() {
  }

  /**
   * Opens the storage at {@code pos} for {@code player} (server side; the client does nothing).
   *
   * @param world   the world
   * @param pos     the tile entity's position
   * @param player  the player
   * @param storage the block
   */
  public static void open(World world, BlockPos pos, EntityPlayer player,
      IResidentialStorage storage) {
    if (world.isRemote || storage.getSlots() <= 0) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityResidentialStorage) {
      ((TileEntityResidentialStorage) te).ensureSlots(storage.getSlots());
      player.openGui(Csm.instance, BlockResidentialStorage.GUI_ID, world, pos.getX(), pos.getY(),
          pos.getZ());
    }
  }

  /**
   * Drops what the storage at {@code pos} holds where it stood.
   *
   * @param world the world
   * @param pos   the tile entity's position
   */
  public static void dropContents(World world, BlockPos pos) {
    if (world.isRemote) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityResidentialStorage) {
      ((TileEntityResidentialStorage) te).forEachItem(stack -> InventoryHelper.spawnItemStack(
          world, pos.getX(), pos.getY(), pos.getZ(), stack.copy()));
    }
  }

  /**
   * The comparator signal of the storage at {@code pos}, as a chest's.
   *
   * @param world the world
   * @param pos   the tile entity's position
   *
   * @return 0 to 15
   */
  public static int comparator(World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityResidentialStorage
        ? ItemHandlerHelper.calcRedstoneFromInventory(
        ((TileEntityResidentialStorage) te).getItems()) : 0;
  }
}

package com.micatechnologies.minecraft.csm.furniture.appliance;

import com.micatechnologies.minecraft.csm.Csm;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * What every {@link IAppliance} block does, whatever it extends: fill its tank from a water
 * bucket or bottle, open its screen, drop what it held when broken, and give a comparator the
 * fill of its slots (as a furnace does).
 *
 * @since 2026.9
 */
public final class ApplianceHelper {

  private ApplianceHelper() {
  }

  /**
   * A right-click on the appliance at {@code pos}: a water bucket or bottle held fills its tank
   * (if it uses water and has room), anything else opens its screen. Server side does the work;
   * the client only answers that the click was used.
   *
   * @param world  the world
   * @param pos    the appliance
   * @param player the player
   * @param hand   the hand clicked with
   *
   * @return true: the click is used
   */
  public static boolean activate(World world, BlockPos pos, EntityPlayer player, EnumHand hand) {
    if (world.isRemote) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityAppliance)) {
      return true;
    }
    TileEntityAppliance appliance = (TileEntityAppliance) te;
    ApplianceSpec spec = appliance.spec();
    ItemStack held = player.getHeldItem(hand);
    if (spec != null && spec.usesWater() && fillFrom(appliance, spec, player, hand, held)) {
      return true;
    }
    player.openGui(Csm.instance, IAppliance.GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  /** Pours a held water bucket or bottle into the tank; false if it is neither or it is full. */
  private static boolean fillFrom(TileEntityAppliance appliance, ApplianceSpec spec,
      EntityPlayer player, EnumHand hand, ItemStack held) {
    boolean bucket = held.getItem() == Items.WATER_BUCKET;
    boolean bottle = held.getItem() == Items.POTIONITEM
        && PotionUtils.getPotionFromItem(held) == PotionTypes.WATER;
    if (!(bucket || bottle) || !appliance.addWater(bucket ? spec.getWaterCapacity() : 1)) {
      return false;
    }
    if (!player.capabilities.isCreativeMode) {
      ItemStack empty = new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE);
      held.shrink(1);
      if (held.isEmpty()) {
        player.setHeldItem(hand, empty);
      } else if (!player.inventory.addItemStackToInventory(empty)) {
        player.dropItem(empty, false);
      }
    }
    appliance.getWorld().playSound(null, appliance.getPos(),
        bucket ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BOTTLE_EMPTY,
        SoundCategory.BLOCKS, 0.8F, 1.0F);
    return true;
  }

  /**
   * Drops what the appliance at {@code pos} holds where it stood.
   *
   * @param world the world
   * @param pos   the appliance
   */
  public static void dropContents(World world, BlockPos pos) {
    if (world.isRemote) {
      return;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityAppliance) {
      ((TileEntityAppliance) te).forEachItem(stack -> InventoryHelper.spawnItemStack(world,
          pos.getX(), pos.getY(), pos.getZ(), stack.copy()));
    }
  }

  /**
   * The comparator signal of the appliance at {@code pos}: how full its slots are.
   *
   * @param world the world
   * @param pos   the appliance
   *
   * @return 0 to 15
   */
  public static int comparator(World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityAppliance
        ? ItemHandlerHelper.calcRedstoneFromInventory(((TileEntityAppliance) te).getItems()) : 0;
  }
}

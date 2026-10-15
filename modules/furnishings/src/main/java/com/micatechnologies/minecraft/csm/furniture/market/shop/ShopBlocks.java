package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.Csm;
import javax.annotation.Nullable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * What every shop block does the same way, whatever it extends: who it belongs to when placed,
 * which screen a click opens, and who may break it.
 *
 * <p><b>Clicks.</b> A player shop's owner clicking it opens the setup screen; sneaking, they see
 * the counter as a customer would. Anyone else gets the counter. A server shop opens the counter
 * for everyone, and its managers sneak-click (empty-handed) to set it up, as SUM's server shop
 * does; an admin reaches anyone's setup the same way. A shop with no owner (one set with a
 * command) belongs to the first player to click it.</p>
 *
 * <p><b>Breaking.</b> Only those who may manage a shop can break it, in creative or not, and it
 * shrugs off explosions. What it holds drops, emeralds in its till included. Money cannot drop,
 * so a shop with money in its till is emptied into its owner's wallet as they break it, or, if
 * that cannot be done (the economy is gone, or an admin is breaking it), cannot be broken until
 * the till is collected.</p>
 *
 * @since 2026.10
 */
public final class ShopBlocks {

  /** Explosion resistance: a shop holds other players' value. */
  public static final float RESISTANCE = 2000.0F;

  private ShopBlocks() {
  }

  @Nullable
  public static TileEntityShop shop(IBlockAccess world, BlockPos pos, IShopBlock block) {
    TileEntity te = world.getTileEntity(block.getShopPos(world, pos));
    return te instanceof TileEntityShop ? (TileEntityShop) te : null;
  }

  /** The placer owns it. */
  public static void placed(World world, BlockPos pos, IShopBlock block,
      EntityLivingBase placer) {
    if (world.isRemote || !(placer instanceof EntityPlayer)) {
      return;
    }
    TileEntityShop shop = shop(world, pos, block);
    if (shop != null && !shop.isOwned() && !shop.isServerShop()) {
      shop.claim((EntityPlayer) placer);
    }
  }

  /** A click: opens the setup or the counter. */
  public static boolean activate(World world, BlockPos pos, EntityPlayer player, EnumHand hand,
      IShopBlock block) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (player.isSneaking() && !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntityShop shop = shop(world, pos, block);
    if (shop == null) {
      return true;
    }
    if (!shop.isOwned() && !shop.isServerShop()) {
      shop.claim(player);
    }
    boolean setup = player.isSneaking() ? ShopPermissions.mayManage(player, shop)
        && !(shop.isOwner(player) && !shop.isServerShop())
        : shop.isOwner(player) && !shop.isServerShop();
    openScreen(player, shop, setup);
    return true;
  }

  /** Opens the setup ({@code setup}) or the counter for {@code player}, server side. */
  static void openScreen(EntityPlayer player, TileEntityShop shop, boolean setup) {
    BlockPos at = shop.getPos();
    player.openGui(Csm.instance, setup ? IShopBlock.GUI_SETUP : IShopBlock.GUI_COUNTER,
        shop.getWorld(), at.getX(), at.getY(), at.getZ());
    if (player instanceof EntityPlayerMP) {
      ShopStatusPacket.send((EntityPlayerMP) player, shop);
    }
  }

  /**
   * Whether {@code player} may break the shop now. Empties a money till into its owner's wallet
   * first; tells them why not otherwise.
   */
  public static boolean mayBreak(World world, BlockPos pos, EntityPlayer player,
      IShopBlock block) {
    TileEntityShop shop = shop(world, pos, block);
    if (shop == null) {
      return true;
    }
    if (!ShopPermissions.mayManage(player, shop)) {
      if (!world.isRemote && player instanceof EntityPlayerMP) {
        ShopTrade.tell((EntityPlayerMP) player, "csm.shop.msg.not_yours");
      }
      return false;
    }
    if (world.isRemote || shop.getTillMoney() <= 0) {
      return true;
    }
    if (shop.isOwner(player) && player instanceof EntityPlayerMP && ShopEconomy.isMoneyMode()) {
      ShopTrade.withdraw((EntityPlayerMP) player, shop);
    }
    if (shop.getTillMoney() > 0) {
      if (player instanceof EntityPlayerMP) {
        ShopTrade.tell((EntityPlayerMP) player, "csm.shop.msg.till_not_empty");
      }
      return false;
    }
    return true;
  }

  /** Breaking speed: none for those who may not break it. */
  public static boolean mayDig(IBlockAccess world, BlockPos pos, EntityPlayer player,
      IShopBlock block) {
    TileEntityShop shop = shop(world, pos, block);
    return shop == null || ShopPermissions.mayManage(player, shop);
  }
}

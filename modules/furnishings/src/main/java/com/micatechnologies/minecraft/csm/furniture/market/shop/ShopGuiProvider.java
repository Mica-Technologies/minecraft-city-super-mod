package com.micatechnologies.minecraft.csm.furniture.market.shop;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The shops' two screens: the setup (its container holds the shop's slots) and the counter
 * (an empty container, so FML will open it from the server).
 *
 * @since 2026.10
 */
public class ShopGuiProvider implements ICsmGuiProvider {

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityShop)) {
      return null;
    }
    String title = world.getBlockState(pos).getBlock().getLocalizedName();
    if (id == IShopBlock.GUI_SETUP) {
      return new GuiShopSetup(player.inventory,
          new ContainerShopSetup(player.inventory, (TileEntityShop) te, false), title);
    }
    if (id == IShopBlock.GUI_COUNTER) {
      return new GuiShopCounter(new ContainerShopCounter((TileEntityShop) te), title);
    }
    return null;
  }

  @Nullable
  @Override
  public Object getServerGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (id == IShopBlock.GUI_SETUP && te instanceof TileEntityShop
        && ShopPermissions.mayManage(player, (TileEntityShop) te)) {
      return new ContainerShopSetup(player.inventory, (TileEntityShop) te, true);
    }
    if (id == IShopBlock.GUI_COUNTER && te instanceof TileEntityShop) {
      return new ContainerShopCounter((TileEntityShop) te);
    }
    return null;
  }
}

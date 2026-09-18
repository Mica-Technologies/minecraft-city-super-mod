package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * An item that fits one {@link ScaffoldAddon} to a placed scaffold bay: right-click the bay.
 *
 * <p>Fitting sets the bay's stored flag and uses up one item; a bay that already has the add-on
 * refuses, and keeps the item. Taking it off again is the block's own job -- sneak and
 * right-click with an empty hand -- and gives the item back.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class ItemScaffoldAddon extends AbstractItem {

  /**
   * The add-on this item fits.
   *
   * @return the add-on
   *
   * @since 1.0
   */
  public abstract ScaffoldAddon getAddon();

  @Override
  public String getItemRegistryName() {
    return getAddon().getItemRegistryName();
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    IBlockState state = worldIn.getBlockState(pos);
    if (!(state.getBlock() instanceof BlockScaffoldFrame)) {
      return EnumActionResult.PASS;
    }
    ScaffoldAddon addon = getAddon();
    if (state.getValue(addon.getProperty())) {
      return EnumActionResult.FAIL;
    }
    if (!worldIn.isRemote) {
      worldIn.setBlockState(pos, state.withProperty(addon.getProperty(), true), 3);
      if (!player.capabilities.isCreativeMode) {
        player.getHeldItem(hand).shrink(1);
      }
      worldIn.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 0.8F,
          1.0F);
    }
    return EnumActionResult.SUCCESS;
  }
}

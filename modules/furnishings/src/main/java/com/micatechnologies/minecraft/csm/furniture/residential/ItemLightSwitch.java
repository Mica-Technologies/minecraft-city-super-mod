package com.micatechnologies.minecraft.csm.furniture.residential;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The light switch's item, which carries a link ({@link SwitchLinks}). A right-click on a block
 * that switches with redstone links it (and places nothing); a sneak-right-click on the air or
 * on any other block clears the link; a plain right-click on any other block places the switch,
 * which keeps the link. Linking is checked before the clicked block's own click, so a door or a
 * lamp is linked rather than opened or switched.
 *
 * @since 2026.9
 */
public class ItemLightSwitch extends ItemBlock {

  /**
   * Constructs the item.
   *
   * @param block the light switch
   */
  public ItemLightSwitch(Block block) {
    super(block);
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos,
      EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (SwitchLinks.isTarget(world.getBlockState(pos))) {
      if (!world.isRemote) {
        SwitchLinks.link(stack, player, world, pos);
      }
      return EnumActionResult.SUCCESS;
    }
    if (player.isSneaking()) {
      if (!world.isRemote) {
        SwitchLinks.clear(stack, player);
      }
      return EnumActionResult.SUCCESS;
    }
    return EnumActionResult.PASS;
  }

  @Override
  @Nonnull
  public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, EntityPlayer player,
      @Nonnull EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    if (player.isSneaking() && SwitchLinks.readLink(stack) != null) {
      if (!world.isRemote) {
        SwitchLinks.clear(stack, player);
      }
      return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    return super.onItemRightClick(world, player, hand);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
      @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    int[] link = SwitchLinks.readLink(stack);
    if (link != null) {
      tooltip.add(I18n.format("csm.furnishings.switch.tooltip", link[0], link[1], link[2]));
    } else {
      tooltip.add(I18n.format("csm.furnishings.switch.hint"));
    }
  }

  /** A linked switch shimmers, as an enchanted item does, so it is told from a plain one. */
  @Override
  @SideOnly(Side.CLIENT)
  public boolean hasEffect(@Nonnull ItemStack stack) {
    return SwitchLinks.readLink(stack) != null;
  }
}

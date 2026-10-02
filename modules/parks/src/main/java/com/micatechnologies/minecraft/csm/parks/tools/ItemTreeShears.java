package com.micatechnologies.minecraft.csm.parks.tools;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tree shears (loppers): right-click a branch, leaves or moss within reach to cut it, with what it
 * alone held ({@link BranchCutting}).
 *
 * @since 2026.10
 */
public class ItemTreeShears extends ItemBranchTool {

  public ItemTreeShears() {
    super(476);
  }

  @Override
  public String getItemRegistryName() {
    return "tree_shears";
  }

  @Override
  protected String getUseTooltipKey() {
    return "csm.parks.shears.tooltip.use";
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    return tryCut(world, player, pos, player.getHeldItem(hand));
  }
}

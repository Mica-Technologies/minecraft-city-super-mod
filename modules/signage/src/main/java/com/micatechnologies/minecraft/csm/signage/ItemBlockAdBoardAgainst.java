package com.micatechnologies.minecraft.csm.signage;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The item of a board that is {@link AdBoardKind#isPlacedAgainst() set against} something: the
 * shelter panel. The board is built up from the block placed, so a click on the upper half of
 * what it is set against -- a shelter's end frame, at eye level -- would build it a block too
 * high. Where the cell under the one clicked into is open and the block under the one clicked is
 * there to be set against, the board is placed a block lower instead, as if that had been
 * clicked.
 */
public class ItemBlockAdBoardAgainst extends ItemBlock {

  public ItemBlockAdBoardAgainst(Block block) {
    super(block);
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(@Nonnull EntityPlayer player, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX,
      float hitY, float hitZ) {
    if (facing.getAxis().isHorizontal()
        && !world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
      BlockPos below = pos.down();
      BlockPos into = below.offset(facing);
      if (!world.getBlockState(below).getBlock().isReplaceable(world, below)
          && world.getBlockState(into).getBlock().isReplaceable(world, into)) {
        return super.onItemUse(player, world, below, hand, facing, hitX, hitY, hitZ);
      }
    }
    return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
  }
}

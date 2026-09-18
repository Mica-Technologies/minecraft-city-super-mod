package com.micatechnologies.minecraft.csm.constructionsite;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The item for a crane part that comes in liveries: the livery is its metadata, and each livery
 * has its own name ({@code tile.<name>.<livery>.name}).
 *
 * <p>For a mast section it is also how a crane climbs: used on any block of a crane whose mast is
 * the same size -- not sneaking -- it inserts a section under the head (see
 * {@link CraneClimber}). Sneaking places it as an ordinary block, so a mast can still be built
 * beside a crane.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class ItemBlockCraneLivery extends ItemBlock {

  /**
   * Constructs an {@link ItemBlockCraneLivery}.
   *
   * @param block the crane part this is the item for
   *
   * @since 1.0
   */
  public ItemBlockCraneLivery(Block block) {
    super(block);
    setHasSubtypes(true);
    setMaxDamage(0);
  }

  @Override
  public int getMetadata(int damage) {
    return damage;
  }

  @Override
  @Nonnull
  public String getTranslationKey(@Nonnull ItemStack stack) {
    return super.getTranslationKey(stack) + "."
        + CraneLivery.fromOrdinal(stack.getMetadata()).getName();
  }

  @Override
  @Nonnull
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() && block instanceof BlockCraneMast) {
      TileEntityCraneHead head = CraneLocator.findHead(worldIn, pos);
      if (head != null && head.isLarge() == (block instanceof BlockCraneMastLarge)) {
        if (!worldIn.isRemote) {
          ItemStack stack = player.getHeldItem(hand);
          CraneClimber.climb(worldIn, head, (BlockCraneMast) block,
              CraneLivery.fromOrdinal(stack.getMetadata()), stack, player);
        }
        return EnumActionResult.SUCCESS;
      }
    }
    return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
  }
}

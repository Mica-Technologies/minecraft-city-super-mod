package com.micatechnologies.minecraft.csm.technology;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Custom {@link ItemBlock} for fare gates. Shifts the placement target one cell upward so
 * the gate's "cabinet" portion (which extends one cell below the placed cell in the model)
 * naturally lands on the surface the player clicked, and the gate's actual block cell ends
 * up at chest height — which is where the player is naturally aiming when they look at the
 * gate horizontally. That makes the gate clickable without having to look down.
 *
 * <p>Without this shift, clicking the floor would put the gate's block cell at floor level,
 * with the cabinet appearing one block underground (invisible) and the door at floor level
 * where horizontal aim doesn't reach.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class ItemBlockFareGate extends ItemBlock {

  public ItemBlockFareGate(Block block) {
    super(block);
    // Inherit the registry name set by AbstractBlock after construction.
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    // Vanilla logic: if the clicked block isn't replaceable, target the adjacent cell.
    IBlockState clickedState = worldIn.getBlockState(pos);
    if (!clickedState.getBlock().isReplaceable(worldIn, pos)) {
      pos = pos.offset(facing);
    }

    // Shift +1 in Y so the gate's block cell ends up at chest level when the player aimed
    // at the floor. The cabinet visually fills the cell at the original placement target
    // (so it lines up with the floor / surface they aimed at).
    BlockPos shifted = pos.up();

    ItemStack stack = player.getHeldItem(hand);
    if (stack.isEmpty()) {
      return EnumActionResult.FAIL;
    }
    if (!player.canPlayerEdit(shifted, facing, stack)
        || !player.canPlayerEdit(pos, facing, stack)) {
      return EnumActionResult.FAIL;
    }
    // Both cells must be air or replaceable so the gate can occupy chest level and the
    // cabinet can show without overlapping into a real block in the floor cell.
    if (!worldIn.getBlockState(shifted).getBlock().isReplaceable(worldIn, shifted)) {
      return EnumActionResult.FAIL;
    }
    if (!this.block.canPlaceBlockAt(worldIn, shifted)) {
      return EnumActionResult.FAIL;
    }

    int meta = this.getMetadata(stack.getMetadata());
    IBlockState toPlace = this.block.getStateForPlacement(worldIn, shifted, facing,
        hitX, hitY, hitZ, meta, player, hand);
    if (placeBlockAt(stack, player, worldIn, shifted, facing, hitX, hitY, hitZ, toPlace)) {
      toPlace = worldIn.getBlockState(shifted);
      SoundType sound = toPlace.getBlock().getSoundType(toPlace, worldIn, shifted, player);
      worldIn.playSound(player,
          shifted, sound.getPlaceSound(),
          SoundCategory.BLOCKS,
          (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
      stack.shrink(1);
    }
    return EnumActionResult.SUCCESS;
  }
}

package com.micatechnologies.minecraft.csm.constructionsite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Climbs a tower crane: inserts a mast section under its head, the way a real crane is jacked
 * up, so the top never has to be rebuilt.
 *
 * <p>The head moves up one block, its configuration with it, and a new section goes in where it
 * was. On a 2x2 the section is a whole layer -- the head's own quarter and the three beside it --
 * and takes four mast pieces. The new section is the livery of the pieces used, so a mast can be
 * built in two colours. Refused, with a message, at the build limit, when anything is in the way,
 * or without enough pieces.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CraneClimber {

  private CraneClimber() {
  }

  /**
   * Climbs the crane {@code head} belongs to by one section. Call on the server.
   *
   * @param world  the world
   * @param head   the crane's head
   * @param mast   the mast block the section is made of (1x1 or 2x2)
   * @param livery the livery of the section
   * @param stack  the stack the pieces come from
   * @param player the player climbing it
   *
   * @return whether the crane climbed
   *
   * @since 1.0
   */
  public static boolean climb(World world, TileEntityCraneHead head, BlockCraneMast mast,
      CraneLivery livery, ItemStack stack, EntityPlayer player) {
    BlockPos at = head.getPos();
    BlockPos above = at.up();
    if (above.getY() >= world.getHeight()) {
      refuse(player, "limit");
      return false;
    }
    // The positions the new section fills: the head's own, and on a 2x2 the three beside it.
    List<BlockPos> section = new ArrayList<>();
    section.add(at);
    if (head.isLarge()) {
      int dx = head.getCentreX() > 0 ? 1 : -1;
      int dz = head.getCentreZ() > 0 ? 1 : -1;
      section.add(at.add(dx, 0, 0));
      section.add(at.add(0, 0, dz));
      section.add(at.add(dx, 0, dz));
    }
    int pieces = section.size();
    if (!player.capabilities.isCreativeMode && stack.getCount() < pieces) {
      refuse(player, "pieces");
      return false;
    }
    if (!replaceable(world, above)) {
      refuse(player, "blocked");
      return false;
    }
    for (BlockPos p : section.subList(1, section.size())) {
      if (!replaceable(world, p)) {
        refuse(player, "blocked");
        return false;
      }
    }

    // Move the head up, configuration and all.
    IBlockState headState = world.getBlockState(at);
    NBTTagCompound tag = head.writeToNBT(new NBTTagCompound());
    tag.setInteger("x", above.getX());
    tag.setInteger("y", above.getY());
    tag.setInteger("z", above.getZ());
    world.setBlockState(above, headState, 3);
    TileEntity moved = world.getTileEntity(above);
    if (moved instanceof TileEntityCraneHead) {
      moved.readFromNBT(tag);
      ((TileEntityCraneHead) moved).markDirtySync(world, above, true);
    }
    // And the new section where it was.
    IBlockState section0 = mast.getDefaultState().withProperty(BlockCraneMast.LIVERY, livery);
    for (BlockPos p : section) {
      world.setBlockState(p, section0, 3);
    }
    if (!player.capabilities.isCreativeMode) {
      stack.shrink(pieces);
    }
    world.playSound(null, at, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.5F, 1.4F);
    return true;
  }

  private static boolean replaceable(World world, BlockPos pos) {
    Block block = world.getBlockState(pos).getBlock();
    return block.isReplaceable(world, pos);
  }

  private static void refuse(EntityPlayer player, String why) {
    player.sendStatusMessage(new TextComponentTranslation("gui.csm.crane.climb." + why), true);
  }
}

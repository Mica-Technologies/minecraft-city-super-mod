package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A wall first aid cabinet. Click it for a first aid kit: one per player per in-game day from each
 * cabinet, so a cabinet is a place to patch up, not a supply. The day each player last took one
 * is kept on the player, by cabinet position, so the cabinet itself stores nothing.
 *
 * @since 2026.9
 */
public class BlockFirstAidCabinet extends BlockFireProtectionProp {

  private static final String TAKEN_PREFIX = "csmFirstAid";

  public BlockFirstAidCabinet(String registryName, int[] box) {
    super(registryName, box, true);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || world.isRemote) {
      return true;
    }
    long day = world.getWorldTime() / 24000;
    String key = TAKEN_PREFIX + pos.toLong();
    if (player.getEntityData().hasKey(key) && player.getEntityData().getLong(key) == day) {
      player.sendMessage(new TextComponentTranslation("csm.lifesafety.first_aid.empty"));
      return true;
    }
    Item kit = CsmRegistry.getItem("first_aid_kit");
    if (kit == null) {
      return true;
    }
    player.getEntityData().setLong(key, day);
    ItemStack stack = new ItemStack(kit);
    if (!player.inventory.addItemStackToInventory(stack)) {
      player.dropItem(stack, false);
    }
    world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.5F, 1.4F);
    return true;
  }
}

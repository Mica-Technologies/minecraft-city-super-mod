package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * What the pawn blocks that pay out (the counter, the drop-off bin) do the same way, whatever
 * they extend: only {@value PawnPermissions#NODE_ADMIN} places or breaks one, since each pays
 * from nothing, and a creeper does not move one either.
 *
 * @since 2026.10
 */
public final class PawnBlocks {

  /** As the shops': a server's pawn shop should outlast a creeper. */
  public static final float RESISTANCE = 2000.0F;

  private PawnBlocks() {
  }

  /** Placed by someone without the node: taken back up, the item returned. */
  public static void placed(World world, BlockPos pos, Block block, EntityLivingBase placer) {
    if (world.isRemote || !(placer instanceof EntityPlayer)
        || PawnPermissions.isAdmin((EntityPlayer) placer)) {
      return;
    }
    EntityPlayer player = (EntityPlayer) placer;
    world.setBlockToAir(pos);
    if (!player.capabilities.isCreativeMode) {
      ItemStack back = new ItemStack(block);
      if (!player.inventory.addItemStackToInventory(back)) {
        player.dropItem(back, false);
      }
      // The item block takes its one AFTER this, so the server's count ends where it began and
      // nothing tells the client, which has already shown one gone: resend the inventory at the
      // end of this tick, once the shrink has happened.
      if (player instanceof EntityPlayerMP) {
        PawnEvents.resyncInventory((EntityPlayerMP) player);
      }
    }
    player.sendMessage(new TextComponentTranslation("csm.pawn.msg.no_place"));
  }

  /** Whether {@code player} may break it; tells them why not. */
  public static boolean mayBreak(World world, EntityPlayer player) {
    if (PawnPermissions.isAdmin(player)) {
      return true;
    }
    if (!world.isRemote && player instanceof EntityPlayerMP) {
      player.sendMessage(new TextComponentTranslation("csm.pawn.msg.no_break"));
    }
    return false;
  }

  /** Breaking speed: none for those who may not break it. */
  public static boolean mayDig(EntityPlayer player) {
    return PawnPermissions.isAdmin(player);
  }

  /** Sends each line of a pawn's outcome. */
  static void tell(EntityPlayer player, Iterable<ITextComponent> lines) {
    for (ITextComponent line : lines) {
      player.sendMessage(line);
    }
  }
}

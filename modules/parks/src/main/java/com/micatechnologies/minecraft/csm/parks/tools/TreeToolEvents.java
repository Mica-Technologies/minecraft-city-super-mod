package com.micatechnologies.minecraft.csm.parks.tools;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The tree tools' left-click: a branch tool refuses to start on a log or crown too big for it,
 * with a hint to use the chainsaw, the stump grinder refuses a log a tree still stands on, and a
 * chainsaw or grinder that is off says how to start it. Registered on the Forge event bus from
 * the module's {@code preInit}.
 *
 * <p>The refusal cancels the click on both sides, so the block never starts to crack; the hint
 * is sent from the server only, once a click.</p>
 *
 * @since 2026.10
 */
public class TreeToolEvents {

  @SubscribeEvent
  public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
    ItemStack stack = event.getItemStack();
    Item item = stack.getItem();
    World world = event.getWorld();
    if (item instanceof ItemBranchTool) {
      IBlockState state = world.getBlockState(event.getPos());
      if (BranchCutting.judge(world, event.getPos(), state) == BranchCutting.Verdict.TOO_BIG) {
        event.setCanceled(true);
        if (!world.isRemote) {
          ((ItemBranchTool) item).tooBig(event.getEntityPlayer());
        }
      }
    } else if (item instanceof ItemStumpGrinder) {
      onGrinderClick(event, (ItemStumpGrinder) item, stack, world);
    } else if (item instanceof ItemChainsaw && !world.isRemote
        && !ItemChainsaw.isRunning(stack)
        && AnyTrees.isLog(world, event.getPos(), world.getBlockState(event.getPos()))) {
      event.getEntityPlayer().sendStatusMessage(
          new TextComponentTranslation("csm.parks.chainsaw.cold"), true);
    }
  }

  /**
   * The stump grinder against a log: refused, with the hint to fell it first, while a tree still
   * stands on it; off, the hint to start it; running, the grinding sound starts as the button goes
   * down.
   */
  private static void onGrinderClick(PlayerInteractEvent.LeftClickBlock event,
      ItemStumpGrinder grinder, ItemStack stack, World world) {
    BlockPos pos = event.getPos();
    if (!AnyTrees.isLog(world, pos, world.getBlockState(pos))) {
      return;
    }
    EntityPlayer player = event.getEntityPlayer();
    if (StumpGrinding.isStanding(ItemStumpGrinder.cells(world), pos)) {
      event.setCanceled(true);
      if (!world.isRemote) {
        grinder.standing(player);
      }
    } else if (!world.isRemote) {
      if (ItemFuelledTool.isRunning(stack)) {
        grinder.grindSound(world, player);
      } else {
        player.sendStatusMessage(new TextComponentTranslation(grinder.messageKey("cold")), true);
      }
    }
  }
}

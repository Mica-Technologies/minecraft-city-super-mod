package com.micatechnologies.minecraft.csm.parks.tools;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The tree tools' left-click: a branch tool refuses to start on a log or crown too big for it,
 * with a hint to use the chainsaw, and a chainsaw that is off says how to start it. Registered on
 * the Forge event bus from the module's {@code preInit}.
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
    } else if (item instanceof ItemChainsaw && !world.isRemote
        && !ItemChainsaw.isRunning(stack)
        && AnyTrees.isLog(world, event.getPos(), world.getBlockState(event.getPos()))) {
      event.getEntityPlayer().sendStatusMessage(
          new TextComponentTranslation("csm.parks.chainsaw.cold"), true);
    }
  }
}

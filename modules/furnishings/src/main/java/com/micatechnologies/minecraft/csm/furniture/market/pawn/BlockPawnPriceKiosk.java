package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.furniture.market.BlockMarketTall;
import com.micatechnologies.minecraft.csm.furniture.market.pawn.PawnValuation.Quote;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * The price check kiosk: two blocks tall, it says what a pawn shop would pay for the stack held
 * (and for one of it), or why it would not take it. It buys nothing, so anyone may place one.
 *
 * @since 2026.10
 */
public class BlockPawnPriceKiosk extends BlockMarketTall {

  public BlockPawnPriceKiosk(String registryName, int[] box) {
    super(registryName, box, 7, FurnishingsSounds.APPLIANCE_BEEP);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()) {
      return false;
    }
    super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    if (world.isRemote) {
      return true;
    }
    ItemStack held = player.getHeldItemMainhand();
    if (held.isEmpty()) {
      player.sendMessage(new TextComponentTranslation("csm.pawn.kiosk.hint"));
      return true;
    }
    Quote q = PawnValuation.quote(held);
    ITextComponent line;
    if (!q.isAccepted()) {
      line = new TextComponentTranslation("csm.pawn.msg.refused_item", held.getTextComponent(),
          new TextComponentTranslation(q.refusal));
      line.getStyle().setColor(TextFormatting.RED);
    } else if (held.getCount() > 1) {
      ItemStack one = held.copy();
      one.setCount(1);
      line = new TextComponentTranslation("csm.pawn.kiosk.quote_many", held.getTextComponent(),
          held.getCount(), PawnEconomy.describe(q.payout),
          PawnEconomy.describe(PawnValuation.quote(one).payout));
    } else {
      line = new TextComponentTranslation("csm.pawn.kiosk.quote_one", held.getTextComponent(),
          PawnEconomy.describe(q.payout));
    }
    player.sendMessage(line);
    return true;
  }
}

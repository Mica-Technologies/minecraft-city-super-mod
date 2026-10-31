package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumCompanionSeat;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumSeat;
import com.micatechnologies.minecraft.csm.venues.seating.IStadiumSeat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The venues module's proxy on the client. Every block is drawn from baked models, so it binds no
 * renderer. It registers the stadium seats' colour: every seat shares one set of grey models and
 * the shell is tinted (tint index 0) by the block's colour, or for an aisle-end block and the
 * companion seat by the colour of the seat beside it, so ten colours cost one set of models.
 *
 * @since 2026.10
 */
public class CsmVenuesClientProxy extends CsmVenuesCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    super.init(event);
    List<Block> tinted = new ArrayList<>();
    for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
      if (block instanceof IStadiumSeat || block instanceof BlockStadiumCompanionSeat) {
        tinted.add(block);
      }
    }
    Block[] blocks = tinted.toArray(new Block[0]);
    Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(
        (state, world, pos, tintIndex) -> {
          if (tintIndex != 0) {
            return -1;
          }
          Block block = state.getBlock();
          if (world == null || pos == null) {
            return block instanceof BlockStadiumSeat ? ((BlockStadiumSeat) block).getColour()
                : IStadiumSeat.DEFAULT_COLOUR;
          }
          if (block instanceof IStadiumSeat) {
            return ((IStadiumSeat) block).seatColour(world, pos, state);
          }
          return BlockStadiumCompanionSeat.colourBeside(world, pos);
        }, blocks);
    List<Item> items = new ArrayList<>();
    for (Block block : blocks) {
      Item item = Item.getItemFromBlock(block);
      if (item != net.minecraft.init.Items.AIR) {
        items.add(item);
      }
    }
    Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) -> {
      if (tintIndex != 0) {
        return -1;
      }
      Block block = Block.getBlockFromItem(stack.getItem());
      return block instanceof BlockStadiumSeat ? ((BlockStadiumSeat) block).getColour()
          : IStadiumSeat.DEFAULT_COLOUR;
    }, items.toArray(new Item[0]));
  }
}

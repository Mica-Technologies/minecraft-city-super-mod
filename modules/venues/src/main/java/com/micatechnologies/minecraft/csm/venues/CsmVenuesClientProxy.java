package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.venues.av.BlockVenueBanner;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityRibbonText;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityRibbonTextRenderer;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboard;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityVenueBanner;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityVenueBannerRenderer;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import com.micatechnologies.minecraft.csm.venues.ballpark.TileEntityBallparkScoreboard;
import com.micatechnologies.minecraft.csm.venues.ballpark.TileEntityBallparkScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.ballpark.TileEntityOutfieldMarker;
import com.micatechnologies.minecraft.csm.venues.ballpark.TileEntityOutfieldMarkerRenderer;
import com.micatechnologies.minecraft.csm.venues.bowling.TileEntityBowlingMonitor;
import com.micatechnologies.minecraft.csm.venues.bowling.TileEntityBowlingMonitorRenderer;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockTeamStoreFixture;
import com.micatechnologies.minecraft.csm.venues.concourse.TileEntityTeamColour;
import com.micatechnologies.minecraft.csm.venues.exterior.TileEntityMarquee;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldTurf;
import com.micatechnologies.minecraft.csm.venues.field.FieldTint;
import com.micatechnologies.minecraft.csm.venues.field.TileEntityEndZoneLettering;
import com.micatechnologies.minecraft.csm.venues.field.TileEntityEndZoneLetteringRenderer;
import com.micatechnologies.minecraft.csm.venues.field.TileEntityFieldScoreboard;
import com.micatechnologies.minecraft.csm.venues.field.TileEntityFieldScoreboardRenderer;
import com.micatechnologies.minecraft.csm.venues.field.IFieldCell;
import com.micatechnologies.minecraft.csm.venues.exterior.TileEntityMarqueeRenderer;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumCompanionSeat;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumSeat;
import com.micatechnologies.minecraft.csm.venues.seating.IStadiumSeat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The venues module's proxy on the client. It binds the nine renderers (the scoreboard's
 * screens, the ribbon board's scrolling text, the banners' lettering, the exterior marquee's
 * picture, the ballpark scoreboards' line score and count, the outfield distance markers, the
 * football and soccer scoreboards, the end zone lettering, the bowling scoring monitors) and
 * registers the colour
 * handlers: the stadium seats share one set of grey models and the shell is tinted (tint index
 * 0) by the block's colour, or for an aisle-end block and the companion seat by the colour of the
 * seat beside it, so ten colours cost one set of models; a banner's cloth (tint 0) and trim (1)
 * take its tile entity's colours, and so do a team store fixture's garments (tint 0 the team
 * colour, 1 the trim).
 *
 * @since 2026.10
 */
public class CsmVenuesClientProxy extends CsmVenuesCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    super.init(event);
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityScoreboard.class,
        new TileEntityScoreboardRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRibbonText.class,
        new TileEntityRibbonTextRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityVenueBanner.class,
        new TileEntityVenueBannerRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMarquee.class,
        new TileEntityMarqueeRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBallparkScoreboard.class,
        new TileEntityBallparkScoreboardRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityOutfieldMarker.class,
        new TileEntityOutfieldMarkerRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFieldScoreboard.class,
        new TileEntityFieldScoreboardRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityEndZoneLettering.class,
        new TileEntityEndZoneLetteringRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBowlingMonitor.class,
        new TileEntityBowlingMonitorRenderer());
    seatColours();
    bannerColours();
    storeColours();
    fieldColours();
  }

  /**
   * A football or soccer field's tints ({@link FieldTint}): the plain turf's dark stripe and the
   * end zone's colour from their metadata, a painted cell's parts from the turf and end zone
   * beside it.
   */
  private static void fieldColours() {
    List<Block> plain = new ArrayList<>();
    List<Block> cells = new ArrayList<>();
    for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
      if (block instanceof BlockFieldTurf) {
        plain.add(block);
      } else if (block instanceof IFieldCell) {
        cells.add(block);
      }
    }
    Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(
        (state, world, pos, tintIndex) -> tintIndex == 0 ? FieldTint.plain(state) : -1,
        plain.toArray(new Block[0]));
    Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(
        FieldTint::cell, cells.toArray(new Block[0]));
    List<Item> items = new ArrayList<>();
    for (Block block : plain) {
      items.add(Item.getItemFromBlock(block));
    }
    for (Block block : cells) {
      items.add(Item.getItemFromBlock(block));
    }
    Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) -> {
      Block block = Block.getBlockFromItem(stack.getItem());
      if (block instanceof BlockFieldTurf) {
        return tintIndex == 0 ? FieldTint.plain(block.getDefaultState()) : -1;
      }
      return FieldTint.cell(block.getDefaultState(), null, null, tintIndex);
    }, items.toArray(new Item[0]));
  }

  private static void storeColours() {
    List<Block> fixtures = new ArrayList<>();
    for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
      if (block instanceof BlockTeamStoreFixture) {
        fixtures.add(block);
      }
    }
    Block[] blocks = fixtures.toArray(new Block[0]);
    Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(
        (state, world, pos, tintIndex) -> {
          if (tintIndex < 0 || tintIndex > 1) {
            return -1;
          }
          TileEntity te = world == null || pos == null ? null : world.getTileEntity(pos);
          if (te instanceof TileEntityTeamColour) {
            TileEntityTeamColour c = (TileEntityTeamColour) te;
            return VenueColours.rgb(tintIndex == 0 ? c.getColour() : c.getTrim());
          }
          return VenueColours.rgb(tintIndex == 0 ? VenueColours.NAVY : VenueColours.WHITE);
        }, blocks);
    List<Item> items = new ArrayList<>();
    for (Block block : blocks) {
      items.add(Item.getItemFromBlock(block));
    }
    Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) ->
            tintIndex < 0 || tintIndex > 1 ? -1
                : VenueColours.rgb(tintIndex == 0 ? VenueColours.NAVY : VenueColours.WHITE),
        items.toArray(new Item[0]));
  }

  private static void seatColours() {
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

  private static void bannerColours() {
    List<Block> banners = new ArrayList<>();
    for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
      if (block instanceof BlockVenueBanner) {
        banners.add(block);
      }
    }
    Block[] blocks = banners.toArray(new Block[0]);
    Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(
        (state, world, pos, tintIndex) -> {
          if (tintIndex < 0 || tintIndex > 1) {
            return -1;
          }
          TileEntity te = world == null || pos == null ? null : world.getTileEntity(pos);
          if (te instanceof TileEntityVenueBanner) {
            TileEntityVenueBanner b = (TileEntityVenueBanner) te;
            return VenueColours.rgb(tintIndex == 0 ? b.getCloth() : b.getTrim());
          }
          return defaultBanner(state.getBlock(), tintIndex);
        }, blocks);
    List<Item> items = new ArrayList<>();
    for (Block block : blocks) {
      items.add(Item.getItemFromBlock(block));
    }
    Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) ->
            tintIndex < 0 || tintIndex > 1 ? -1
                : defaultBanner(Block.getBlockFromItem(stack.getItem()), tintIndex),
        items.toArray(new Item[0]));
  }

  /** A banner's colours before its tile entity says otherwise: its kind's defaults. */
  private static int defaultBanner(Block block, int tintIndex) {
    boolean retired = block instanceof BlockVenueBanner && ((BlockVenueBanner) block).isRetired();
    if (tintIndex == 0) {
      return VenueColours.rgb(retired ? VenueColours.CRIMSON : VenueColours.NAVY);
    }
    return VenueColours.rgb(retired ? VenueColours.WHITE : VenueColours.GOLD);
  }
}

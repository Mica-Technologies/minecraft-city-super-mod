package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ItemDecorativeFactory;
import com.micatechnologies.minecraft.csm.technology.BlockImac;
import com.micatechnologies.minecraft.csm.technology.BlockImacPro;
import com.micatechnologies.minecraft.csm.technology.BlockMacBookPro;
import com.micatechnologies.minecraft.csm.technology.BlockRedstoneTTS;
import com.micatechnologies.minecraft.csm.technology.BlockFareVendingMachine;
import com.micatechnologies.minecraft.csm.technology.BlockSpeakerFactory;
import com.micatechnologies.minecraft.csm.technology.BlockVerifoneMx915;
import com.micatechnologies.minecraft.csm.technology.ItemApplePencil;
import com.micatechnologies.minecraft.csm.technology.ItemFareTicket;
import com.micatechnologies.minecraft.csm.technology.ItemTransitCard;
import com.micatechnologies.minecraft.csm.technology.ItemTtsLinker;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for technology blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 8)
public class CsmTabTechnology extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabtechnology";
  }

  /**
   * Gets the block to use as the icon of the tab
   *
   * @return the block to use as the icon of the tab
   *
   * @since 1.0
   */
  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("imacpro");
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabHidden() {
    return false;
  }

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(new BlockSpeakerFactory("atls1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("appletv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.125000, 0.687500), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("bose1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("bose2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("fjs1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("fjs2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(BlockFareVendingMachine.class, fmlPreInitializationEvent);
    initTabBlock(BlockImac.class, fmlPreInitializationEvent);
    initTabBlock(BlockImacPro.class, fmlPreInitializationEvent);
    initTabBlock(new BlockSpeakerFactory("jblc1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("jblc2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(BlockMacBookPro.class, fmlPreInitializationEvent);
    initTabBlock(new BlockRotatableNSEWUDFactory("stbox", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.250000, 1.000000, 0.187500, 0.750000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("tvdish", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.000000, 0.250000, 0.812500, 1.000000, 0.812500), false, true, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("tvdishside", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.375000, 0.125000, 0.812500, 1.000000, 1.000000), false, true, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("vcs1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs6", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs7", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs8", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs9", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(BlockVerifoneMx915.class, fmlPreInitializationEvent);
    initTabBlock(new BlockRotatableNSEWUDFactory("waptpl225", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.250000, 0.937500, 0.750000, 0.750000, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("wapac", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.250000, 0.937500, 0.750000, 0.750000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("wapn", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.250000, 0.937500, 0.750000, 0.750000, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("wg", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.000000, 0.187500, 0.687500, 0.562500, 0.812500), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(BlockRedstoneTTS.class, fmlPreInitializationEvent);
    initTabItem(ItemTtsLinker.class, fmlPreInitializationEvent);
    initTabItem(ItemFareTicket.class, fmlPreInitializationEvent);
    initTabItem(ItemTransitCard.class, fmlPreInitializationEvent);
    initTabItem(new ItemDecorativeFactory("appleipadpro", "This iPad does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appleiphonese2020", "This iPhone does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appleiphonexr", "This iPhone does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appleiphonexs", "This iPhone does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appletvremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("applewatch", "This Apple Watch does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("directvremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("dishremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("fiosremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("spectrumremote", "This remote does nothing and is only for looks!"));
    initTabItem(ItemApplePencil.class, fmlPreInitializationEvent);
  }
}

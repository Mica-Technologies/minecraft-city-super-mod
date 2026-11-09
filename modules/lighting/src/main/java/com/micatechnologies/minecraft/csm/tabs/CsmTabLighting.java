package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lighting.BlockDecorativeChainFactory;
import com.micatechnologies.minecraft.csm.lighting.BlockBrightLightFactory;
import com.micatechnologies.minecraft.csm.lighting.BlockFloodlight;
import com.micatechnologies.minecraft.csm.lighting.BlockLinearLight;
import com.micatechnologies.minecraft.csm.lighting.BlockLinearLightHanger;
import com.micatechnologies.minecraft.csm.lighting.BlockBrightLightPoleColoredFactory;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoLLM;
import com.micatechnologies.minecraft.csm.lighting.BlockFBM;
import com.micatechnologies.minecraft.csm.lighting.BlockFBM2;
import com.micatechnologies.minecraft.csm.lighting.BlockHBM;
import com.micatechnologies.minecraft.csm.lighting.BlockHBM2;
import com.micatechnologies.minecraft.csm.lighting.BlockHighbayMount;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM2;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM3;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM4;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM5;
import com.micatechnologies.minecraft.csm.lighting.BlockObstructionBeacon;
import com.micatechnologies.minecraft.csm.lighting.BlockPCRM;
import com.micatechnologies.minecraft.csm.lighting.BlockRBM;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for lighting blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 4)
public class CsmTabLighting extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tablighting";
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
    return CsmRegistry.getBlock("altomvul");
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
    return true;
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
    initTabBlock(new BlockBrightLightFactory("ae115", new AxisAlignedBB(0.312500, -0.187500, 0.312500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ae115cu", new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("aeatb0", new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("aeatb2", new AxisAlignedBB(0.187500, 0.000000, 0.000000, 0.812500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("aeacm", new AxisAlignedBB(0.328125, -0.031250, 0.250000, 0.671875, 0.343750, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("aeacmnp", new AxisAlignedBB(0.328125, -0.031250, 0.250000, 0.671875, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altomvul", new AxisAlignedBB(0.187500, 0.612500, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    initTabBlock(new BlockBrightLightFactory("altomvwl", new AxisAlignedBB(0.343750, 0.250000, 0.812500, 0.750000, 0.750000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altomvwlslim", new AxisAlignedBB(0.406250, 0.250000, 0.812500, 0.656250, 0.750000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altorll", new AxisAlignedBB(0.187500, -0.031250, 0.125000, 0.812500, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altorell", new AxisAlignedBB(0.312500, 0.000000, 0.187500, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altosqll", new AxisAlignedBB(0.312500, 0.000000, 0.375000, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("cehalo", new AxisAlignedBB(0.150000, 0.937500, 0.150000, 0.850000, 1.000000, 0.850000), 0));
    initTabBlock(new BlockBrightLightFactory("cesquare", new AxisAlignedBB(0.212500, 0.937500, 0.212500, 0.787500, 1.000000, 0.787500), 0));
    initTabBlock(new BlockBrightLightFactory("cinavion", new AxisAlignedBB(0.312500, 0.000000, 0.000000, 0.687500, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("cinavionalt", new AxisAlignedBB(0.312500, 0.000000, -0.250000, 0.687500, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("creeledway", new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 0.500000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("creestylusfull", new AxisAlignedBB(0.312500, 0.687500, -1.000000, 0.687500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("creestylushalf", new AxisAlignedBB(0.312500, 0.687500, 0.000000, 0.687500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("creexsp", new AxisAlignedBB(0.250000, 0.000000, 0.125000, 0.750000, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("creeledwaysmall", new AxisAlignedBB(0.250000, 0.000000, 0.312500, 0.750000, 0.437500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("dslf", new AxisAlignedBB(0.312500, 0.812500, -1.000000, 0.687500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("dslfr", new AxisAlignedBB(-0.062500, 0.437500, -1.000000, 1.062500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("dslh", new AxisAlignedBB(0.312500, 0.812500, 0.000000, 0.687500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("dslhr", new AxisAlignedBB(-0.062500, 0.437500, 0.000000, 1.062500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geel", new AxisAlignedBB(0.312500, 0.000000, 0.375000, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geelpo", new AxisAlignedBB(0.312500, 0.000000, 0.375000, 0.687500, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geelsn", new AxisAlignedBB(0.312500, 0.000000, 0.375000, 0.687500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gees", new AxisAlignedBB(0.312500, 0.000000, 0.500000, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geespo", new AxisAlignedBB(0.312500, 0.000000, 0.500000, 0.687500, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geesn", new AxisAlignedBB(0.312500, 0.000000, 0.500000, 0.687500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geeals", new AxisAlignedBB(0.250000, -0.031250, 0.187500, 0.750000, 0.265625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geealsarm", new AxisAlignedBB(0.250000, -0.062500, 0.093750, 0.750000, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geealswall", new AxisAlignedBB(0.250000, -0.031250, 0.046875, 0.750000, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory.PostTopAimed("geealspt", new AxisAlignedBB(0.250000, -0.015625, -0.328125, 0.750000, 0.406250, 0.578125), 1));
    initTabBlock(new BlockBrightLightFactory("hpmongoose", new AxisAlignedBB(0.234375, -0.031250, -0.125000, 0.765625, 0.328125, 1.000000), 0));
    // Floodlights (#99): gen_floodlights.py; their ground, ceiling and crossarm blocks are in
    // CsmTabLightingHidden
    initTabBlock(new BlockFloodlight("lithdsxf1kn", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.328125, 0.187500, 0.531250, 0.671875, 0.578125, 1.000000), 16));
    initTabBlock(new BlockFloodlight("lithdsxf1yk", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.265625, 0.359375, 0.687500, 0.734375, 0.640625, 1.000000), 16));
    initTabBlock(new BlockFloodlight("lithdsxf1sf", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.328125, 0.000000, 0.437500, 0.671875, 0.390625, 1.000000), 16));
    initTabBlock(new BlockFloodlight("lithdsxf2kn", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.265625, 0.187500, 0.546875, 0.734375, 0.562500, 1.000000), 20));
    initTabBlock(new BlockFloodlight("lithdsxf2yk", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.203125, 0.359375, 0.703125, 0.796875, 0.640625, 1.000000), 20));
    initTabBlock(new BlockFloodlight("lithdsxf2sf", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.265625, 0.000000, 0.453125, 0.734375, 0.359375, 1.000000), 20));
    initTabBlock(new BlockFloodlight("lithdsxf3kn", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.265625, 0.187500, 0.390625, 0.734375, 0.718750, 1.000000), 24));
    initTabBlock(new BlockFloodlight("lithdsxf3yk", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.203125, 0.265625, 0.546875, 0.796875, 0.734375, 1.000000), 24));
    initTabBlock(new BlockFloodlight("lithdsxf3sf", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.265625, 0.000000, 0.296875, 0.734375, 0.531250, 1.000000), 24));
    initTabBlock(new BlockFloodlight("lithtflkn", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.203125, 0.187500, 0.312500, 0.796875, 0.812500, 1.000000), 24));
    initTabBlock(new BlockFloodlight("lithtflyk", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.140625, 0.218750, 0.484375, 0.859375, 0.781250, 1.000000), 24));
    initTabBlock(new BlockFloodlight("lithtflsf", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.203125, 0.000000, 0.218750, 0.796875, 0.609375, 1.000000), 24));
    initTabBlock(new BlockFloodlight("geefmkn", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.203125, 0.187500, 0.343750, 0.796875, 0.812500, 1.000000), 28));
    initTabBlock(new BlockFloodlight("geefmyk", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.140625, 0.218750, 0.468750, 0.859375, 0.781250, 1.000000), 28));
    initTabBlock(new BlockFloodlight("geefmsf", BlockFloodlight.Surface.WALL, new AxisAlignedBB(0.203125, 0.000000, 0.234375, 0.796875, 0.609375, 1.000000), 28));
    initTabBlock(new BlockBrightLightFactory("geform109", new AxisAlignedBB(0.312500, -0.156250, 0.437500, 0.687500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geform109td", new AxisAlignedBB(0.187500, -0.468750, 0.250000, 0.812500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400r3", new AxisAlignedBB(0.312500, -0.156250, 0.062500, 0.687500, 0.406250, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400r3cu", new AxisAlignedBB(0.312500, 0.000000, 0.062500, 0.687500, 0.406250, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400r3cunp", new AxisAlignedBB(0.312500, 0.000000, 0.062500, 0.687500, 0.312500, 1.000000), 0));
    // OBJ fixtures (#99): gen_streetlight_fixtures.py
    initTabBlock(new BlockBrightLightFactory("gem400a", new AxisAlignedBB(0.281250, -0.156250, 0.062500, 0.718750, 0.343750, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400acu", new AxisAlignedBB(0.281250, -0.031250, 0.062500, 0.718750, 0.343750, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400acunp", new AxisAlignedBB(0.281250, -0.031250, 0.062500, 0.718750, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400acugreen", new AxisAlignedBB(0.281250, -0.031250, 0.062500, 0.718750, 0.343750, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gepb", new AxisAlignedBB(0.125000, 0.000000, -1.000000, 0.875000, 0.750000, 1.125000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r1", new AxisAlignedBB(0.312500, -0.125000, 0.062500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r1np", new AxisAlignedBB(0.312500, -0.125000, 0.062500, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r2", new AxisAlignedBB(0.312500, -0.125000, 0.312500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r2cu", new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem250a", new AxisAlignedBB(0.296875, -0.140625, 0.171875, 0.703125, 0.328125, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem250acu", new AxisAlignedBB(0.296875, -0.031250, 0.171875, 0.703125, 0.328125, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem250acunp", new AxisAlignedBB(0.296875, -0.031250, 0.171875, 0.703125, 0.234375, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("hb1", new AxisAlignedBB(0.000000, 0.250000, 0.000000, 1.000000, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("hb2", new AxisAlignedBB(0.000000, 0.500000, 0.000000, 1.000000, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("l2esl2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("l2esl4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lgtlf", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lgtlh", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lrtlf", new AxisAlignedBB(-0.078125, 0.000000, -1.000000, 1.078125, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lrtlh", new AxisAlignedBB(-0.078125, 0.000000, 0.000000, 1.078125, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("troffer_slot_2x2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.312500, 1.000000), 0)); // Louvered Slot Troffer (2x2)
    initTabBlock(new BlockBrightLightFactory("troffer_slot_2x4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.312500, 1.000000), 0)); // Louvered Slot Troffer (2x4)
    initTabBlock(new BlockBrightLightFactory("troffer_basket_2x2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.312500, 1.000000), 0)); // Center Basket Troffer (2x2)
    initTabBlock(new BlockBrightLightFactory("troffer_basket_2x4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.312500, 1.000000), 0)); // Center Basket Troffer (2x4)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_bars_2x2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Staggered Bars, 2x2)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_bars_2x4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Staggered Bars, 2x4)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_dashes_2x2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Dashes, 2x2)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_dashes_2x4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Dashes, 2x4)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_squares_2x2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Squares, 2x2)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_squares_2x4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Squares, 2x4)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_dots_2x2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Dots, 2x2)
    initTabBlock(new BlockBrightLightFactory("troffer_pattern_dots_2x4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.312500, 1.000000), 0)); // Patterned Lens Troffer (Dots, 2x4)
    initTabBlock(new BlockLinearLight("linear_light_black")); // Linear LED Light (Black)
    initTabBlock(new BlockLinearLight("linear_light_silver")); // Linear LED Light (Silver)
    initTabBlock(new BlockLinearLight("linear_light_white")); // Linear LED Light (White)
    initTabBlock(new BlockLinearLight("linear_light_light_grey")); // Linear LED Light (Light Grey)
    initTabBlock(new BlockLinearLight("linear_light_charcoal")); // Linear LED Light (Charcoal)
    initTabBlock(new BlockLinearLight("linear_light_navy")); // Linear LED Light (Navy)
    initTabBlock(new BlockLinearLight("linear_light_sage")); // Linear LED Light (Sage)
    initTabBlock(new BlockLinearLight("linear_light_terracotta")); // Linear LED Light (Terracotta)
    initTabBlock(new BlockLinearLight("linear_light_oak")); // Linear LED Light (Light Oak)
    initTabBlock(new BlockLinearLight("linear_light_walnut")); // Linear LED Light (Walnut)
    initTabBlock(new BlockLinearLight("linear_light_louver_black")); // Louvered Linear LED Light (Black)
    initTabBlock(new BlockLinearLight("linear_light_louver_silver")); // Louvered Linear LED Light (Silver)
    initTabBlock(new BlockLinearLight("linear_light_louver_white")); // Louvered Linear LED Light (White)
    initTabBlock(new BlockLinearLight("linear_light_louver_light_grey")); // Louvered Linear LED Light (Light Grey)
    initTabBlock(new BlockLinearLight("linear_light_louver_charcoal")); // Louvered Linear LED Light (Charcoal)
    initTabBlock(new BlockLinearLight("linear_light_louver_navy")); // Louvered Linear LED Light (Navy)
    initTabBlock(new BlockLinearLight("linear_light_louver_sage")); // Louvered Linear LED Light (Sage)
    initTabBlock(new BlockLinearLight("linear_light_louver_terracotta")); // Louvered Linear LED Light (Terracotta)
    initTabBlock(new BlockLinearLight("linear_light_louver_oak")); // Louvered Linear LED Light (Light Oak)
    initTabBlock(new BlockLinearLight("linear_light_louver_walnut")); // Louvered Linear LED Light (Walnut)
    initTabBlock(new BlockLinearLightHanger()); // Linear Light Cable Hanger
    initTabBlock(new BlockBrightLightFactory("ltecdtd", new AxisAlignedBB(0.250000, 0.000000, 0.312500, 0.750000, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgc1v1np", new AxisAlignedBB(0.250000, 0.000000, 0.062500, 0.750000, 0.340625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgcj", new AxisAlignedBB(0.375000, 0.000000, 0.500000, 0.625000, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgcjsn", new AxisAlignedBB(0.375000, 0.000000, 0.500000, 0.625000, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgcl", new AxisAlignedBB(0.250000, 0.000000, 0.250000, 0.750000, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgcmsn", new AxisAlignedBB(0.312500, 0.000000, 0.250000, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgcmv2", new AxisAlignedBB(0.312500, 0.000000, 0.250000, 0.687500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgcmv2np", new AxisAlignedBB(0.312500, 0.000000, 0.250000, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltec", new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgc1v1", new AxisAlignedBB(0.250000, 0.000000, 0.062500, 0.750000, 0.437500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgc1v2", new AxisAlignedBB(0.250000, 0.000000, 0.062500, 0.750000, 0.437500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltgcm", new AxisAlignedBB(0.312500, 0.000000, 0.250000, 0.687500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltkc0", new AxisAlignedBB(0.343750, -0.031250, 0.406250, 0.656250, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltkc0hss", new AxisAlignedBB(0.343750, -0.062500, 0.406250, 0.656250, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltkc1", new AxisAlignedBB(0.328125, -0.031250, 0.312500, 0.671875, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltkc1hss", new AxisAlignedBB(0.328125, -0.062500, 0.312500, 0.671875, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltkc2", new AxisAlignedBB(0.296875, -0.031250, 0.125000, 0.703125, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("ltkc2hss", new AxisAlignedBB(0.296875, -0.062500, 0.125000, 0.703125, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory.PostTop("mclaclassicpostlight", new AxisAlignedBB(0.117188, 0.000000, 0.117188, 0.882813, 1.250000, 0.882813), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("mclaparklight", new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 1.000000, 0.937500), 1));
    initTabBlock(new BlockBrightLightPoleColoredFactory("postlight1", new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 1.437500, 0.937500), 1));
    initTabBlock(new BlockBrightLightPoleColoredFactory("postlight2", new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 1.437500, 0.937500), 1));
    initTabBlock(new BlockBrightLightPoleColoredFactory("postlight3", new AxisAlignedBB(-0.125000, 0.000000, -0.125000, 1.125000, 1.437500, 1.125000), 1));
    // Decorative and historic post tops for the decorative poles (gen_decorative_poles.py --fragments)
    initTabBlock(new BlockBrightLightFactory.PostTop("lawilshirespecial", new AxisAlignedBB(0.211520, 0.001250, 0.211520, 0.788480, 1.500000, 0.788480), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lametropolitan", new AxisAlignedBB(0.058999, 0.001250, 0.058999, 0.941001, 1.500000, 0.941001), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lawestwoodlantern", new AxisAlignedBB(0.106250, 0.001250, 0.159002, 0.893750, 1.500000, 0.840998), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("labroadwayrosehead", new AxisAlignedBB(-0.192333, 0.001250, 0.222296, 1.192333, 1.137500, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("laum1906twin", new AxisAlignedBB(-0.168750, 0.001250, 0.222296, 1.168750, 1.100000, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lage104globe", new AxisAlignedBB(0.210222, 0.001250, 0.210222, 0.789778, 1.468750, 0.789778), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lage198globe", new AxisAlignedBB(0.089482, 0.001250, 0.089482, 0.910518, 1.125000, 0.910518), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("laparagonjr", new AxisAlignedBB(0.101556, 0.001250, 0.101556, 0.898444, 1.337500, 0.898444), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lallewellyn5", new AxisAlignedBB(-0.066785, 0.001250, -0.066785, 1.066785, 1.262500, 1.066785), 1));
    initTabBlock(new BlockBrightLightFactory.PostTopAimed("lagenie", new AxisAlignedBB(0.222296, 0.001250, -0.321250, 0.777704, 0.975000, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lavinedouble", new AxisAlignedBB(-0.321250, 0.001250, 0.222296, 1.321250, 1.125000, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTopAimed("lahollywoodstar", new AxisAlignedBB(0.222296, 0.001250, -0.500000, 0.777704, 0.931250, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lahollywoodwof", new AxisAlignedBB(-0.137500, 0.001250, 0.222296, 1.137500, 1.500000, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("lbcalheightslantern", new AxisAlignedBB(0.222296, 0.001250, 0.222296, 0.777704, 1.087500, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("reproacorn", new AxisAlignedBB(0.161926, 0.001250, 0.161926, 0.838074, 1.350000, 0.838074), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("reprowashingtonglobe", new AxisAlignedBB(0.113630, 0.001250, 0.113630, 0.886370, 1.262500, 0.886370), 1));
    initTabBlock(new BlockBrightLightFactory.PostTop("reprotwin20", new AxisAlignedBB(-0.216481, 0.001250, 0.222296, 1.216481, 1.137500, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory.PostTopAimed("reprobishopscrook", new AxisAlignedBB(0.258519, 0.001250, -0.352187, 0.741481, 1.500000, 0.741481), 1));
    initTabBlock(new BlockBrightLightFactory.PostTopAimed("reprotypef", new AxisAlignedBB(0.222296, 0.001250, -0.494375, 0.777704, 0.937500, 0.777704), 1));
    initTabBlock(new BlockBrightLightFactory("slf", new AxisAlignedBB(0.421875, 0.875000, -1.000000, 0.578125, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("slfe", new AxisAlignedBB(0.859375, 0.875000, -1.000000, 1.000000, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("slh", new AxisAlignedBB(0.421875, 0.875000, 0.000000, 0.578125, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("slhe", new AxisAlignedBB(0.859375, 0.875000, 0.000000, 1.000000, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("smsmx", new AxisAlignedBB(0.187500, -0.062500, 0.062500, 0.812500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("smsmxll5", new AxisAlignedBB(0.234375, -0.031250, 0.156250, 0.765625, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sslf", new AxisAlignedBB(0.312500, 0.812500, -1.000000, 0.687500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sslfr", new AxisAlignedBB(-0.062500, 0.437500, -1.000000, 1.062500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sslh", new AxisAlignedBB(0.312500, 0.812500, 0.000000, 0.687500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sslhr", new AxisAlignedBB(-0.062500, 0.437500, 0.000000, 1.062500, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("searslf", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("searslf2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("searslf3", new AxisAlignedBB(-1.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("searslf4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("uledf", new AxisAlignedBB(-0.078125, 0.000000, -1.000000, 1.078125, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("uledh", new AxisAlignedBB(-0.078125, 0.000000, 0.000000, 1.078125, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov20", new AxisAlignedBB(0.312500, -0.125000, 0.437500, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov50", new AxisAlignedBB(0.312500, -0.187500, 0.062500, 0.687500, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov50finned", new AxisAlignedBB(0.312500, -0.187500, 0.062500, 0.687500, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov50np", new AxisAlignedBB(0.312500, -0.187500, 0.062500, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov50shaded", new AxisAlignedBB(0.125000, -0.187500, 0.062500, 0.875000, 0.375000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov50shadednp", new AxisAlignedBB(0.125000, -0.187500, 0.062500, 0.875000, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov25", new AxisAlignedBB(0.296875, -0.125000, 0.046875, 0.703125, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov25hps", new AxisAlignedBB(0.296875, -0.125000, 0.046875, 0.703125, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whov25early", new AxisAlignedBB(0.296875, -0.125000, 0.046875, 0.703125, 0.296875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whl250", new AxisAlignedBB(0.296875, -0.125000, 0.234375, 0.703125, 0.328125, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("whl250hps", new AxisAlignedBB(0.296875, -0.125000, 0.234375, 0.703125, 0.328125, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("wsl", new AxisAlignedBB(0.437500, 0.000000, 0.875000, 0.562500, 1.000000, 1.000000), 0));
    // Aviation obstruction beacons for tall buildings (gen_obstruction_beacons.py --fragments)
    initTabBlock(new BlockObstructionBeacon("obstruction_beacon_red", new double[]{3.4, 0, 3.4, 12.6, 10.2, 12.6}, new double[]{3, 0, 3, 13, 13.2, 16}, 9));
    initTabBlock(new BlockObstructionBeacon("obstruction_beacon_white", new double[]{3.4, 0, 3.4, 12.6, 12.2, 12.6}, new double[]{3, 0, 3, 13, 15.2, 16}, 9));
    // Decorative pendants
    initTabBlock(new BlockBrightLightFactory("pendschoolhouseblack", new AxisAlignedBB(0.187500, 0.225000, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    initTabBlock(new BlockBrightLightFactory("pendschoolhousebronze", new AxisAlignedBB(0.187500, 0.225000, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    initTabBlock(new BlockBrightLightFactory("pendschoolhousenickel", new AxisAlignedBB(0.187500, 0.225000, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    initTabBlock(new BlockBrightLightFactory("penddomeblack", new AxisAlignedBB(0.106250, 0.265625, 0.106250, 0.893750, 1.000000, 0.893750), 0));
    initTabBlock(new BlockBrightLightFactory("penddomebronze", new AxisAlignedBB(0.106250, 0.265625, 0.106250, 0.893750, 1.000000, 0.893750), 0));
    initTabBlock(new BlockBrightLightFactory("penddomenickel", new AxisAlignedBB(0.106250, 0.265625, 0.106250, 0.893750, 1.000000, 0.893750), 0));
    initTabBlock(new BlockBrightLightFactory("pendconeblack", new AxisAlignedBB(0.156250, 0.215625, 0.156250, 0.843750, 1.000000, 0.843750), 0));
    initTabBlock(new BlockBrightLightFactory("pendconebronze", new AxisAlignedBB(0.156250, 0.215625, 0.156250, 0.843750, 1.000000, 0.843750), 0));
    initTabBlock(new BlockBrightLightFactory("pendconenickel", new AxisAlignedBB(0.156250, 0.215625, 0.156250, 0.843750, 1.000000, 0.843750), 0));
    initTabBlock(new BlockBrightLightFactory("pendcagedblack", new AxisAlignedBB(0.301630, 0.281250, 0.315625, 0.698370, 1.000000, 0.684375), 0));
    initTabBlock(new BlockBrightLightFactory("pendcagedbronze", new AxisAlignedBB(0.301630, 0.281250, 0.315625, 0.698370, 1.000000, 0.684375), 0));
    initTabBlock(new BlockBrightLightFactory("pendcagednickel", new AxisAlignedBB(0.301630, 0.281250, 0.315625, 0.698370, 1.000000, 0.684375), 0));
    initTabBlock(new BlockBrightLightFactory("pendglobeblack", new AxisAlignedBB(0.262867, 0.256250, 0.262867, 0.737133, 1.000000, 0.737133), 0));
    initTabBlock(new BlockBrightLightFactory("pendglobebronze", new AxisAlignedBB(0.262867, 0.256250, 0.262867, 0.737133, 1.000000, 0.737133), 0));
    initTabBlock(new BlockBrightLightFactory("pendglobenickel", new AxisAlignedBB(0.262867, 0.256250, 0.262867, 0.737133, 1.000000, 0.737133), 0));
    // Decorative wall sconces
    initTabBlock(new BlockBrightLightFactory("sconcehalfshellblack", new AxisAlignedBB(0.137500, 0.243750, 0.640625, 0.862500, 0.575000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcehalfshellbronze", new AxisAlignedBB(0.137500, 0.243750, 0.640625, 0.862500, 0.575000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcehalfshellnickel", new AxisAlignedBB(0.137500, 0.243750, 0.640625, 0.862500, 0.575000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcecolonialblack", new AxisAlignedBB(0.368750, 0.287500, 0.718750, 0.631250, 0.793750, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcecolonialbronze", new AxisAlignedBB(0.368750, 0.287500, 0.718750, 0.631250, 0.793750, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcecolonialnickel", new AxisAlignedBB(0.368750, 0.287500, 0.718750, 0.631250, 0.793750, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcevanityblack", new AxisAlignedBB(0.087500, 0.378125, 0.693750, 0.912500, 0.621875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcevanitybronze", new AxisAlignedBB(0.087500, 0.378125, 0.693750, 0.912500, 0.621875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcevanitynickel", new AxisAlignedBB(0.087500, 0.378125, 0.693750, 0.912500, 0.621875, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcelanternblack", new AxisAlignedBB(0.281250, 0.268750, 0.500000, 0.718750, 0.884375, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcelanternbronze", new AxisAlignedBB(0.281250, 0.268750, 0.500000, 0.718750, 0.884375, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcelanternnickel", new AxisAlignedBB(0.281250, 0.268750, 0.500000, 0.718750, 0.884375, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcerlmblack", new AxisAlignedBB(0.100000, 0.256250, 0.112500, 0.900000, 0.800000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcerlmbronze", new AxisAlignedBB(0.100000, 0.256250, 0.112500, 0.900000, 0.800000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("sconcerlmnickel", new AxisAlignedBB(0.100000, 0.256250, 0.112500, 0.900000, 0.800000, 1.000000), 0));
    // Decorative ceiling fixtures
    initTabBlock(new BlockBrightLightFactory("ceilmissionblack", new AxisAlignedBB(0.187500, 0.678125, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    initTabBlock(new BlockBrightLightFactory("ceilmissionbronze", new AxisAlignedBB(0.187500, 0.678125, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    initTabBlock(new BlockBrightLightFactory("ceilmissionnickel", new AxisAlignedBB(0.187500, 0.678125, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    // Chain that extends any pendant's drop
    initTabBlock(new BlockDecorativeChainFactory("pendchainblack"));
    initTabBlock(new BlockDecorativeChainFactory("pendchainbronze"));
    initTabBlock(new BlockDecorativeChainFactory("pendchainnickel"));

    initTabBlock(BlockAltoLLM.class, fmlPreInitializationEvent);
    initTabBlock(BlockFBM.class, fmlPreInitializationEvent);
    initTabBlock(BlockFBM2.class, fmlPreInitializationEvent);
    initTabBlock(BlockHBM.class, fmlPreInitializationEvent);
    initTabBlock(BlockHBM2.class, fmlPreInitializationEvent);
    initTabBlock(BlockHighbayMount.class, fmlPreInitializationEvent);
    initTabBlock(BlockNOVTM.class, fmlPreInitializationEvent);
    initTabBlock(BlockNOVTM2.class, fmlPreInitializationEvent);
    initTabBlock(BlockNOVTM3.class, fmlPreInitializationEvent);
    initTabBlock(BlockNOVTM4.class, fmlPreInitializationEvent);
    initTabBlock(BlockNOVTM5.class, fmlPreInitializationEvent);
    initTabBlock(BlockPCRM.class, fmlPreInitializationEvent);
    initTabBlock(BlockRBM.class, fmlPreInitializationEvent);
  }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lighting.BlockFloodlight;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPT;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB2;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPT;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The hidden tab holding the Lighting blocks that never belong in the creative inventory: the
 * legacy concrete pole pieces while they retire. (The light-up air a lit fixture projects is
 * Core's, registered from {@code CsmTabCoreHidden}, so Sports &amp; Venues can place it too.)
 *
 * <p>Hidden tabs have no creative-inventory presence at all: {@link CsmTab} gives them a
 * {@code null} {@code CreativeTabs}, which is also what keeps their blocks out of the
 * Fabricator. They exist only so a retiring block stays registered and can convert an old
 * placement to its replacement.</p>
 *
 * @version 1.0
 */
@CsmTab.Load(order = -9)
public class CsmTabLightingHidden extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return null;
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
    return null;
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
    return true;
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
    // The hand-placed concrete pole pieces, retiring into the concrete poles in the Traffic
    // Accessories tab (BlockTrafficPoleConcrete), which draw their own base and top.
    initTabBlock(BlockOCPB.class, fmlPreInitializationEvent);
    initTabBlock(BlockOCPM.class, fmlPreInitializationEvent);
    initTabBlock(BlockOCPT.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPB.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPB2.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPM.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPT.class, fmlPreInitializationEvent);
    // The floodlights' blocks for the other surfaces (BlockFloodlight): placed by the visible
    // block's item on the top or underside of a block, or against a RealGrid crossarm
    initTabBlock(new BlockFloodlight("lithdsxf1kn_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.328125, 0.000000, 0.312500, 0.671875, 0.437500, 0.687500), 16));
    initTabBlock(new BlockFloodlight("lithdsxf1kn_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.328125, 0.562500, 0.312500, 0.671875, 1.000000, 0.687500), 16));
    initTabBlock(new BlockFloodlight("lithdsxf1kn_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.328125, 0.187500, 0.531250, 0.671875, 0.968750, 1.000000), 16));
    initTabBlock(new BlockFloodlight("lithdsxf1yk_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.265625, 0.000000, 0.359375, 0.734375, 0.328125, 0.640625), 16));
    initTabBlock(new BlockFloodlight("lithdsxf1yk_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.265625, 0.671875, 0.359375, 0.734375, 1.000000, 0.640625), 16));
    initTabBlock(new BlockFloodlight("lithdsxf1yk_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.265625, 0.359375, 0.687500, 0.734375, 0.968750, 1.000000), 16));
    initTabBlock(new BlockFloodlight.PostTop("lithdsxf1sf_g", new AxisAlignedBB(0.328125, 0.000000, 0.218750, 0.671875, 0.546875, 0.578125), 16));
    initTabBlock(new BlockFloodlight("lithdsxf2kn_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.265625, 0.000000, 0.312500, 0.734375, 0.421875, 0.687500), 20));
    initTabBlock(new BlockFloodlight("lithdsxf2kn_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.265625, 0.578125, 0.312500, 0.734375, 1.000000, 0.687500), 20));
    initTabBlock(new BlockFloodlight("lithdsxf2kn_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.265625, 0.187500, 0.546875, 0.734375, 0.968750, 1.000000), 20));
    initTabBlock(new BlockFloodlight("lithdsxf2yk_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.203125, 0.000000, 0.375000, 0.796875, 0.312500, 0.625000), 20));
    initTabBlock(new BlockFloodlight("lithdsxf2yk_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.203125, 0.687500, 0.375000, 0.796875, 1.000000, 0.625000), 20));
    initTabBlock(new BlockFloodlight("lithdsxf2yk_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.203125, 0.359375, 0.703125, 0.796875, 0.968750, 1.000000), 20));
    initTabBlock(new BlockFloodlight.PostTop("lithdsxf2sf_g", new AxisAlignedBB(0.265625, 0.000000, 0.234375, 0.734375, 0.531250, 0.578125), 20));
    initTabBlock(new BlockFloodlight("lithdsxf3kn_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.265625, 0.000000, 0.234375, 0.734375, 0.578125, 0.765625), 24));
    initTabBlock(new BlockFloodlight("lithdsxf3kn_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.265625, 0.421875, 0.234375, 0.734375, 1.000000, 0.765625), 24));
    initTabBlock(new BlockFloodlight("lithdsxf3kn_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.265625, 0.187500, 0.390625, 0.734375, 0.968750, 1.000000), 24));
    initTabBlock(new BlockFloodlight("lithdsxf3yk_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.203125, 0.000000, 0.281250, 0.796875, 0.468750, 0.718750), 24));
    initTabBlock(new BlockFloodlight("lithdsxf3yk_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.203125, 0.531250, 0.281250, 0.796875, 1.000000, 0.718750), 24));
    initTabBlock(new BlockFloodlight("lithdsxf3yk_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.203125, 0.265625, 0.546875, 0.796875, 0.968750, 1.000000), 24));
    initTabBlock(new BlockFloodlight.PostTop("lithdsxf3sf_g", new AxisAlignedBB(0.265625, 0.000000, 0.078125, 0.734375, 0.687500, 0.578125), 24));
    initTabBlock(new BlockFloodlight("lithtflkn_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.203125, 0.000000, 0.203125, 0.796875, 0.656250, 0.796875), 24));
    initTabBlock(new BlockFloodlight("lithtflkn_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.203125, 0.343750, 0.203125, 0.796875, 1.000000, 0.796875), 24));
    initTabBlock(new BlockFloodlight("lithtflkn_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.203125, 0.187500, 0.312500, 0.796875, 0.968750, 1.000000), 24));
    initTabBlock(new BlockFloodlight("lithtflyk_g", BlockFloodlight.Surface.GROUND, new AxisAlignedBB(0.140625, 0.000000, 0.234375, 0.859375, 0.546875, 0.765625), 24));
    initTabBlock(new BlockFloodlight("lithtflyk_c", BlockFloodlight.Surface.CEILING, new AxisAlignedBB(0.140625, 0.453125, 0.234375, 0.859375, 1.000000, 0.765625), 24));
    initTabBlock(new BlockFloodlight("lithtflyk_x", BlockFloodlight.Surface.CROSSARM, new AxisAlignedBB(0.140625, 0.218750, 0.484375, 0.859375, 0.968750, 1.000000), 24));
    initTabBlock(new BlockFloodlight.PostTop("lithtflsf_g", new AxisAlignedBB(0.203125, 0.000000, 0.000000, 0.796875, 0.765625, 0.578125), 24));
  }
}

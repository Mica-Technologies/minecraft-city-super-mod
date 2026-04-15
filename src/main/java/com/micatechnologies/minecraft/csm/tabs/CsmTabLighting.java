package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lighting.BlockBrightLightFactory;
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
import com.micatechnologies.minecraft.csm.lighting.BlockOCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPT;
import com.micatechnologies.minecraft.csm.lighting.BlockPCRM;
import com.micatechnologies.minecraft.csm.lighting.BlockRBM;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB2;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPT;
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
    initTabBlock(new BlockBrightLightFactory("altomvul", new AxisAlignedBB(0.187500, 0.612500, 0.187500, 0.812500, 1.000000, 0.812500), 0));
    initTabBlock(new BlockBrightLightFactory("altomvwl", new AxisAlignedBB(0.343750, 0.250000, 0.812500, 0.750000, 0.750000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altomvwlslim", new AxisAlignedBB(0.406250, 0.250000, 0.812500, 0.656250, 0.750000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altorll", new AxisAlignedBB(0.312500, 0.000000, 0.187500, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altorell", new AxisAlignedBB(0.312500, 0.000000, 0.187500, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("altosqll", new AxisAlignedBB(0.312500, 0.000000, 0.375000, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("cehalo", new AxisAlignedBB(0.375000, 1.000000, 0.375000, 0.625000, 1.100000, 0.625000), 0));
    initTabBlock(new BlockBrightLightFactory("cesquare", new AxisAlignedBB(0.312500, 1.000000, 0.312500, 0.687500, 1.100000, 0.687500), 0));
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
    initTabBlock(new BlockBrightLightFactory("geform109", new AxisAlignedBB(0.312500, -0.156250, 0.437500, 0.687500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("geform109td", new AxisAlignedBB(0.187500, -0.468750, 0.250000, 0.812500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400r3", new AxisAlignedBB(0.312500, -0.156250, 0.062500, 0.687500, 0.406250, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400r3cu", new AxisAlignedBB(0.312500, 0.000000, 0.062500, 0.687500, 0.406250, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem400r3cunp", new AxisAlignedBB(0.312500, 0.000000, 0.062500, 0.687500, 0.312500, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gepb", new AxisAlignedBB(0.125000, 0.000000, -1.000000, 0.875000, 0.750000, 1.125000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r1", new AxisAlignedBB(0.312500, -0.125000, 0.062500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r1np", new AxisAlignedBB(0.312500, -0.125000, 0.062500, 0.687500, 0.250000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r2", new AxisAlignedBB(0.312500, -0.125000, 0.312500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("gem240r2cu", new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.390625, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("hb1", new AxisAlignedBB(0.000000, 0.250000, 0.000000, 1.000000, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("hb2", new AxisAlignedBB(0.000000, 0.500000, 0.000000, 1.000000, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("l2esl2", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("l2esl4", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lgtlf", new AxisAlignedBB(0.000000, 0.000000, -1.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lgtlh", new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lrtlf", new AxisAlignedBB(-0.078125, 0.000000, -1.000000, 1.078125, 0.625000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("lrtlh", new AxisAlignedBB(-0.078125, 0.000000, 0.000000, 1.078125, 0.625000, 1.000000), 0));
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
    initTabBlock(new BlockBrightLightFactory("mclaclassicpostlight", new AxisAlignedBB(0.117188, 0.000000, 0.117188, 0.882813, 1.250000, 0.882813), 1));
    initTabBlock(new BlockBrightLightFactory("mclaparklight", new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 1.000000, 0.937500), 1));
    initTabBlock(new BlockBrightLightPoleColoredFactory("postlight1", new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 1.437500, 0.937500), 1));
    initTabBlock(new BlockBrightLightPoleColoredFactory("postlight2", new AxisAlignedBB(0.062500, 0.000000, 0.062500, 0.937500, 1.437500, 0.937500), 1));
    initTabBlock(new BlockBrightLightPoleColoredFactory("postlight3", new AxisAlignedBB(-0.125000, 0.000000, -0.125000, 1.125000, 1.437500, 1.125000), 1));
    initTabBlock(new BlockBrightLightFactory("slf", new AxisAlignedBB(0.421875, 0.875000, -1.000000, 0.578125, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("slh", new AxisAlignedBB(0.421875, 0.875000, 0.000000, 0.578125, 1.000000, 1.000000), 0));
    initTabBlock(new BlockBrightLightFactory("smsmx", new AxisAlignedBB(0.187500, -0.062500, 0.062500, 0.812500, 0.312500, 1.000000), 0));
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
    initTabBlock(new BlockBrightLightFactory("wsl", new AxisAlignedBB(0.437500, 0.000000, 0.875000, 0.562500, 1.000000, 1.000000), 0));
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
    initTabBlock(BlockOCPB.class, fmlPreInitializationEvent);
    initTabBlock(BlockOCPM.class, fmlPreInitializationEvent);
    initTabBlock(BlockOCPT.class, fmlPreInitializationEvent);
    initTabBlock(BlockPCRM.class, fmlPreInitializationEvent);
    initTabBlock(BlockRBM.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPB.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPB2.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPM.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPT.class, fmlPreInitializationEvent);
  }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.BlockAFEI;
import com.micatechnologies.minecraft.csm.powergrid.BlockAFEIS;
import com.micatechnologies.minecraft.csm.powergrid.BlockFGPHVSign;
import com.micatechnologies.minecraft.csm.powergrid.BlockFGPoleBottom;
import com.micatechnologies.minecraft.csm.powergrid.BlockFGPoleMiddle;
import com.micatechnologies.minecraft.csm.powergrid.BlockFGPoleTop;
import com.micatechnologies.minecraft.csm.powergrid.BlockMLUVMB1;
import com.micatechnologies.minecraft.csm.powergrid.BlockMLUVMB2;
import com.micatechnologies.minecraft.csm.powergrid.BlockMLUVMB3;
import com.micatechnologies.minecraft.csm.powergrid.BlockMLUVMB4;
import com.micatechnologies.minecraft.csm.powergrid.BlockMLUVMB5;
import com.micatechnologies.minecraft.csm.powergrid.BlockMPHVSign;
import com.micatechnologies.minecraft.csm.powergrid.BlockNewBrooksXArm1;
import com.micatechnologies.minecraft.csm.powergrid.BlockNewBrooksXArm2;
import com.micatechnologies.minecraft.csm.powergrid.BlockNewBrooksXArm3;
import com.micatechnologies.minecraft.csm.powergrid.BlockNewBrooksXArm4;
import com.micatechnologies.minecraft.csm.powergrid.BlockNewESBrooksXArm1;
import com.micatechnologies.minecraft.csm.powergrid.BlockNewESBrooksXArm2;
import com.micatechnologies.minecraft.csm.powergrid.BlockNewESBrooksXArm3;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldBrooksXArm1;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldBrooksXArm2;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldBrooksXArm3;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldBrooksXArm4;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldBrooksXArm5;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldBrooksXArm6;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldBrooksXArm7;
import com.micatechnologies.minecraft.csm.powergrid.BlockOldESBrooksXArm;
import com.micatechnologies.minecraft.csm.powergrid.BlockPCAB1;
import com.micatechnologies.minecraft.csm.powergrid.BlockPCAB2;
import com.micatechnologies.minecraft.csm.powergrid.BlockPCAB3;
import com.micatechnologies.minecraft.csm.powergrid.BlockPCAW1;
import com.micatechnologies.minecraft.csm.powergrid.BlockPCAW2;
import com.micatechnologies.minecraft.csm.powergrid.BlockPCAW3;
import com.micatechnologies.minecraft.csm.powergrid.BlockPoleHVSign;
import com.micatechnologies.minecraft.csm.powergrid.BlockPoleVisStrips;
import com.micatechnologies.minecraft.csm.powergrid.BlockPoleWireMount;
import com.micatechnologies.minecraft.csm.powergrid.BlockPullyMount;
import com.micatechnologies.minecraft.csm.powergrid.BlockSCELightMount;
import com.micatechnologies.minecraft.csm.powergrid.BlockSCELightMountSmall;
import com.micatechnologies.minecraft.csm.powergrid.BlockTEInsulatorCover;
import com.micatechnologies.minecraft.csm.powergrid.BlockTEInsulatorCoverDE;
import com.micatechnologies.minecraft.csm.powergrid.BlockTEPerchGuard;
import com.micatechnologies.minecraft.csm.powergrid.BlockTSC;
import com.micatechnologies.minecraft.csm.powergrid.BlockTransformerMount;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyProducer;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyToRedstone;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for power grid blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 6)
public class CsmTabPowerGrid extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabpowergrid";
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
    return CsmRegistry.getBlock("rftors");
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
   * Initializes all the items belonging to the tab.
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockAFEI.class, fmlPreInitializationEvent); // AFEI
    initTabBlock(BlockAFEIS.class, fmlPreInitializationEvent); // AFEIS
    initTabBlock(BlockFGPHVSign.class, fmlPreInitializationEvent); // FGPHVSign
    initTabBlock(BlockFGPoleBottom.class, fmlPreInitializationEvent); // FGPoleBottom
    initTabBlock(BlockFGPoleMiddle.class, fmlPreInitializationEvent); // FGPPoleMiddle
    initTabBlock(BlockFGPoleTop.class, fmlPreInitializationEvent); // FGPoleTop
    initTabBlock(BlockMLUVMB1.class, fmlPreInitializationEvent); // MLUVMB1
    initTabBlock(BlockMLUVMB2.class, fmlPreInitializationEvent); // MLUVMB2
    initTabBlock(BlockMLUVMB3.class, fmlPreInitializationEvent); // MLUVMB3
    initTabBlock(BlockMLUVMB4.class, fmlPreInitializationEvent); // MLUVMB4
    initTabBlock(BlockMLUVMB5.class, fmlPreInitializationEvent); // MLUVMB5
    initTabBlock(BlockMPHVSign.class, fmlPreInitializationEvent); // MPHVSign
    initTabBlock(BlockNewBrooksXArm1.class, fmlPreInitializationEvent); // NewBrooksXArm1
    initTabBlock(BlockNewBrooksXArm2.class, fmlPreInitializationEvent); // NewBrooksXArm2
    initTabBlock(BlockNewBrooksXArm3.class, fmlPreInitializationEvent); // NewBrooksXArm3
    initTabBlock(BlockNewBrooksXArm4.class, fmlPreInitializationEvent); // NewBrooksXArm4
    initTabBlock(BlockNewESBrooksXArm1.class, fmlPreInitializationEvent); // NewESBrooksXArm1
    initTabBlock(BlockNewESBrooksXArm2.class, fmlPreInitializationEvent); // NewESBrooksXArm2
    initTabBlock(BlockNewESBrooksXArm3.class, fmlPreInitializationEvent); // NewESBrooksXArm3
    initTabBlock(BlockOldBrooksXArm1.class, fmlPreInitializationEvent); // OldBrooksXArm1
    initTabBlock(BlockOldBrooksXArm2.class, fmlPreInitializationEvent); // OldBrooksXArm2
    initTabBlock(BlockOldBrooksXArm3.class, fmlPreInitializationEvent); // OldBrooksXArm3
    initTabBlock(BlockOldBrooksXArm4.class, fmlPreInitializationEvent); // OldBrooksXArm4
    initTabBlock(BlockOldBrooksXArm5.class, fmlPreInitializationEvent); // OldBrooksXArm5
    initTabBlock(BlockOldBrooksXArm6.class, fmlPreInitializationEvent); // OldBrooksXArm6
    initTabBlock(BlockOldBrooksXArm7.class, fmlPreInitializationEvent); // OldBrooksXArm7
    initTabBlock(BlockOldESBrooksXArm.class, fmlPreInitializationEvent); // OldESBrooksXArm
    initTabBlock(BlockPCAB1.class, fmlPreInitializationEvent); // PCAB1
    initTabBlock(BlockPCAB2.class, fmlPreInitializationEvent); // PCAB2
    initTabBlock(BlockPCAB3.class, fmlPreInitializationEvent); // PCAB3
    initTabBlock(BlockPCAW1.class, fmlPreInitializationEvent); // PCAW1
    initTabBlock(BlockPCAW2.class, fmlPreInitializationEvent); // PCAW2
    initTabBlock(BlockPCAW3.class, fmlPreInitializationEvent); // PCAW3
    initTabBlock(BlockPoleHVSign.class, fmlPreInitializationEvent); // PoleHVSign
    initTabBlock(BlockPoleVisStrips.class, fmlPreInitializationEvent); // PoleVisStrips
    initTabBlock(BlockPoleWireMount.class, fmlPreInitializationEvent); // PoleWireMount
    initTabBlock(BlockPullyMount.class, fmlPreInitializationEvent); // PullyMount
    initTabBlock(BlockSCELightMount.class, fmlPreInitializationEvent); // SCELightMount
    initTabBlock(BlockSCELightMountSmall.class, fmlPreInitializationEvent); // SCELightMountSmall
    initTabBlock(BlockTEInsulatorCover.class, fmlPreInitializationEvent); // TEInsulatorCover
    initTabBlock(BlockTEInsulatorCoverDE.class, fmlPreInitializationEvent); // TEInsulatorCoverDE
    initTabBlock(BlockTEPerchGuard.class, fmlPreInitializationEvent); // TEPerchGuard
    initTabBlock(BlockTransformerMount.class, fmlPreInitializationEvent); // TransformerMount
    initTabBlock(BlockTSC.class, fmlPreInitializationEvent); // TSC
    initTabBlock(BlockForgeEnergyProducer.class,
        fmlPreInitializationEvent); // Forge Energy Producer
    initTabBlock(BlockForgeEnergyToRedstone.class,
        fmlPreInitializationEvent); // Forge Energy to Redstone
  }
}

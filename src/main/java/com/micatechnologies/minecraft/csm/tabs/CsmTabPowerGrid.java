package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.*;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyProducer;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyToRedstone;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for power grid blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabPowerGrid extends CsmTab
{
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
        return CsmRegistry.getBlock( BlockForgeEnergyToRedstone.class );
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
    public void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent ) {
        initTabBlock( BlockAFEI.class, fmlPreInitializationEvent ); // AFEI
        initTabBlock( BlockAFEIS.class, fmlPreInitializationEvent ); // AFEIS
        initTabBlock( BlockFGPHVSign.class, fmlPreInitializationEvent ); // FGPHVSign
        initTabBlock( BlockFGPoleBottom.class, fmlPreInitializationEvent ); // FGPoleBottom
        initTabBlock( BlockFGPoleMiddle.class, fmlPreInitializationEvent ); // FGPPoleMiddle
        initTabBlock( BlockFGPoleTop.class, fmlPreInitializationEvent ); // FGPoleTop
        initTabBlock( BlockMLUVMB1.class, fmlPreInitializationEvent ); // MLUVMB1
        initTabBlock( BlockMLUVMB2.class, fmlPreInitializationEvent ); // MLUVMB2
        initTabBlock( BlockMLUVMB3.class, fmlPreInitializationEvent ); // MLUVMB3
        initTabBlock( BlockMLUVMB4.class, fmlPreInitializationEvent ); // MLUVMB4
        initTabBlock( BlockMLUVMB5.class, fmlPreInitializationEvent ); // MLUVMB5
        initTabBlock( BlockMPHVSign.class, fmlPreInitializationEvent ); // MPHVSign
        initTabBlock( BlockNewBrooksXArm1.class, fmlPreInitializationEvent ); // NewBrooksXArm1
        initTabBlock( BlockNewBrooksXArm2.class, fmlPreInitializationEvent ); // NewBrooksXArm2
        initTabBlock( BlockNewBrooksXArm3.class, fmlPreInitializationEvent ); // NewBrooksXArm3
        initTabBlock( BlockNewBrooksXArm4.class, fmlPreInitializationEvent ); // NewBrooksXArm4
        initTabBlock( BlockNewESBrooksXArm1.class, fmlPreInitializationEvent ); // NewESBrooksXArm1
        initTabBlock( BlockNewESBrooksXArm1.class, fmlPreInitializationEvent ); // NewESBrooksXArm2
        initTabBlock( BlockNewESBrooksXArm1.class, fmlPreInitializationEvent ); // NewESBrooksXArm1
        initTabBlock( BlockOldBrooksXArm1.class, fmlPreInitializationEvent ); // OldBrooksXArm1
        initTabBlock( BlockOldBrooksXArm2.class, fmlPreInitializationEvent ); // OldBrooksXArm2
        initTabBlock( BlockOldBrooksXArm3.class, fmlPreInitializationEvent ); // OldBrooksXArm3
        initTabBlock( BlockOldBrooksXArm4.class, fmlPreInitializationEvent ); // OldBrooksXArm4
        initTabBlock( BlockOldBrooksXArm5.class, fmlPreInitializationEvent ); // OldBrooksXArm5
        initTabBlock( BlockOldBrooksXArm6.class, fmlPreInitializationEvent ); // OldBrooksXArm6
        initTabBlock( BlockOldBrooksXArm7.class, fmlPreInitializationEvent ); // OldBrooksXArm7
        initTabBlock( BlockOldESBrooksXArm.class, fmlPreInitializationEvent ); // OldESBrooksXArm
        initTabBlock( BlockPCAB1.class, fmlPreInitializationEvent ); // PCAB1
        initTabBlock( BlockPCAB2.class, fmlPreInitializationEvent ); // PCAB2
        initTabBlock( BlockPCAB3.class, fmlPreInitializationEvent ); // PCAB3
        initTabBlock( BlockPCAW1.class, fmlPreInitializationEvent ); // PCAW1
        initTabBlock( BlockPCAW2.class, fmlPreInitializationEvent ); // PCAW2
        initTabBlock( BlockPCAW3.class, fmlPreInitializationEvent ); // PCAW3
        initTabBlock( BlockPoleHVSign.class, fmlPreInitializationEvent ); // PoleHVSign
        initTabBlock( BlockPoleVisStrips.class, fmlPreInitializationEvent ); // PoleVisStrips
        initTabBlock( BlockPoleWireMount.class, fmlPreInitializationEvent ); // PoleWireMount
        initTabBlock( BlockPullyMount.class, fmlPreInitializationEvent ); // PullyMount
        initTabBlock( BlockSCELightMount.class, fmlPreInitializationEvent ); // SCELightMount
        initTabBlock( BlockSCELightMountSmall.class, fmlPreInitializationEvent ); // SCELightMountSmall
        initTabBlock( BlockTEInsulatorCover.class, fmlPreInitializationEvent ); // TEInsulatorCover
        initTabBlock( BlockTEInsulatorCoverDE.class, fmlPreInitializationEvent ); // TEInsulatorCoverDE
        initTabBlock( BlockTEPerchGuard.class, fmlPreInitializationEvent ); // TEPerchGuard
        initTabBlock( BlockTransformerMount.class, fmlPreInitializationEvent ); // TransformerMount
        initTabBlock( BlockTEInsulatorCover.class, fmlPreInitializationEvent ); // TSC
        initTabBlock( BlockForgeEnergyProducer.class, fmlPreInitializationEvent ); // Forge Energy Producer
        initTabBlock( BlockForgeEnergyToRedstone.class, fmlPreInitializationEvent ); // Forge Energy to Redstone
    }
}

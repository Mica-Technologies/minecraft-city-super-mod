package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockPCC;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.BlockART1;
import com.micatechnologies.minecraft.csm.hvac.BlockSV4;
import com.micatechnologies.minecraft.csm.lighting.*;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for lighting blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabLighting extends CsmTab
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
        return CsmRegistry.getBlock( "altomvul" );
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
     * Initializes all the items belonging to the tab.
     *
     * @since 1.0
     */
    @Override
    public void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent ) {
        initTabBlock( BlockAE115.class, fmlPreInitializationEvent ); // AE115
        initTabBlock( BlockAE115CU.class, fmlPreInitializationEvent ); // AE115CU
        initTabBlock( BlockAEATB0.class, fmlPreInitializationEvent ); // AEATB0
        initTabBlock( BlockAEATB2.class, fmlPreInitializationEvent ); // AEATB2
        initTabBlock( BlockAltoLLM.class, fmlPreInitializationEvent ); // AltoLLM
        initTabBlock( BlockAltoMVUL.class, fmlPreInitializationEvent ); // AltoMVUL
        initTabBlock( BlockAltoMVWL.class, fmlPreInitializationEvent ); // AltoMVWL
        initTabBlock( BlockAltoMVWLSlim.class, fmlPreInitializationEvent ); // AltoMVWLSlim
        initTabBlock( BlockAltoReLL.class, fmlPreInitializationEvent ); // AltoReLL
        initTabBlock( BlockAltoRLL.class, fmlPreInitializationEvent ); // AltoRLL
        initTabBlock( BlockAltoSQLL.class, fmlPreInitializationEvent ); // AltoSQLL
        initTabBlock( BlockCEHalo.class, fmlPreInitializationEvent ); // CEHalo
        initTabBlock( BlockCESquare.class, fmlPreInitializationEvent ); // CESquare
        initTabBlock( BlockCINavionAlt.class, fmlPreInitializationEvent ); // CINavionAlt
        initTabBlock( BlockCreeLEDway.class, fmlPreInitializationEvent ); // CreeLEDway
        initTabBlock( BlockCreeledwaysmall.class, fmlPreInitializationEvent ); // Creeledwaysmall
        initTabBlock( BlockCreeStylusFull.class, fmlPreInitializationEvent ); // CreeStylusFull
        initTabBlock( BlockCreeStylusHalf.class, fmlPreInitializationEvent ); // CreeStylusHalf
        initTabBlock( BlockCreeXSP.class, fmlPreInitializationEvent ); // CreeXSP
        initTabBlock( BlockDSLF.class, fmlPreInitializationEvent ); // DSLF
        initTabBlock( BlockDSLFR.class, fmlPreInitializationEvent ); // DSLFR
        initTabBlock( BlockDSLH.class, fmlPreInitializationEvent ); // DSLH
        initTabBlock( BlockDSLHR.class, fmlPreInitializationEvent ); // DSLHR
        initTabBlock( BlockFBM.class, fmlPreInitializationEvent ); // FBM
        initTabBlock( BlockFBM2.class, fmlPreInitializationEvent ); // FBM2
        initTabBlock( BlockGEELPO.class, fmlPreInitializationEvent ); // GEELPO
        initTabBlock( BlockGEELSN.class, fmlPreInitializationEvent ); // GEELSN
        initTabBlock( BlockGEES.class, fmlPreInitializationEvent ); // GEES
        initTabBlock( BlockGEESPO.class, fmlPreInitializationEvent ); // GEESPO
        initTabBlock( BlockGEESSN.class, fmlPreInitializationEvent ); // GEESSN
        initTabBlock( BlockGEForm109.class, fmlPreInitializationEvent ); // GEForm109
        initTabBlock( BlockGEForm109TD.class, fmlPreInitializationEvent ); // GEForm109TD
        initTabBlock( BlockGEm240r1.class, fmlPreInitializationEvent ); // GEm240r1
        initTabBlock( BlockGEm240r1np.class, fmlPreInitializationEvent ); // GEm240r1np
        initTabBlock( BlockGEm240r2.class, fmlPreInitializationEvent ); // GEm240r2
        initTabBlock( BlockGEm240r2cu.class, fmlPreInitializationEvent ); // GEm240r2cu
        initTabBlock( BlockGEM400r3cu.class, fmlPreInitializationEvent ); // GEM400r3cu
        initTabBlock( BlockGEM400r3cunp.class, fmlPreInitializationEvent ); // GEM400r3cunp
        initTabBlock( BlockGEPB.class, fmlPreInitializationEvent ); // GEPB
        initTabBlock( BlockHB1.class, fmlPreInitializationEvent ); // HB1
        initTabBlock( BlockHB2.class, fmlPreInitializationEvent ); // HB2
        initTabBlock( BlockHBM.class, fmlPreInitializationEvent ); // HBM
        initTabBlock( BlockHBM2.class, fmlPreInitializationEvent ); // HBM2
        initTabBlock( BlockHighbayMount.class, fmlPreInitializationEvent ); // HighbayMount
        initTabBlock( BlockL2ESL2.class, fmlPreInitializationEvent ); // L2ESL2
        initTabBlock( BlockL2ESL4.class, fmlPreInitializationEvent ); // L2ESL4
        initTabBlock( BlockLGTLF.class, fmlPreInitializationEvent ); // LGTLF
        initTabBlock( BlockLRTLF.class, fmlPreInitializationEvent ); // LRTLF
        initTabBlock( BlockLRTLH.class, fmlPreInitializationEvent ); // LRTLH
        initTabBlock( BlockLtec.class, fmlPreInitializationEvent ); // Ltec
        initTabBlock( BlockLTECDTD.class, fmlPreInitializationEvent ); // LTECDTD
        initTabBlock( BlockLtgc1v1.class, fmlPreInitializationEvent ); // Ltgc1v1
        initTabBlock( BlockLTGC1v1np.class, fmlPreInitializationEvent ); // LTGC1v1np
        initTabBlock( BlockLtgc1v2.class, fmlPreInitializationEvent ); // Ltgc1v2
        initTabBlock( BlockLTGCJ.class, fmlPreInitializationEvent ); // LTGCJ
        initTabBlock( BlockLTGCJSN.class, fmlPreInitializationEvent ); // LTGCJSN
        initTabBlock( BlockLTGCL.class, fmlPreInitializationEvent ); // LTGCL
        initTabBlock( BlockLtgcm.class, fmlPreInitializationEvent ); // Ltgcm
        initTabBlock( BlockLTGCMSN.class, fmlPreInitializationEvent ); // LTGCMSN
        initTabBlock( BlockLTGCMv2np.class, fmlPreInitializationEvent ); // LTGCMv2np
        initTabBlock( BlockMCLAClassicPostLight.class, fmlPreInitializationEvent ); // MCLAClassicPostLight
        initTabBlock( BlockMCLAParkLight.class, fmlPreInitializationEvent ); // MCLAParkLight
        initTabBlock( BlockNOVTM.class, fmlPreInitializationEvent ); // NOVTM
        initTabBlock( BlockNOVTM2.class, fmlPreInitializationEvent ); // NOVTM2
        initTabBlock( BlockNOVTM3.class, fmlPreInitializationEvent ); // NOVTM3
        initTabBlock( BlockNOVTM4.class, fmlPreInitializationEvent ); // NOVTM4
        initTabBlock( BlockNOVTM5.class, fmlPreInitializationEvent ); // NOVTM5
        initTabBlock( BlockOCPB.class, fmlPreInitializationEvent ); // OCPB
        initTabBlock( BlockOCPM.class, fmlPreInitializationEvent ); // OCPM
        initTabBlock( BlockOCPT.class, fmlPreInitializationEvent ); // OCPT
        initTabBlock( BlockRBM.class, fmlPreInitializationEvent ); // RBM
        initTabBlock( BlockRCPB.class, fmlPreInitializationEvent ); // RCPB
        initTabBlock( BlockRCPB2.class, fmlPreInitializationEvent ); // RCPB2
        initTabBlock( BlockRCPM.class, fmlPreInitializationEvent ); // RCPM
        initTabBlock( BlockRCPT.class, fmlPreInitializationEvent ); // RCPT
        initTabBlock( BlockSearsLF.class, fmlPreInitializationEvent ); // SearsLF
        initTabBlock( BlockSearsLF2.class, fmlPreInitializationEvent ); // SearsLF2
        initTabBlock( BlockSearsLF3.class, fmlPreInitializationEvent ); // SearsLF3
        initTabBlock( BlockSearsLF4.class, fmlPreInitializationEvent ); // SearsLF4
        initTabBlock( BlockSLF.class, fmlPreInitializationEvent ); // SLF
        initTabBlock( BlockSLH.class, fmlPreInitializationEvent ); // SLH
        initTabBlock( BlockSMSMX.class, fmlPreInitializationEvent ); // SMSMX
        initTabBlock( BlockSSLFR.class, fmlPreInitializationEvent ); // SSLFR
        initTabBlock( BlockSSLH.class, fmlPreInitializationEvent ); // SSLH
        initTabBlock( BlockSSLHR.class, fmlPreInitializationEvent ); // SSLHR
        initTabBlock( BlockULEDF.class, fmlPreInitializationEvent ); // ULEDF
        initTabBlock( BlockULEDH.class, fmlPreInitializationEvent ); // ULEDH
        initTabBlock( BlockWHOV20.class, fmlPreInitializationEvent ); // WHOV20
        initTabBlock( BlockWHOV50.class, fmlPreInitializationEvent ); // WHOV50
        initTabBlock( BlockWHOV50Finned.class, fmlPreInitializationEvent ); // WHOV50Finned
        initTabBlock( BlockWHOV50NP.class, fmlPreInitializationEvent ); // WHOV50NP
        initTabBlock( BlockWHOV50Shaded.class, fmlPreInitializationEvent ); // WHOV50Shaded
        initTabBlock( BlockWHOV50ShadedNP.class, fmlPreInitializationEvent ); // WHOV50ShadedNP
        initTabBlock( BlockWSL.class, fmlPreInitializationEvent ); // WSL
    }
}

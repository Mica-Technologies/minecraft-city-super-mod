package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lighting.BlockAE115;
import com.micatechnologies.minecraft.csm.lighting.BlockAE115CU;
import com.micatechnologies.minecraft.csm.lighting.BlockAEATB0;
import com.micatechnologies.minecraft.csm.lighting.BlockAEATB2;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoLLM;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoMVUL;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoMVWL;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoMVWLSlim;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoRLL;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoReLL;
import com.micatechnologies.minecraft.csm.lighting.BlockAltoSQLL;
import com.micatechnologies.minecraft.csm.lighting.BlockCEHalo;
import com.micatechnologies.minecraft.csm.lighting.BlockCESquare;
import com.micatechnologies.minecraft.csm.lighting.BlockCINavion;
import com.micatechnologies.minecraft.csm.lighting.BlockCINavionAlt;
import com.micatechnologies.minecraft.csm.lighting.BlockCreeLEDway;
import com.micatechnologies.minecraft.csm.lighting.BlockCreeStylusFull;
import com.micatechnologies.minecraft.csm.lighting.BlockCreeStylusHalf;
import com.micatechnologies.minecraft.csm.lighting.BlockCreeXSP;
import com.micatechnologies.minecraft.csm.lighting.BlockCreeledwaysmall;
import com.micatechnologies.minecraft.csm.lighting.BlockDSLF;
import com.micatechnologies.minecraft.csm.lighting.BlockDSLFR;
import com.micatechnologies.minecraft.csm.lighting.BlockDSLH;
import com.micatechnologies.minecraft.csm.lighting.BlockDSLHR;
import com.micatechnologies.minecraft.csm.lighting.BlockFBM;
import com.micatechnologies.minecraft.csm.lighting.BlockFBM2;
import com.micatechnologies.minecraft.csm.lighting.BlockGEEL;
import com.micatechnologies.minecraft.csm.lighting.BlockGEELPO;
import com.micatechnologies.minecraft.csm.lighting.BlockGEELSN;
import com.micatechnologies.minecraft.csm.lighting.BlockGEES;
import com.micatechnologies.minecraft.csm.lighting.BlockGEESPO;
import com.micatechnologies.minecraft.csm.lighting.BlockGEESSN;
import com.micatechnologies.minecraft.csm.lighting.BlockGEForm109;
import com.micatechnologies.minecraft.csm.lighting.BlockGEForm109TD;
import com.micatechnologies.minecraft.csm.lighting.BlockGEM400r3;
import com.micatechnologies.minecraft.csm.lighting.BlockGEM400r3cu;
import com.micatechnologies.minecraft.csm.lighting.BlockGEM400r3cunp;
import com.micatechnologies.minecraft.csm.lighting.BlockGEPB;
import com.micatechnologies.minecraft.csm.lighting.BlockGEm240r1;
import com.micatechnologies.minecraft.csm.lighting.BlockGEm240r1np;
import com.micatechnologies.minecraft.csm.lighting.BlockGEm240r2;
import com.micatechnologies.minecraft.csm.lighting.BlockGEm240r2cu;
import com.micatechnologies.minecraft.csm.lighting.BlockHB1;
import com.micatechnologies.minecraft.csm.lighting.BlockHB2;
import com.micatechnologies.minecraft.csm.lighting.BlockHBM;
import com.micatechnologies.minecraft.csm.lighting.BlockHBM2;
import com.micatechnologies.minecraft.csm.lighting.BlockHighbayMount;
import com.micatechnologies.minecraft.csm.lighting.BlockL2ESL2;
import com.micatechnologies.minecraft.csm.lighting.BlockL2ESL4;
import com.micatechnologies.minecraft.csm.lighting.BlockLGTLF;
import com.micatechnologies.minecraft.csm.lighting.BlockLGTLH;
import com.micatechnologies.minecraft.csm.lighting.BlockLRTLF;
import com.micatechnologies.minecraft.csm.lighting.BlockLRTLH;
import com.micatechnologies.minecraft.csm.lighting.BlockLTECDTD;
import com.micatechnologies.minecraft.csm.lighting.BlockLTGC1v1np;
import com.micatechnologies.minecraft.csm.lighting.BlockLTGCJ;
import com.micatechnologies.minecraft.csm.lighting.BlockLTGCJSN;
import com.micatechnologies.minecraft.csm.lighting.BlockLTGCL;
import com.micatechnologies.minecraft.csm.lighting.BlockLTGCMSN;
import com.micatechnologies.minecraft.csm.lighting.BlockLTGCMv2;
import com.micatechnologies.minecraft.csm.lighting.BlockLTGCMv2np;
import com.micatechnologies.minecraft.csm.lighting.BlockLtec;
import com.micatechnologies.minecraft.csm.lighting.BlockLtgc1v1;
import com.micatechnologies.minecraft.csm.lighting.BlockLtgc1v2;
import com.micatechnologies.minecraft.csm.lighting.BlockLtgcm;
import com.micatechnologies.minecraft.csm.lighting.BlockMCLAClassicPostLight;
import com.micatechnologies.minecraft.csm.lighting.BlockMCLAParkLight;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM2;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM3;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM4;
import com.micatechnologies.minecraft.csm.lighting.BlockNOVTM5;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPT;
import com.micatechnologies.minecraft.csm.lighting.BlockPCRM;
import com.micatechnologies.minecraft.csm.lighting.BlockPostLight1;
import com.micatechnologies.minecraft.csm.lighting.BlockPostLight2;
import com.micatechnologies.minecraft.csm.lighting.BlockPostLight3;
import com.micatechnologies.minecraft.csm.lighting.BlockRBM;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB2;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPT;
import com.micatechnologies.minecraft.csm.lighting.BlockSLF;
import com.micatechnologies.minecraft.csm.lighting.BlockSLH;
import com.micatechnologies.minecraft.csm.lighting.BlockSMSMX;
import com.micatechnologies.minecraft.csm.lighting.BlockSSLF;
import com.micatechnologies.minecraft.csm.lighting.BlockSSLFR;
import com.micatechnologies.minecraft.csm.lighting.BlockSSLH;
import com.micatechnologies.minecraft.csm.lighting.BlockSSLHR;
import com.micatechnologies.minecraft.csm.lighting.BlockSearsLF;
import com.micatechnologies.minecraft.csm.lighting.BlockSearsLF2;
import com.micatechnologies.minecraft.csm.lighting.BlockSearsLF3;
import com.micatechnologies.minecraft.csm.lighting.BlockSearsLF4;
import com.micatechnologies.minecraft.csm.lighting.BlockULEDF;
import com.micatechnologies.minecraft.csm.lighting.BlockULEDH;
import com.micatechnologies.minecraft.csm.lighting.BlockWHOV20;
import com.micatechnologies.minecraft.csm.lighting.BlockWHOV50;
import com.micatechnologies.minecraft.csm.lighting.BlockWHOV50Finned;
import com.micatechnologies.minecraft.csm.lighting.BlockWHOV50NP;
import com.micatechnologies.minecraft.csm.lighting.BlockWHOV50Shaded;
import com.micatechnologies.minecraft.csm.lighting.BlockWHOV50ShadedNP;
import com.micatechnologies.minecraft.csm.lighting.BlockWSL;
import net.minecraft.block.Block;
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
   * Initializes all the items belonging to the tab.
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockAE115.class, fmlPreInitializationEvent); // AE115
    initTabBlock(BlockAE115CU.class, fmlPreInitializationEvent); // AE115CU
    initTabBlock(BlockAEATB0.class, fmlPreInitializationEvent); // AEATB0
    initTabBlock(BlockAEATB2.class, fmlPreInitializationEvent); // AEATB2
    initTabBlock(BlockAltoLLM.class, fmlPreInitializationEvent); // AltoLLM
    initTabBlock(BlockAltoMVUL.class, fmlPreInitializationEvent); // AltoMVUL
    initTabBlock(BlockAltoMVWL.class, fmlPreInitializationEvent); // AltoMVWL
    initTabBlock(BlockAltoMVWLSlim.class, fmlPreInitializationEvent); // AltoMVWLSlim
    initTabBlock(BlockAltoRLL.class, fmlPreInitializationEvent); // AltoRLL
    initTabBlock(BlockAltoReLL.class, fmlPreInitializationEvent); // AltoReLL
    initTabBlock(BlockAltoSQLL.class, fmlPreInitializationEvent); // AltoSQLL
    initTabBlock(BlockCEHalo.class, fmlPreInitializationEvent); // CEHalo
    initTabBlock(BlockCESquare.class, fmlPreInitializationEvent); // CESquare
    initTabBlock(BlockCINavion.class, fmlPreInitializationEvent); // CINavion
    initTabBlock(BlockCINavionAlt.class, fmlPreInitializationEvent); // CINavionAlt
    initTabBlock(BlockCreeLEDway.class, fmlPreInitializationEvent); // CreeLEDway
    initTabBlock(BlockCreeStylusFull.class, fmlPreInitializationEvent); // CreeStylusFull
    initTabBlock(BlockCreeStylusHalf.class, fmlPreInitializationEvent); // CreeStylusHalf
    initTabBlock(BlockCreeXSP.class, fmlPreInitializationEvent); // CreeXSP
    initTabBlock(BlockCreeledwaysmall.class, fmlPreInitializationEvent); // Creeledwaysmall
    initTabBlock(BlockDSLF.class, fmlPreInitializationEvent); // DSLF
    initTabBlock(BlockDSLFR.class, fmlPreInitializationEvent); // DSLFR
    initTabBlock(BlockDSLH.class, fmlPreInitializationEvent); // DSLH
    initTabBlock(BlockDSLHR.class, fmlPreInitializationEvent); // DSLHR
    initTabBlock(BlockFBM.class, fmlPreInitializationEvent); // FBM
    initTabBlock(BlockFBM2.class, fmlPreInitializationEvent); // FBM2
    initTabBlock(BlockGEEL.class, fmlPreInitializationEvent); // GEEL
    initTabBlock(BlockGEELPO.class, fmlPreInitializationEvent); // GEELPO
    initTabBlock(BlockGEELSN.class, fmlPreInitializationEvent); // GEELSN
    initTabBlock(BlockGEES.class, fmlPreInitializationEvent); // GEES
    initTabBlock(BlockGEESPO.class, fmlPreInitializationEvent); // GEESPO
    initTabBlock(BlockGEESSN.class, fmlPreInitializationEvent); // GEESSN
    initTabBlock(BlockGEForm109.class, fmlPreInitializationEvent); // GEForm109
    initTabBlock(BlockGEForm109TD.class, fmlPreInitializationEvent); // GEForm109TD
    initTabBlock(BlockGEM400r3.class, fmlPreInitializationEvent); // GEM400r3
    initTabBlock(BlockGEM400r3cu.class, fmlPreInitializationEvent); // GEM400r3cu
    initTabBlock(BlockGEM400r3cunp.class, fmlPreInitializationEvent); // GEM400r3cunp
    initTabBlock(BlockGEPB.class, fmlPreInitializationEvent); // GEPB
    initTabBlock(BlockGEm240r1.class, fmlPreInitializationEvent); // GEm240r1
    initTabBlock(BlockGEm240r1np.class, fmlPreInitializationEvent); // GEm240r1np
    initTabBlock(BlockGEm240r2.class, fmlPreInitializationEvent); // GEm240r2
    initTabBlock(BlockGEm240r2cu.class, fmlPreInitializationEvent); // GEm240r2cu
    initTabBlock(BlockHB1.class, fmlPreInitializationEvent); // HB1
    initTabBlock(BlockHB2.class, fmlPreInitializationEvent); // HB2
    initTabBlock(BlockHBM.class, fmlPreInitializationEvent); // HBM
    initTabBlock(BlockHBM2.class, fmlPreInitializationEvent); // HBM2
    initTabBlock(BlockHighbayMount.class, fmlPreInitializationEvent); // HighbayMount
    initTabBlock(BlockL2ESL2.class, fmlPreInitializationEvent); // L2ESL2
    initTabBlock(BlockL2ESL4.class, fmlPreInitializationEvent); // L2ESL4
    initTabBlock(BlockLGTLF.class, fmlPreInitializationEvent); // LGTLF
    initTabBlock(BlockLGTLH.class, fmlPreInitializationEvent); // LGTLH
    initTabBlock(BlockLRTLF.class, fmlPreInitializationEvent); // LRTLF
    initTabBlock(BlockLRTLH.class, fmlPreInitializationEvent); // LRTLH
    initTabBlock(BlockLTECDTD.class, fmlPreInitializationEvent); // LTECDTD
    initTabBlock(BlockLTGC1v1np.class, fmlPreInitializationEvent); // LTGC1v1np
    initTabBlock(BlockLTGCJ.class, fmlPreInitializationEvent); // LTGCJ
    initTabBlock(BlockLTGCJSN.class, fmlPreInitializationEvent); // LTGCJSN
    initTabBlock(BlockLTGCL.class, fmlPreInitializationEvent); // LTGCL
    initTabBlock(BlockLTGCMSN.class, fmlPreInitializationEvent); // LTGCMSN
    initTabBlock(BlockLTGCMv2.class, fmlPreInitializationEvent); // LTGCMv2
    initTabBlock(BlockLTGCMv2np.class, fmlPreInitializationEvent); // LTGCMv2np
    initTabBlock(BlockLtec.class, fmlPreInitializationEvent); // Ltec
    initTabBlock(BlockLtgc1v1.class, fmlPreInitializationEvent); // Ltgc1v1
    initTabBlock(BlockLtgc1v2.class, fmlPreInitializationEvent); // Ltgc1v2
    initTabBlock(BlockLtgcm.class, fmlPreInitializationEvent); // Ltgcm
    initTabBlock(BlockMCLAClassicPostLight.class,
        fmlPreInitializationEvent); // MCLAClassicPostLight
    initTabBlock(BlockMCLAParkLight.class, fmlPreInitializationEvent); // MCLAParkLight
    initTabBlock(BlockNOVTM.class, fmlPreInitializationEvent); // NOVTM
    initTabBlock(BlockNOVTM2.class, fmlPreInitializationEvent); // NOVTM2
    initTabBlock(BlockNOVTM3.class, fmlPreInitializationEvent); // NOVTM3
    initTabBlock(BlockNOVTM4.class, fmlPreInitializationEvent); // NOVTM4
    initTabBlock(BlockNOVTM5.class, fmlPreInitializationEvent); // NOVTM5
    initTabBlock(BlockOCPB.class, fmlPreInitializationEvent); // OCPB
    initTabBlock(BlockOCPM.class, fmlPreInitializationEvent); // OCPM
    initTabBlock(BlockOCPT.class, fmlPreInitializationEvent); // OCPT
    initTabBlock(BlockPCRM.class, fmlPreInitializationEvent); // PCRM
    initTabBlock(BlockRBM.class, fmlPreInitializationEvent); // RBM
    initTabBlock(BlockRCPB.class, fmlPreInitializationEvent); // RCPB
    initTabBlock(BlockRCPB2.class, fmlPreInitializationEvent); // RCPB2
    initTabBlock(BlockRCPM.class, fmlPreInitializationEvent); // RCPM
    initTabBlock(BlockRCPT.class, fmlPreInitializationEvent); // RCPT
    initTabBlock(BlockSLF.class, fmlPreInitializationEvent); // SLF
    initTabBlock(BlockSLH.class, fmlPreInitializationEvent); // SLH
    initTabBlock(BlockSMSMX.class, fmlPreInitializationEvent); // SMSMX
    initTabBlock(BlockSSLF.class, fmlPreInitializationEvent); // SSLF
    initTabBlock(BlockSSLFR.class, fmlPreInitializationEvent); // SSLFR
    initTabBlock(BlockSSLH.class, fmlPreInitializationEvent); // SSLH
    initTabBlock(BlockSSLHR.class, fmlPreInitializationEvent); // SSLHR
    initTabBlock(BlockSearsLF.class, fmlPreInitializationEvent); // SearsLF
    initTabBlock(BlockSearsLF2.class, fmlPreInitializationEvent); // SearsLF2
    initTabBlock(BlockSearsLF3.class, fmlPreInitializationEvent); // SearsLF3
    initTabBlock(BlockSearsLF4.class, fmlPreInitializationEvent); // SearsLF4
    initTabBlock(BlockULEDF.class, fmlPreInitializationEvent); // ULEDF
    initTabBlock(BlockULEDH.class, fmlPreInitializationEvent); // ULEDH
    initTabBlock(BlockWHOV20.class, fmlPreInitializationEvent); // WHOV20
    initTabBlock(BlockWHOV50.class, fmlPreInitializationEvent); // WHOV50
    initTabBlock(BlockWHOV50Finned.class, fmlPreInitializationEvent); // WHOV50Finned
    initTabBlock(BlockWHOV50NP.class, fmlPreInitializationEvent); // WHOV50NP
    initTabBlock(BlockWHOV50Shaded.class, fmlPreInitializationEvent); // WHOV50Shaded
    initTabBlock(BlockWHOV50ShadedNP.class, fmlPreInitializationEvent); // WHOV50ShadedNP
    initTabBlock(BlockWSL.class, fmlPreInitializationEvent); // WSL
    initTabBlock(BlockPostLight1.class, fmlPreInitializationEvent); // PostLight1
    initTabBlock(BlockPostLight2.class, fmlPreInitializationEvent); // PostLight2
    initTabBlock(BlockPostLight3.class, fmlPreInitializationEvent); // PostLight3
  }
}

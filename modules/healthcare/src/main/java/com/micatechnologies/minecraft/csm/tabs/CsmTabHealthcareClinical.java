package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.nursecall.BlockNurseCallMasterPanel;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Healthcare module's clinical tab: nursing (the nurse call master panel, infection control,
 * linen), and later the emergency department, surgery support, imaging, the lab, pharmacy, the
 * morgue and the building systems.
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 30)
public class CsmTabHealthcareClinical extends CsmTab {

  @Override
  public String getTabId() {
    return "tabhealthcareclinical";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("nurse_call_master_panel");
    return icon != null ? icon : Blocks.QUARTZ_BLOCK;
  }

  @Override
  public boolean getTabSearchable() {
    return false;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // --- gen_healthcare_wards.py --fragments ---
    initTabBlock(new BlockNurseCallMasterPanel());
    initTabBlock(new BlockHealthcare("sharps_container", new int[]{4, 3, 11, 12, 13, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("glove_box_dispenser", new int[]{1, 4, 12, 15, 12, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("hand_sanitizer_dispenser", new int[]{5, 3, 12, 11, 13, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("ppe_isolation_cart", new int[]{2, 0, 3, 14, 14, 13}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("biohazard_bin", new int[]{4, 0, 4, 12, 11, 12}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("soiled_linen_hamper", new int[]{3, 0, 3, 13, 14, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("clean_linen_cart", new int[]{1, 0, 3, 15, 16, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("medical_gas_outlet_o2", new int[]{5, 5, 14, 11, 11, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("medical_gas_zone_sign", new int[]{1, 4, 15, 15, 12, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
  }
}

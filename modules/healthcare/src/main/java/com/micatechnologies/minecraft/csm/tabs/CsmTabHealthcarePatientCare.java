package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Healthcare module's patient-facing tab: patient rooms and wards, and later maternity,
 * rehabilitation, clinics, waiting areas and wayfinding.
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 29)
public class CsmTabHealthcarePatientCare extends CsmTab {

  @Override
  public String getTabId() {
    return "tabhealthcarepatientcare";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("hospital_bed_blue");
    return icon != null ? icon : Blocks.WOOL;
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
  }
}

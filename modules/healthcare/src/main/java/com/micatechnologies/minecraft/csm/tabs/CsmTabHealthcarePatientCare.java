package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareLight;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcarePair;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareScreen;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.nursecall.BlockNurseCallDevice;
import com.micatechnologies.minecraft.csm.healthcare.nursecall.BlockNurseCallDomeLight;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockCorridorRail;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockCurtainTrack;
import com.micatechnologies.minecraft.csm.healthcare.wards.BlockHospitalBed;
import com.micatechnologies.minecraft.csm.healthcare.wards.HospitalBedVariant;
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
    // --- gen_healthcare_wards.py --fragments ---
    initTabBlock(new BlockHospitalBed(HospitalBedVariant.WHITE));
    initTabBlock(new BlockHospitalBed(HospitalBedVariant.BLUE));
    initTabBlock(new BlockHospitalBed(HospitalBedVariant.GREEN));
    initTabBlock(new BlockHospitalBed(HospitalBedVariant.MAUVE));
    initTabBlock(new BlockHospitalBed(HospitalBedVariant.ICU));
    initTabBlock(new BlockHospitalBed(HospitalBedVariant.BARIATRIC));
    initTabBlock(new BlockHealthcarePair("patient_headwall", new int[]{0, 0, 13, 16, 16, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, BlockHealthcarePair.Shape.WIDE));
    initTabBlock(new BlockHealthcarePair("patient_headwall_upper", new int[]{0, 0, 14, 16, 16, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, BlockHealthcarePair.Shape.WIDE));
    initTabBlock(new BlockHealthcareLight("patient_reading_light", new int[]{5, 8, 6, 11, 14, 16}, 12));
    initTabBlock(new BlockHealthcareScreen("patient_monitor_wall", new int[]{2, 4, 6, 14, 14, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcareScreen("patient_monitor_stand", new int[]{3, 0, 3, 13, 16, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("iv_pole", new int[]{6, 0, 6, 10, 16, 10}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcareScreen("iv_pole_pump", new int[]{5, 0, 5, 11, 16, 11}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("overbed_table", new int[]{0, 0, 2, 16, 13, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("patient_recliner", new int[]{1, 0, 1, 15, 16, 16}, HealthcareMaterial.CLOTH, BlockHealthcare.Mount.FLOOR, true).withSeat(8, 1));
    initTabBlock(new BlockHealthcare("visitor_chair", new int[]{2, 0, 2, 14, 16, 15}, HealthcareMaterial.WOOD, BlockHealthcare.Mount.FLOOR, true).withSeat(8.5, 0.5));
    initTabBlock(new BlockHealthcarePair("patient_wardrobe", new int[]{1, 0, 2, 15, 16, 16}, HealthcareMaterial.WOOD, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcareScreen("patient_room_tv", new int[]{1, 3, 8, 15, 13, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcareScreen("vitals_wall_unit", new int[]{3, 2, 11, 13, 14, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("suction_regulator", new int[]{4, 1, 10, 12, 15, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockNurseCallDevice("nurse_call_button", new int[]{4, 4, 14, 12, 12, 16}));
    initTabBlock(new BlockNurseCallDevice("nurse_call_pillow_speaker", new int[]{6, 3, 13, 10, 13, 16}));
    initTabBlock(new BlockNurseCallDevice("nurse_call_pull_cord", new int[]{5, 0, 14, 11, 16, 16}));
    initTabBlock(new BlockNurseCallDomeLight("nurse_call_dome_light", new int[]{4, 5, 10, 12, 11, 16}));
    initTabBlock(new BlockCurtainTrack());
    initTabBlock(new BlockCorridorRail("corridor_handrail", new int[]{0, 13, 12, 16, 16, 16}));
    initTabBlock(new BlockCorridorRail("corridor_crash_rail", new int[]{0, 12, 12, 16, 16, 16}));
    // --- gen_healthcare_clinical.py --fragments ---
    initTabBlock(new BlockHealthcare("wheelchair", new int[]{2, 0, 1, 14, 16, 15}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true).withSeat(7.2, 0));
    initTabBlock(new BlockHealthcare("wheelchair_storage_rack", new int[]{0, 0, 1, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("wheelchair_corral_sign", new int[]{1, 3, 15, 15, 13, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("walker", new int[]{2, 0, 3, 14, 15, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcarePair("patient_lift_mobile", new int[]{2, 0, 0, 14, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcare("ceiling_patient_lift", new int[]{0, 0, 0, 16, 1, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
  }
}

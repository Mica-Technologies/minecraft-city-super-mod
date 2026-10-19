package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareLight;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcarePair;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareScreen;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareUnit;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.clinical.BlockCeilingHead;
import com.micatechnologies.minecraft.csm.healthcare.clinical.BlockCeilingHeadLight;
import com.micatechnologies.minecraft.csm.healthcare.clinical.BlockEdTrackingBoard;
import com.micatechnologies.minecraft.csm.healthcare.clinical.BlockNurseStation;
import com.micatechnologies.minecraft.csm.healthcare.codeblue.BlockCodeBlueAnnunciator;
import com.micatechnologies.minecraft.csm.healthcare.codeblue.BlockCodeBlueButton;
import com.micatechnologies.minecraft.csm.healthcare.diagnostics.BlockLabBench;
import com.micatechnologies.minecraft.csm.healthcare.diagnostics.BlockMorgueCabinet;
import com.micatechnologies.minecraft.csm.healthcare.nursecall.BlockNurseCallMasterPanel;
import com.micatechnologies.minecraft.csm.healthcare.patient.BlockLectureSeating;
import com.micatechnologies.minecraft.csm.healthcare.patient.BlockSimulationMannequin;
import com.micatechnologies.minecraft.csm.healthcare.systems.BlockInfantSecurityPanel;
import com.micatechnologies.minecraft.csm.healthcare.systems.BlockMedicalGasAlarm;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Healthcare module's clinical tab: nursing (the nurse call master panel, the nurses'
 * station, medication, infection control, linen), the ICU, the emergency department and surgery
 * support, and later imaging, the lab, pharmacy, the morgue and the building systems.
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
    // --- gen_healthcare_clinical.py --fragments ---
    initTabBlock(new BlockHealthcareScreen("icu_ventilator", new int[]{3, 0, 3, 13, 16, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockCeilingHead("icu_pendant", new int[]{2, 0, 1, 14, 16, 14}, 1));
    initTabBlock(new BlockCeilingHead("surgical_equipment_boom", new int[]{2, 0, 2, 14, 16, 14}, 1));
    initTabBlock(new BlockCeilingHeadLight("trauma_exam_light", new int[]{2, 2, 2, 14, 16, 14}, 15, 2));
    initTabBlock(new BlockHealthcareScreen("crash_cart", new int[]{1, 0, 3, 15, 16, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcareScreen("defibrillator", new int[]{3, 0, 3, 13, 16, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockNurseStation("nurse_station_counter", new int[]{0, 0, 0, 16, 16, 16}, false));
    initTabBlock(new BlockNurseStation("nurse_station_counter_monitors", new int[]{0, 0, 0, 16, 16, 16}, false));
    initTabBlock(new BlockNurseStation("nurse_station_corner", new int[]{0, 0, 0, 16, 16, 16}, true));
    initTabBlock(new BlockHealthcarePair("patient_status_board", new int[]{0, 0, 13, 16, 16, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, BlockHealthcarePair.Shape.WIDE, false));
    initTabBlock(new BlockHealthcareScreen("workstation_on_wheels", new int[]{2, 0, 2, 14, 16, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcarePair("medication_dispensing_cabinet", new int[]{0, 0, 2, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcare("medication_cart", new int[]{1, 0, 2, 15, 16, 14}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("medication_fridge", new int[]{1, 0, 2, 15, 15, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcareScreen("triage_desk", new int[]{0, 0, 1, 16, 16, 16}, HealthcareMaterial.WOOD, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcarePair("decontamination_shower", new int[]{2, 0, 3, 14, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.WALL, BlockHealthcarePair.Shape.TALL, false));
    initTabBlock(new BlockEdTrackingBoard());
    initTabBlock(new BlockHealthcarePair("ambulance_only_marking", new int[]{0, 0, 0, 16, 1, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.WIDE, false));
    initTabBlock(new BlockHealthcare("ambulance_only_sign", new int[]{1, 1, 15, 15, 15, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcarePair("blanket_warming_cabinet", new int[]{1, 0, 1, 15, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcareScreen("autoclave", new int[]{1, 0, 1, 15, 15, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("case_cart", new int[]{1, 0, 2, 15, 16, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcarePair("scrub_attire_dispenser", new int[]{0, 0, 1, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    // --- gen_healthcare_clinics.py --fragments ---
    initTabBlock(new BlockHealthcareScreen("dialysis_machine", new int[]{2, 0, 3, 14, 16, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcarePair("dialysis_ro_unit", new int[]{0, 0, 1, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcareScreen("endoscopy_tower", new int[]{2, 0, 3, 15, 16, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    // --- Healthcare - Clinical (gen_healthcare_diagnostics.py --fragments) ---
    initTabBlock(new BlockHealthcareUnit("ct_scanner", new int[]{0, 0, -1, -1, 0, 1, 0, 0, 1, 1, 0, 1, -1, 1, 1, 0, 1, 1, 1, 1, 1}, new int[]{3, 0, -16, 13, 12, 16, -10, 0, 18, 26, 31, 30}));
    initTabBlock(new BlockHealthcareUnit("mri_scanner", new int[]{-1, 0, 0, 1, 0, 0, -1, 1, 0, 0, 1, 0, 1, 1, 0, -1, 0, 1, 0, 0, 1, 1, 0, 1, -1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, -1}, new int[]{3, 0, -16, 13, 12, 6, -12, 0, 6, 28, 30, 32}));
    initTabBlock(new BlockCeilingHead("xray_ceiling_tube", new int[]{2, 0, 2, 14, 16, 14}, 2));
    initTabBlock(new BlockHealthcarePair("xray_table", new int[]{0, 0, 2, 16, 12, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.WIDE));
    initTabBlock(new BlockHealthcarePair("xray_wall_bucky", new int[]{2, 0, 8, 14, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcarePair("c_arm", new int[]{2, 0, 2, 14, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcareScreen("c_arm_monitor_cart", new int[]{2, 0, 3, 14, 16, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcarePair("mammography_unit", new int[]{3, 0, 1, 13, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcare("lead_apron_rack", new int[]{1, 1, 12, 15, 15, 16}, HealthcareMaterial.CLOTH, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcarePair("radiology_reading_workstation", new int[]{0, 0, 3, 16, 16, 15}, HealthcareMaterial.WOOD, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.WIDE));
    initTabBlock(new BlockHealthcareLight("xray_in_use_light", new int[]{2, 4, 12, 14, 12, 16}, 7));
    initTabBlock(new BlockHealthcareLight("mri_in_use_light", new int[]{2, 4, 12, 14, 12, 16}, 7));
    initTabBlock(new BlockHealthcare("mri_zone_1_sign", new int[]{2, 2, 15, 14, 14, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("mri_zone_2_sign", new int[]{2, 2, 15, 14, 14, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("mri_zone_3_sign", new int[]{2, 2, 15, 14, 14, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("mri_zone_4_sign", new int[]{2, 2, 15, 14, 14, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("radiation_warning_sign", new int[]{2, 2, 15, 14, 14, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockLabBench("lab_bench"));
    initTabBlock(new BlockLabBench("lab_bench_sink"));
    initTabBlock(new BlockHealthcareScreen("chemistry_analyser", new int[]{0, 0, 2, 16, 16, 15}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcareScreen("haematology_analyser", new int[]{2, 0, 1, 14, 10, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("lab_centrifuge", new int[]{3, 0, 3, 13, 9, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcare("lab_microscope", new int[]{4, 0, 3, 12, 14, 13}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, true));
    initTabBlock(new BlockHealthcarePair("fume_hood", new int[]{0, 0, 0, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcarePair("biosafety_cabinet", new int[]{0, 0, 1, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcarePair("blood_bank_fridge", new int[]{0, 0, 0, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcare("specimen_pass_through", new int[]{2, 3, 12, 14, 13, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("pneumatic_tube_station", new int[]{3, 0, 9, 13, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcarePair("iv_compounding_hood", new int[]{0, 0, 1, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcarePair("pharmacy_bin_shelving", new int[]{0, 0, 1, 16, 16, 15}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcarePair("pharmacy_unit_dose_cabinet", new int[]{0, 0, 0, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockMorgueCabinet());
    initTabBlock(new BlockHealthcarePair("autopsy_table", new int[]{0, 0, 2, 16, 12, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.WIDE));
    initTabBlock(new BlockHealthcarePair("body_cart", new int[]{0, 0, 2, 16, 16, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.WIDE));
    // --- gen_healthcare_patient_areas.py --fragments ---
    initTabBlock(new BlockSimulationMannequin());
    initTabBlock(new BlockHealthcare("anatomical_torso_model", new int[]{4, 0, 5, 12, 16, 11}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.FLOOR, false));
    initTabBlock(new BlockHealthcarePair("skeleton_stand", new int[]{3, 0, 6, 13, 16, 15}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL));
    initTabBlock(new BlockHealthcarePair("dissection_table", new int[]{0, 0, 2, 16, 12, 14}, HealthcareMaterial.METAL, BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.WIDE));
    initTabBlock(new BlockLectureSeating());
    // --- gen_healthcare_systems.py --fragments ---
    initTabBlock(new BlockCodeBlueButton("code_blue_button", new int[]{4, 3, 13, 12, 13, 16}));
    initTabBlock(new BlockCodeBlueAnnunciator("code_blue_annunciator", new int[]{3, 1, 12, 13, 15, 16}));
    initTabBlock(new BlockHealthcare("medical_gas_zone_valve_box", new int[]{0, 0, 13, 16, 16, 16}, HealthcareMaterial.METAL, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockMedicalGasAlarm("medical_gas_master_alarm", new int[]{2, 2, 14, 14, 14, 16}));
    initTabBlock(new BlockHealthcare("hospital_outlet_normal", new int[]{5, 4, 15, 11, 12, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("hospital_outlet_emergency", new int[]{5, 4, 15, 11, 12, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockHealthcare("hospital_outlet_pair", new int[]{2, 4, 15, 14, 12, 16}, HealthcareMaterial.PLASTIC, BlockHealthcare.Mount.WALL, false));
    initTabBlock(new BlockInfantSecurityPanel("infant_security_panel", new int[]{4, 3, 14, 12, 13, 16}));
  }
}

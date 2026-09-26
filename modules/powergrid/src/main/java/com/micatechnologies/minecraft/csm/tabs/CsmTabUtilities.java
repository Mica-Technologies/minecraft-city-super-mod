package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityPanel;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityRun;
import com.micatechnologies.minecraft.csm.streetscape.BlockUtilityBox;
import com.micatechnologies.minecraft.csm.streetscape.UtilityBoxSpec;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Utilities tab: the services a city runs to its buildings, from the meters on the wall back
 * to the plant. The overhead line's poles and hardware stay in the Power Grid tab, in the same
 * module; see {@code assets/docs/UTILITIES_SYSTEM.md} for why there are two.
 *
 * <p>The block lines are written by {@code dev-env-utils/scripts/gen_utilities_meters.py
 * --fragments}, which measures every box from the model it writes.</p>
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 28)
public class CsmTabUtilities extends CsmTab {

  @Override
  public String getTabId() {
    return "tabutilities";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("electric_meter_digital");
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
    // --- Building service meters (gen_utilities_meters.py --fragments) ---
    initTabBlock(new BlockUtilityFixture("electric_meter_digital", new double[]{4.25, 0, 10.29, 11.75, 13, 16}));
    initTabBlock(new BlockUtilityFixture("electric_meter_analog", new double[]{4.25, 0, 10.29, 11.75, 13, 16}));
    initTabBlock(new BlockUtilityFixture("electric_meter_socket", new double[]{4.25, 0, 12.49, 11.75, 13, 16}));
    initTabBlock(new BlockUtilityRun("electric_meter_bank", new double[]{0, 0.6, 10.29, 16, 15.9, 16}));
    initTabBlock(new BlockUtilityFixture("service_disconnect", new double[]{3.2, 0, 11.5, 11.5, 16, 16}));
    initTabBlock(new BlockUtilityPanel("main_panel", new double[]{2.5, 1, 13.2, 13.5, 15.5, 16}, new double[]{2.5, 1, 3, 13.7, 15.5, 16}));
    initTabBlock(new BlockUtilityBox("electric_switchboard", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.019, 0, 0.049, 0.981, 2, 1), null)));
    initTabBlock(new BlockUtilityFixture("gas_meter", new double[]{4.5, 0, 10.95, 15, 15.8, 16}));
    initTabBlock(new BlockUtilityRun("gas_meter_bank", new double[]{0, 0, 10.95, 16, 16, 16}));
    initTabBlock(new BlockUtilityFixture("water_meter_setter", new double[]{0.6, 0, 11.1, 14.3, 16, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_electric", new double[]{1, 9, 15.5, 15, 12.5, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_gas", new double[]{1, 9, 15.5, 15, 12.5, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_water", new double[]{1, 9, 15.5, 15, 12.5, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_disconnect", new double[]{1, 9, 15.5, 15, 12.5, 16}));
  }
}

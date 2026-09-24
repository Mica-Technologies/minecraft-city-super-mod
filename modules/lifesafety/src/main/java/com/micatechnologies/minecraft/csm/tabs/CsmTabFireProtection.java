package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSounderFactory;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionCabinet;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockMagneticDoorHolder;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockStandpipePipe;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockStandpipeWallPipe;
import net.minecraft.util.math.AxisAlignedBB;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerSilver;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler2;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler3;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler4;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler5;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler6;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Fire Protection tab of the Life Safety module: sprinklers, and the rest of what a
 * building has to fight a fire with besides its alarm. The sprinklers moved here from the one
 * Life Safety tab with their registry names unchanged.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 22)
public class CsmTabFireProtection extends CsmTab {

  @Override
  public String getTabId() {
    return "tabfireprotection";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("fire_extinguisher_abc");
  }

  @Override
  public boolean getTabSearchable() {
    return true;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // --- Fire Protection (gen_fire_protection.py --fragments) ---
    initTabBlock(new BlockFireProtectionProp("fire_extinguisher_abc", new int[]{4, 0, 8, 12, 14, 16}, false));
    initTabBlock(new BlockFireProtectionProp("fire_extinguisher_co2", new int[]{4, 0, 8, 12, 14, 16}, false));
    initTabBlock(new BlockFireProtectionProp("fire_extinguisher_water", new int[]{4, 0, 8, 12, 14, 16}, false));
    initTabBlock(new BlockFireProtectionProp("fire_extinguisher_k", new int[]{4, 0, 8, 12, 14, 16}, false));
    initTabBlock(new BlockFireProtectionCabinet("extinguisher_cabinet_glass", new int[]{2, 1, 9, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionCabinet("extinguisher_cabinet_steel", new int[]{2, 1, 9, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionCabinet("fire_hose_cabinet", new int[]{2, 1, 9, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionCabinet("aed_cabinet", new int[]{3, 2, 10, 13, 14, 16}, true));
    initTabBlock(new BlockFireProtectionCabinet("knox_box", new int[]{5, 4, 12, 11, 11, 16}, false));
    initTabBlock(new BlockFireProtectionProp("standpipe_hose_valve", new int[]{5, 4, 5, 11, 12, 16}, true));
    // Connectable dry standpipe (gen_standpipe_system.py --fragments); the riser is its red
    // wall branch pipe.
    initTabBlock(new BlockStandpipePipe("standpipe_pipe_main_red", 4.0F));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_wall_pipe_main_red", 4.0F,
        BlockStandpipeWallPipe.Fitting.NONE, null));
    initTabBlock(new BlockStandpipePipe("standpipe_pipe_main_silver", 4.0F));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_wall_pipe_main_silver", 4.0F,
        BlockStandpipeWallPipe.Fitting.NONE, null));
    initTabBlock(new BlockStandpipePipe("standpipe_pipe_branch_red", 3.0F));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_riser", 3.0F,
        BlockStandpipeWallPipe.Fitting.NONE, null));
    initTabBlock(new BlockStandpipePipe("standpipe_pipe_branch_silver", 3.0F));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_wall_pipe_branch_silver", 3.0F,
        BlockStandpipeWallPipe.Fitting.NONE, null));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_inlet_manifold_red", 4.0F,
        BlockStandpipeWallPipe.Fitting.INLET,
        new AxisAlignedBB(0.0930D, 0.1500D, 0.1993D, 0.9070D, 0.8007D, 0.9875D)));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_inlet_manifold_silver", 4.0F,
        BlockStandpipeWallPipe.Fitting.INLET,
        new AxisAlignedBB(0.0930D, 0.1500D, 0.1993D, 0.9070D, 0.8007D, 0.9875D)));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_hose_outlet_red", 4.0F,
        BlockStandpipeWallPipe.Fitting.HOSE_OUTLET,
        new AxisAlignedBB(0.2000D, 0.1993D, 0.0867D, 0.8000D, 0.8320D, 0.9875D)));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_hose_outlet_silver", 4.0F,
        BlockStandpipeWallPipe.Fitting.HOSE_OUTLET,
        new AxisAlignedBB(0.2000D, 0.1993D, 0.0867D, 0.8000D, 0.8320D, 0.9875D)));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_air_valve_red", 4.0F,
        BlockStandpipeWallPipe.Fitting.AIR_VALVE,
        new AxisAlignedBB(0.2000D, 0.1993D, 0.3875D, 0.8000D, 1.0000D, 0.9875D)));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_air_valve_silver", 4.0F,
        BlockStandpipeWallPipe.Fitting.AIR_VALVE,
        new AxisAlignedBB(0.2000D, 0.1993D, 0.3875D, 0.8000D, 1.0000D, 0.9875D)));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_drain_valve_red", 4.0F,
        BlockStandpipeWallPipe.Fitting.VALVE,
        new AxisAlignedBB(0.1750D, 0.2180D, 0.1368D, 0.8250D, 0.7820D, 1.0000D)));
    initTabBlock(new BlockStandpipeWallPipe("standpipe_drain_valve_silver", 4.0F,
        BlockStandpipeWallPipe.Fitting.VALVE,
        new AxisAlignedBB(0.1750D, 0.2180D, 0.1368D, 0.8250D, 0.7820D, 1.0000D)));
    initTabBlock(new BlockFireProtectionProp("fdc_siamese_brass", new int[]{2, 3, 8, 14, 13, 16}, true));
    initTabBlock(new BlockFireProtectionProp("fdc_siamese_chrome", new int[]{2, 3, 8, 14, 13, 16}, true));
    initTabBlock(new BlockFireProtectionProp("fdc_freestanding", new int[]{4, 0, 3, 12, 13, 11}, true));
    initTabBlock(new BlockFireProtectionProp("fdc_storz", new int[]{3, 3, 9, 13, 13, 16}, true));
    initTabBlock(new BlockFireProtectionProp("sprinkler_alarm_valve", new int[]{4, 0, 7, 12, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("osy_gate_valve", new int[]{3, 0, 7, 13, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("post_indicator_valve", new int[]{5, 0, 4, 11, 16, 12}, true));
    initTabBlock(new BlockFireProtectionProp("fire_backflow_preventer", new int[]{0, 0, 4, 16, 14, 12}, true));
    initTabBlock(new BlockFireAlarmSounderFactory("water_motor_gong", "csm:water_motor_gong", new AxisAlignedBB(0.093750, 0.093750, 0.650000, 0.906250, 0.906250, 1.000000)));
    initTabBlock(new BlockMagneticDoorHolder("magnetic_door_holder", new int[]{5, 4, 12, 11, 12, 16}));
    initTabBlock(new BlockMagneticDoorHolder("magnetic_door_holder_floor", new int[]{5, 0, 4, 11, 9, 12}));
    initTabBlock(new BlockFireProtectionProp("extinguisher_sign", new int[]{2, 1, 15, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionProp("hose_sign", new int[]{2, 1, 15, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionProp("fdc_sign", new int[]{2, 1, 15, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionProp("riser_sign", new int[]{2, 1, 15, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionProp("fire_door_sign", new int[]{2, 1, 15, 14, 15, 16}, false));
    initTabBlock(new BlockFireProtectionProp("aed_sign", new int[]{2, 1, 15, 14, 15, 16}, false));
    // --- Sprinklers, moved here from the one Life Safety tab ---
    initTabBlock(BlockFireAlarmSprinklerBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSprinklerSilver.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSprinklerWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler2.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler3.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler4.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler5.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler6.class, fmlPreInitializationEvent);
  }
}

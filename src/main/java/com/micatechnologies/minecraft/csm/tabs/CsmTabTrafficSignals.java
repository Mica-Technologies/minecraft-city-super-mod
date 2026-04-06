package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalk;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkSignalDouble;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkSignalSingle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonAudible;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonAutomated;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonFemale;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonMale;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonPsGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter1;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter2;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHawkSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableRampMeterOnSignalLeftMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableRampMeterOnSignalMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableRampMeterOnSignalRightMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableTattleTaleBeacon;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableTrafficSignalTrainController;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockOverheightDetectionSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorBell;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorBelowGround;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorBox;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorModern;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorShort;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorTinyCam;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficSignalController;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSensorZoneTool;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalConfigurationTool;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalHeadConfigTool;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalLinkTool;
import com.micatechnologies.minecraft.csm.trafficsignals.TrafficSignalBlocks;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for traffic signal blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 10)
public class CsmTabTrafficSignals extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabtrafficsignals";
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
    return CsmRegistry.getBlock("controllableverticalsolidsignal");
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
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockControllableCrosswalkSignalSingle.class,
        fmlPreInitializationEvent); // New custom-rendered crosswalk signal (symbol)
    initTabBlock(BlockControllableCrosswalkSignalDouble.class,
        fmlPreInitializationEvent); // New custom-rendered crosswalk signal (text)
    initTabBlock(BlockControllableCrosswalkButtonAudible.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonAudible
    initTabBlock(BlockControllableCrosswalkButtonAutomated.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonAutomated
    initTabBlock(BlockControllableCrosswalkButtonFemale.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonFemale
    initTabBlock(BlockControllableCrosswalkButtonMale.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonMale
    initTabBlock(BlockControllableCrosswalkTweeter1.class,
        fmlPreInitializationEvent); // ControllableCrosswalkTweeter1
    initTabBlock(BlockControllableCrosswalkTweeter2.class,
        fmlPreInitializationEvent); // ControllableCrosswalkTweeter2
    // Factory-created signal head blocks (from TrafficSignalBlocks)
    initTabBlock(TrafficSignalBlocks.DOGHOUSE_MAIN_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.DOGHOUSE_MAIN_RIGHT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.DOGHOUSE_SECONDARY_LEFT_FYA_SIGNAL);
    initTabBlock(TrafficSignalBlocks.DOGHOUSE_SECONDARY_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.DOGHOUSE_SECONDARY_RIGHT_FYA_SIGNAL);
    initTabBlock(TrafficSignalBlocks.DOGHOUSE_SECONDARY_RIGHT_SIGNAL);
    initTabBlock(BlockControllableHawkSignal.class,
        fmlPreInitializationEvent); // ControllableHawkSignal
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_AHEAD_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_BIKE_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_RAIL_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_RIGHT2_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_RIGHT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_SOLID_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_U_TURN_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_UP_LEFT_SIGNAL);
    initTabBlock(BlockControllableRampMeterOnSignalLeftMount.class,
        fmlPreInitializationEvent); // ControllableRampMeterOnSignalLeftMount
    initTabBlock(BlockControllableRampMeterOnSignalMount.class,
        fmlPreInitializationEvent); // ControllableRampMeterOnSignalMount
    initTabBlock(BlockControllableRampMeterOnSignalRightMount.class,
        fmlPreInitializationEvent); // ControllableRampMeterOnSignalRightMount
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_GREEN);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_RED);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_YELLOW);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_YELLOW_ADVANCE_FLASH);
    initTabBlock(BlockControllableTattleTaleBeacon.class,
        fmlPreInitializationEvent); // ControllableTattleTaleBeacon
    initTabBlock(BlockControllableTrafficSignalTrainController.class,
        fmlPreInitializationEvent); // ControllableTrafficSignalTrainController
    initTabBlock(TrafficSignalBlocks.VERTICAL_AHEAD_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_AHEAD_SIGNAL8812_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_BIKE_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_BIKE_SIGNAL8_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_BIKE_SIGNAL8_INCH_BLACK);
    initTabBlock(TrafficSignalBlocks.VERTICAL_BIKE_SIGNAL4_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_HYBRID_LEFT_ADD_ON_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_HYBRID_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_ADD_ON_FYA_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_ADD_ON_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_DOUBLE_ADD_ON_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_SIGNAL8812_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_SIGNAL_LED);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_SIGNAL_SOLID_RED);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RAIL_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT2_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_ADD_ON_FYA_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_ADD_ON_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_DOUBLE_ADD_ON_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_FLASH_YELLOW_ADD_ON_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_FLASH_YELLOW_SR_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_FLASH_YELLOW_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_SIGNAL8812_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_FLASH_GREEN_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_FLASH_RED_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_FLASH_YELLOW_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL1288_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL8812_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL8_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_LED);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_LED1288_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_LED8812_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_LED8_INCH);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_NO_RED_VISOR);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_NO_VISORS);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_REVERSED);
    initTabBlock(TrafficSignalBlocks.VERTICAL_UP_LEFT_ADD_ON_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_UP_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_UTURN_SIGNAL);
    initTabBlock(BlockControllableCrosswalkButtonPsGray.class, fmlPreInitializationEvent); // ControllableCrosswalkButtonPsGray
    initTabBlock(BlockTrafficLightSensor.class, fmlPreInitializationEvent); // TrafficLightSensor
    initTabBlock(BlockTrafficLightSensorBell.class,
        fmlPreInitializationEvent); // TrafficLightSensorBell
    initTabBlock(BlockTrafficLightSensorBelowGround.class,
        fmlPreInitializationEvent); // TrafficLightSensorBelowGround
    initTabBlock(BlockTrafficLightSensorBox.class,
        fmlPreInitializationEvent); // TrafficLightSensorBox
    initTabBlock(BlockTrafficLightSensorModern.class,
        fmlPreInitializationEvent); // TrafficLightSensorModern
    initTabBlock(BlockTrafficLightSensorShort.class,
        fmlPreInitializationEvent); // TrafficLightSensorShort
    initTabBlock(BlockTrafficLightSensorTinyCam.class,
        fmlPreInitializationEvent); // TrafficLightSensorTinyCam
    initTabBlock(BlockOverheightDetectionSensor.class,
        fmlPreInitializationEvent); // OverheightDetectionSensor
    initTabBlock(BlockTrafficSignalController.class,
        fmlPreInitializationEvent); // TrafficSignalController
    initTabItem(ItemSensorZoneTool.class, fmlPreInitializationEvent); // Sensor Zone Tool
    initTabItem(ItemSignalLinkTool.class, fmlPreInitializationEvent); // Signal Link Tool
    initTabItem(ItemSignalConfigurationTool.class,
        fmlPreInitializationEvent); // SignalConfigurationTool
    initTabItem(ItemSignalHeadConfigTool.class,
        fmlPreInitializationEvent); // SignalHeadConfigTool
  }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalk;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonAudible;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonAutomated;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonFemale;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonMale;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkButtonPsGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedBaseMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedLeftMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedRearMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedRightMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkLeftMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkLeftMount90Deg;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMount90Deg;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMountGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkRightMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkRightMount90Deg;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter1;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkTweeter2;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableDoghouseMainLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableDoghouseMainRightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableDoghouseSecondaryLeftFYASignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableDoghouseSecondaryLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableDoghouseSecondaryRightFYASignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableDoghouseSecondaryRightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHawkSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAheadSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleAheadSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleBikeSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleRailSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleRight2Signal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleRightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleSolidSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleUTurnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalAngleUpLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalBikeSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalRailSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalRight2Signal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalRightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalSolidSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalUTurnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableHorizontalUpLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableRampMeterOnSignalLeftMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableRampMeterOnSignalMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableRampMeterOnSignalRightMount;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalGreen;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalGreenLeftAngle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalGreenRightAngle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalRed;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalRedLeftAngle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalRedRightAngle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalYellow;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalYellowAdvanceFlash;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalYellowLeftAngle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalYellowRightAngle;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableTattleTaleBeacon;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableTrafficSignalTrainController;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAheadSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAheadSignal8812Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAheadSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2AheadSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2BikeSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2LeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2RailSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2Right2Signal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2RightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2SolidSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2UTurnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2UpLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleAheadSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleBikeSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleRailSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleRight2Signal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleRightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleSolidSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleUTurnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleUpLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalBikeSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalBikeSignal8Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalBikeSignal8InchBlack;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalBikeSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalHybridLeftAddOnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalHybridLeftAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalHybridLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalHybridLeftSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftAddOnFYASignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftAddOnFYASignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftAddOnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftDoubleAddOnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftSignal8812Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftSignalLED;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftSignalSolidRed;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRailSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRailSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRight2Signal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightAddOnFYASignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightAddOnFYASignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightAddOnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightDoubleAddOnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightFlashYellowAddOnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightFlashYellowSRSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightFlashYellowSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightSignal8812Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashGreenSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashGreenSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashRedSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashRedSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashYellowSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashYellowSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignal1288Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignal8812Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignal8Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalBarlo;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalLED;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalLED1288Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalLED8812Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalLED8Inch;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalNoRedVisor;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalNoVisors;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalReversed;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalUpLeftAddOnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalUpLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalUturnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorBell;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorBelowGround;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorBox;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorModern;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorShort;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficLightSensorTinyCam;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficSignalController;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemEWSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemNSSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemSignalConfigurationTool;
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
   * Initializes all the items belonging to the tab.
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockControllableCrosswalk.class,
        fmlPreInitializationEvent); // ControllableCrosswalk
    initTabBlock(BlockControllableCrosswalkButtonAudible.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonAudible
    initTabBlock(BlockControllableCrosswalkButtonAutomated.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonAutomated
    initTabBlock(BlockControllableCrosswalkButtonFemale.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonFemale
    initTabBlock(BlockControllableCrosswalkButtonMale.class,
        fmlPreInitializationEvent); // ControllableCrosswalkButtonMale
    initTabBlock(BlockControllableCrosswalkDoubleWordedBaseMount.class,
        fmlPreInitializationEvent); // ControllableCrosswalkDoubleWordedBaseMount
    initTabBlock(BlockControllableCrosswalkDoubleWordedLeftMount.class,
        fmlPreInitializationEvent); // ControllableCrosswalkDoubleWordedLeftMount
    initTabBlock(BlockControllableCrosswalkDoubleWordedRearMount.class,
        fmlPreInitializationEvent); // ControllableCrosswalkDoubleWordedRearMount
    initTabBlock(BlockControllableCrosswalkDoubleWordedRightMount.class,
        fmlPreInitializationEvent); // ControllableCrosswalkDoubleWordedRightMount
    initTabBlock(BlockControllableCrosswalkLeftMount.class,
        fmlPreInitializationEvent); // ControllableCrosswalkLeftMount
    initTabBlock(BlockControllableCrosswalkLeftMount90Deg.class,
        fmlPreInitializationEvent); // ControllableCrosswalkLeftMount90Deg
    initTabBlock(BlockControllableCrosswalkMount.class,
        fmlPreInitializationEvent); // ControllableCrosswalkMount
    initTabBlock(BlockControllableCrosswalkMount90Deg.class,
        fmlPreInitializationEvent); // ControllableCrosswalkMount90Deg
    initTabBlock(BlockControllableCrosswalkRightMount.class,
        fmlPreInitializationEvent); // ControllableCrosswalkRightMount
    initTabBlock(BlockControllableCrosswalkRightMount90Deg.class,
        fmlPreInitializationEvent); // ControllableCrosswalkRightMount90Deg
    initTabBlock(BlockControllableCrosswalkTweeter1.class,
        fmlPreInitializationEvent); // ControllableCrosswalkTweeter1
    initTabBlock(BlockControllableCrosswalkTweeter2.class,
        fmlPreInitializationEvent); // ControllableCrosswalkTweeter2
    initTabBlock(BlockControllableDoghouseMainLeftSignal.class,
        fmlPreInitializationEvent); // ControllableDoghouseMainLeftSignal
    initTabBlock(BlockControllableDoghouseMainRightSignal.class,
        fmlPreInitializationEvent); // ControllableDoghouseMainRightSignal
    initTabBlock(BlockControllableDoghouseSecondaryLeftFYASignal.class,
        fmlPreInitializationEvent); // ControllableDoghouseSecondaryLeftFYASignal
    initTabBlock(BlockControllableDoghouseSecondaryLeftSignal.class,
        fmlPreInitializationEvent); // ControllableDoghouseSecondaryLeftSignal
    initTabBlock(BlockControllableDoghouseSecondaryRightFYASignal.class,
        fmlPreInitializationEvent); // ControllableDoghouseSecondaryRightFYASignal
    initTabBlock(BlockControllableDoghouseSecondaryRightSignal.class,
        fmlPreInitializationEvent); // ControllableDoghouseSecondaryRightSignal
    initTabBlock(BlockControllableHawkSignal.class,
        fmlPreInitializationEvent); // ControllableHawkSignal
    initTabBlock(BlockControllableHorizontalAheadSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAheadSignal
    initTabBlock(BlockControllableHorizontalAngleAheadSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleAheadSignal
    initTabBlock(BlockControllableHorizontalAngleBikeSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleBikeSignal
    initTabBlock(BlockControllableHorizontalAngleLeftSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleLeftSignal
    initTabBlock(BlockControllableHorizontalAngleRailSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleRailSignal
    initTabBlock(BlockControllableHorizontalAngleRight2Signal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleRight2Signal
    initTabBlock(BlockControllableHorizontalAngleRightSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleRightSignal
    initTabBlock(BlockControllableHorizontalAngleSolidSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleSolidSignal
    initTabBlock(BlockControllableHorizontalAngleUTurnSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleUTurnSignal
    initTabBlock(BlockControllableHorizontalAngleUpLeftSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalAngleUpLeftSignal
    initTabBlock(BlockControllableHorizontalBikeSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalBikeSignal
    initTabBlock(BlockControllableHorizontalLeftSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalLeftSignal
    initTabBlock(BlockControllableHorizontalRailSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalRailSignal
    initTabBlock(BlockControllableHorizontalRight2Signal.class,
        fmlPreInitializationEvent); // ControllableHorizontalRight2Signal
    initTabBlock(BlockControllableHorizontalRightSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalRightSignal
    initTabBlock(BlockControllableHorizontalSolidSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalSolidSignal
    initTabBlock(BlockControllableHorizontalUTurnSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalUTurnSignal
    initTabBlock(BlockControllableHorizontalUpLeftSignal.class,
        fmlPreInitializationEvent); // ControllableHorizontalUpLeftSignal
    initTabBlock(BlockControllableRampMeterOnSignalLeftMount.class,
        fmlPreInitializationEvent); // ControllableRampMeterOnSignalLeftMount
    initTabBlock(BlockControllableRampMeterOnSignalMount.class,
        fmlPreInitializationEvent); // ControllableRampMeterOnSignalMount
    initTabBlock(BlockControllableRampMeterOnSignalRightMount.class,
        fmlPreInitializationEvent); // ControllableRampMeterOnSignalRightMount
    initTabBlock(BlockControllableSingleSolidSignalGreen.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalGreen
    initTabBlock(BlockControllableSingleSolidSignalGreenLeftAngle.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalGreenLeftAngle
    initTabBlock(BlockControllableSingleSolidSignalGreenRightAngle.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalGreenRightAngle
    initTabBlock(BlockControllableSingleSolidSignalRed.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalRed
    initTabBlock(BlockControllableSingleSolidSignalRedLeftAngle.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalRedLeftAngle
    initTabBlock(BlockControllableSingleSolidSignalRedRightAngle.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalRedRightAngle
    initTabBlock(BlockControllableSingleSolidSignalYellow.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalYellow
    initTabBlock(BlockControllableSingleSolidSignalYellowAdvanceFlash.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalYellowAdvanceFlash
    initTabBlock(BlockControllableSingleSolidSignalYellowLeftAngle.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalYellowLeftAngle
    initTabBlock(BlockControllableSingleSolidSignalYellowRightAngle.class,
        fmlPreInitializationEvent); // ControllableSingleSolidSignalYellowRightAngle
    initTabBlock(BlockControllableTattleTaleBeacon.class,
        fmlPreInitializationEvent); // ControllableTattleTaleBeacon
    initTabBlock(BlockControllableTrafficSignalTrainController.class,
        fmlPreInitializationEvent); // ControllableTrafficSignalTrainController
    initTabBlock(BlockControllableVerticalAheadSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAheadSignal
    initTabBlock(BlockControllableVerticalAheadSignal8812Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalAheadSignal8812Inch
    initTabBlock(BlockControllableVerticalAngle2AheadSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2AheadSignal
    initTabBlock(BlockControllableVerticalAngle2BikeSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2BikeSignal
    initTabBlock(BlockControllableVerticalAngle2LeftSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2LeftSignal
    initTabBlock(BlockControllableVerticalAngle2RailSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2RailSignal
    initTabBlock(BlockControllableVerticalAngle2Right2Signal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2Right2Signal
    initTabBlock(BlockControllableVerticalAngle2RightSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2RightSignal
    initTabBlock(BlockControllableVerticalAngle2SolidSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2SolidSignal
    initTabBlock(BlockControllableVerticalAngle2UTurnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2UTurnSignal
    initTabBlock(BlockControllableVerticalAngle2UpLeftSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngle2UpLeftSignal
    initTabBlock(BlockControllableVerticalAngleAheadSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleAheadSignal
    initTabBlock(BlockControllableVerticalAngleBikeSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleBikeSignal
    initTabBlock(BlockControllableVerticalAngleLeftSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleLeftSignal
    initTabBlock(BlockControllableVerticalAngleRailSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleRailSignal
    initTabBlock(BlockControllableVerticalAngleRight2Signal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleRight2Signal
    initTabBlock(BlockControllableVerticalAngleRightSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleRightSignal
    initTabBlock(BlockControllableVerticalAngleSolidSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleSolidSignal
    initTabBlock(BlockControllableVerticalAngleUTurnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleUTurnSignal
    initTabBlock(BlockControllableVerticalAngleUpLeftSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalAngleUpLeftSignal
    initTabBlock(BlockControllableVerticalBikeSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalBikeSignal
    initTabBlock(BlockControllableVerticalBikeSignal8Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalBikeSignal8Inch
    initTabBlock(BlockControllableVerticalBikeSignal8InchBlack.class,
        fmlPreInitializationEvent); // ControllableVerticalBikeSignal8InchBlack
    initTabBlock(BlockControllableVerticalHybridLeftAddOnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalHybridLeftAddOnSignal
    initTabBlock(BlockControllableVerticalHybridLeftSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalHybridLeftSignal
    initTabBlock(BlockControllableVerticalLeftAddOnFYASignal.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftAddOnFYASignal
    initTabBlock(BlockControllableVerticalLeftAddOnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftAddOnSignal
    initTabBlock(BlockControllableVerticalLeftDoubleAddOnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftDoubleAddOnSignal
    initTabBlock(BlockControllableVerticalLeftSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftSignal
    initTabBlock(BlockControllableVerticalLeftSignal8812Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftSignal8812Inch
    initTabBlock(BlockControllableVerticalLeftSignalLED.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftSignalLED
    initTabBlock(BlockControllableVerticalLeftSignalSolidRed.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftSignalSolidRed
    initTabBlock(BlockControllableVerticalRailSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRailSignal
    initTabBlock(BlockControllableVerticalRight2Signal.class,
        fmlPreInitializationEvent); // ControllableVerticalRight2Signal
    initTabBlock(BlockControllableVerticalRightAddOnFYASignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRightAddOnFYASignal
    initTabBlock(BlockControllableVerticalRightAddOnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRightAddOnSignal
    initTabBlock(BlockControllableVerticalRightDoubleAddOnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRightDoubleAddOnSignal
    initTabBlock(BlockControllableVerticalRightFlashYellowAddOnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRightFlashYellowAddOnSignal
    initTabBlock(BlockControllableVerticalRightFlashYellowSRSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRightFlashYellowSRSignal
    initTabBlock(BlockControllableVerticalRightFlashYellowSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRightFlashYellowSignal
    initTabBlock(BlockControllableVerticalRightSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalRightSignal
    initTabBlock(BlockControllableVerticalRightSignal8812Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalRightSignal8812Inch
    initTabBlock(BlockControllableVerticalSolidFlashGreenSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidFlashGreenSignal
    initTabBlock(BlockControllableVerticalSolidFlashRedSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidFlashRedSignal
    initTabBlock(BlockControllableVerticalSolidFlashYellowSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidFlashYellowSignal
    initTabBlock(BlockControllableVerticalSolidSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignal
    initTabBlock(BlockControllableVerticalSolidSignal1288Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignal1288Inch
    initTabBlock(BlockControllableVerticalSolidSignal8812Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignal8812Inch
    initTabBlock(BlockControllableVerticalSolidSignal8Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignal8Inch
    initTabBlock(BlockControllableVerticalSolidSignalBarlo.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalBarlo
    initTabBlock(BlockControllableVerticalSolidSignalLED.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalLED
    initTabBlock(BlockControllableVerticalSolidSignalLED1288Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalLED1288Inch
    initTabBlock(BlockControllableVerticalSolidSignalLED8812Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalLED8812Inch
    initTabBlock(BlockControllableVerticalSolidSignalLED8Inch.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalLED8Inch
    initTabBlock(BlockControllableVerticalSolidSignalNoRedVisor.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalNoRedVisor
    initTabBlock(BlockControllableVerticalSolidSignalNoVisors.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalNoVisors
    initTabBlock(BlockControllableVerticalSolidSignalReversed.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalReversed
    initTabBlock(BlockControllableVerticalUpLeftAddOnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalUpLeftAddOnSignal
    initTabBlock(BlockControllableVerticalUpLeftSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalUpLeftSignal
    initTabBlock(BlockControllableVerticalUturnSignal.class,
        fmlPreInitializationEvent); // ControllableVerticalUturnSignal
    initTabBlock(BlockControllableVerticalSolidSignalGray.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidSignalGray
    initTabBlock(BlockControllableVerticalLeftSignalGray.class,
        fmlPreInitializationEvent); // ControllableVerticalLeftSignalGray
    initTabBlock(BlockControllableVerticalRightSignalGray.class,
        fmlPreInitializationEvent); // ControllableVerticalRightSignalGray
    initTabBlock(BlockControllableVerticalSolidFlashGreenSignalGray.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidFlashGreenSignalGray
    initTabBlock(BlockControllableVerticalSolidFlashYellowSignalGray.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidFlashYellowSignalGray
    initTabBlock(BlockControllableVerticalSolidFlashRedSignalGray.class,
        fmlPreInitializationEvent); // ControllableVerticalSolidFlashRedSignalGray
    initTabBlock(BlockControllableVerticalHybridLeftSignalGray.class, fmlPreInitializationEvent); // ControllableVerticalHybridLeftSignalGray
    initTabBlock(BlockControllableVerticalHybridLeftAddOnSignalGray.class, fmlPreInitializationEvent); // ControllableVerticalHybridLeftAddOnSignalGray
    initTabBlock(BlockControllableVerticalAheadSignalGray.class, fmlPreInitializationEvent); // ControllableVerticalAheadSignalGray
    initTabBlock(BlockControllableVerticalBikeSignalGray.class, fmlPreInitializationEvent); // ControllableVerticalBikeSignalGray
    initTabBlock(BlockControllableVerticalRailSignalGray.class, fmlPreInitializationEvent); // ControllableVerticalRailSignalGray
    initTabBlock(BlockControllableCrosswalkMountGray.class, fmlPreInitializationEvent); // ControllableCrosswalkMountGray
    initTabBlock(BlockControllableCrosswalkButtonPsGray.class, fmlPreInitializationEvent); // ControllableCrosswalkButtonPsGray
    initTabBlock(BlockControllableVerticalLeftAddOnSignalGray.class, fmlPreInitializationEvent); // ControllableVerticalLeftAddOnSignalGray
    initTabBlock(BlockControllableVerticalRightAddOnSignalGray.class, fmlPreInitializationEvent); // ControllableVerticalRightAddOnSignalGray
    initTabBlock(BlockControllableVerticalLeftAddOnFYASignalGray.class, fmlPreInitializationEvent); // ControllableVerticalLeftAddOnFYASignalGray
    initTabBlock(BlockControllableVerticalRightAddOnFYASignalGray.class, fmlPreInitializationEvent); // ControllableVerticalRightAddOnFYASignalGray
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
    initTabBlock(BlockTrafficSignalController.class,
        fmlPreInitializationEvent); // TrafficSignalController
    initTabItem(ItemEWSignalLinker.class, fmlPreInitializationEvent); // EWSignalLinker
    initTabItem(ItemNSSignalLinker.class, fmlPreInitializationEvent); // NSSignalLinker
    initTabItem(ItemSignalConfigurationTool.class,
        fmlPreInitializationEvent); // SignalConfigurationTool
  }
}

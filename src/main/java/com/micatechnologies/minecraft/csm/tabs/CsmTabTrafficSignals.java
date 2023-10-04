package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.BlockSV4;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderYellowBlack;
import com.micatechnologies.minecraft.csm.trafficsignals.*;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for traffic signal blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabTrafficSignals extends CsmTab
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
        return CsmRegistry.getBlock( BlockControllableVerticalSolidSignal.class );
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
    public void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent ) {
        initTabBlock( BlockControllableCrosswalk.class, fmlPreInitializationEvent ); // ControllableCrosswalk
        initTabBlock( BlockControllableCrosswalkButtonAudible.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkButtonAudible
        initTabBlock( BlockControllableCrosswalkButtonAutomated.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkButtonAutomated
        initTabBlock( BlockControllableCrosswalkButtonFemale.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkButtonFemale
        initTabBlock( BlockControllableCrosswalkButtonMale.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkButtonMale
        initTabBlock( BlockControllableCrosswalkDoubleWordedBaseMount.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkDoubleWordedBaseMount
        initTabBlock( BlockControllableCrosswalkDoubleWordedLeftMount.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkDoubleWordedLeftMount
        initTabBlock( BlockControllableCrosswalkDoubleWordedRearMount.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkDoubleWordedRearMount
        initTabBlock( BlockControllableCrosswalkDoubleWordedRightMount.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkDoubleWordedRightMount
        initTabBlock( BlockControllableCrosswalkLeftMount.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkLeftMount
        initTabBlock( BlockControllableCrosswalkLeftMount90Deg.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkLeftMount90Deg
        initTabBlock( BlockControllableCrosswalkMount.class, fmlPreInitializationEvent ); // ControllableCrosswalkMount
        initTabBlock( BlockControllableCrosswalkMount90Deg.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkMount90Deg
        initTabBlock( BlockControllableCrosswalkRightMount.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkRightMount
        initTabBlock( BlockControllableCrosswalkRightMount90Deg.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkRightMount90Deg
        initTabBlock( BlockControllableCrosswalkTweeter1.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkTweeter1
        initTabBlock( BlockControllableCrosswalkTweeter2.class,
                      fmlPreInitializationEvent ); // ControllableCrosswalkTweeter2
        initTabBlock( BlockControllableDoghouseMainLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableDoghouseMainLeftSignal
        initTabBlock( BlockControllableDoghouseMainRightSignal.class,
                      fmlPreInitializationEvent ); // ControllableDoghouseMainRightSignal
        initTabBlock( BlockControllableDoghouseSecondaryLeftFYASignal.class,
                      fmlPreInitializationEvent ); // ControllableDoghouseSecondaryLeftFYASignal
        initTabBlock( BlockControllableDoghouseSecondaryLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableDoghouseSecondaryLeftSignal
        initTabBlock( BlockControllableDoghouseSecondaryRightFYASignal.class,
                      fmlPreInitializationEvent ); // ControllableDoghouseSecondaryRightFYASignal
        initTabBlock( BlockControllableDoghouseSecondaryRightSignal.class,
                      fmlPreInitializationEvent ); // ControllableDoghouseSecondaryRightSignal
        initTabBlock( BlockControllableHawkSignal.class, fmlPreInitializationEvent ); // ControllableHawkSignal
        initTabBlock( BlockControllableHorizontalAheadSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAheadSignal
        initTabBlock( BlockControllableHorizontalAngleAheadSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleAheadSignal
        initTabBlock( BlockControllableHorizontalAngleBikeSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleBikeSignal
        initTabBlock( BlockControllableHorizontalAngleLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleLeftSignal
        initTabBlock( BlockControllableHorizontalAngleRailSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleRailSignal
        initTabBlock( BlockControllableHorizontalAngleRight2Signal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleRight2Signal
        initTabBlock( BlockControllableHorizontalAngleRightSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleRightSignal
        initTabBlock( BlockControllableHorizontalAngleSolidSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleSolidSignal
        initTabBlock( BlockControllableHorizontalAngleUTurnSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleUTurnSignal
        initTabBlock( BlockControllableHorizontalAngleUpLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalAngleUpLeftSignal
        initTabBlock( BlockControllableHorizontalBikeSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalBikeSignal
        initTabBlock( BlockControllableHorizontalLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalLeftSignal
        initTabBlock( BlockControllableHorizontalRailSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalRailSignal
        initTabBlock( BlockControllableHorizontalRight2Signal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalRight2Signal
        initTabBlock( BlockControllableHorizontalRightSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalRightSignal
        initTabBlock( BlockControllableHorizontalSolidSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalSolidSignal
        initTabBlock( BlockControllableHorizontalUTurnSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalUTurnSignal
        initTabBlock( BlockControllableHorizontalUpLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableHorizontalUpLeftSignal
        initTabBlock( BlockControllableRampMeterOnSignalLeftMount.class,
                      fmlPreInitializationEvent ); // ControllableRampMeterOnSignalLeftMount
        initTabBlock( BlockControllableRampMeterOnSignalMount.class,
                      fmlPreInitializationEvent ); // ControllableRampMeterOnSignalMount
        initTabBlock( BlockControllableRampMeterOnSignalRightMount.class,
                      fmlPreInitializationEvent ); // ControllableRampMeterOnSignalRightMount
        initTabBlock( BlockControllableSingleSolidSignalGreen.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalGreen
        initTabBlock( BlockControllableSingleSolidSignalGreenLeftAngle.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalGreenLeftAngle
        initTabBlock( BlockControllableSingleSolidSignalGreenRightAngle.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalGreenRightAngle
        initTabBlock( BlockControllableSingleSolidSignalRed.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalRed
        initTabBlock( BlockControllableSingleSolidSignalRedLeftAngle.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalRedLeftAngle
        initTabBlock( BlockControllableSingleSolidSignalRedRightAngle.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalRedRightAngle
        initTabBlock( BlockControllableSingleSolidSignalYellow.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalYellow
        initTabBlock( BlockControllableSingleSolidSignalYellowAdvanceFlash.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalYellowAdvanceFlash
        initTabBlock( BlockControllableSingleSolidSignalYellowLeftAngle.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalYellowLeftAngle
        initTabBlock( BlockControllableSingleSolidSignalYellowRightAngle.class,
                      fmlPreInitializationEvent ); // ControllableSingleSolidSignalYellowRightAngle
        initTabBlock( BlockControllableTattleTaleBeacon.class,
                      fmlPreInitializationEvent ); // ControllableTattleTaleBeacon
        initTabBlock( BlockControllableTrafficSignalTrainController.class,
                      fmlPreInitializationEvent ); // ControllableTrafficSignalTrainController
        initTabBlock( BlockControllableVerticalAheadSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAheadSignal
        initTabBlock( BlockControllableVerticalAheadSignal8812Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAheadSignal8812Inch
        initTabBlock( BlockControllableVerticalAngle2AheadSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2AheadSignal
        initTabBlock( BlockControllableVerticalAngle2BikeSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2BikeSignal
        initTabBlock( BlockControllableVerticalAngle2LeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2LeftSignal
        initTabBlock( BlockControllableVerticalAngle2RailSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2RailSignal
        initTabBlock( BlockControllableVerticalAngle2Right2Signal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2Right2Signal
        initTabBlock( BlockControllableVerticalAngle2RightSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2RightSignal
        initTabBlock( BlockControllableVerticalAngle2SolidSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2SolidSignal
        initTabBlock( BlockControllableVerticalAngle2UTurnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2UTurnSignal
        initTabBlock( BlockControllableVerticalAngle2UpLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngle2UpLeftSignal
        initTabBlock( BlockControllableVerticalAngleAheadSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleAheadSignal
        initTabBlock( BlockControllableVerticalAngleBikeSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleBikeSignal
        initTabBlock( BlockControllableVerticalAngleLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleLeftSignal
        initTabBlock( BlockControllableVerticalAngleRailSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleRailSignal
        initTabBlock( BlockControllableVerticalAngleRight2Signal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleRight2Signal
        initTabBlock( BlockControllableVerticalAngleRightSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleRightSignal
        initTabBlock( BlockControllableVerticalAngleSolidSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleSolidSignal
        initTabBlock( BlockControllableVerticalAngleUTurnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleUTurnSignal
        initTabBlock( BlockControllableVerticalAngleUpLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalAngleUpLeftSignal
        initTabBlock( BlockControllableVerticalBikeSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalBikeSignal
        initTabBlock( BlockControllableVerticalBikeSignal8Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalBikeSignal8Inch
        initTabBlock( BlockControllableVerticalBikeSignal8InchBlack.class,
                      fmlPreInitializationEvent ); // ControllableVerticalBikeSignal8InchBlack
        initTabBlock( BlockControllableVerticalHybridLeftAddOnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalHybridLeftAddOnSignal
        initTabBlock( BlockControllableVerticalHybridLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalHybridLeftSignal
        initTabBlock( BlockControllableVerticalLeftAddOnFYASignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalLeftAddOnFYASignal
        initTabBlock( BlockControllableVerticalLeftAddOnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalLeftAddOnSignal
        initTabBlock( BlockControllableVerticalLeftDoubleAddOnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalLeftDoubleAddOnSignal
        initTabBlock( BlockControllableVerticalLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalLeftSignal
        initTabBlock( BlockControllableVerticalLeftSignal8812Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalLeftSignal8812Inch
        initTabBlock( BlockControllableVerticalLeftSignalLED.class,
                      fmlPreInitializationEvent ); // ControllableVerticalLeftSignalLED
        initTabBlock( BlockControllableVerticalLeftSignalSolidRed.class,
                      fmlPreInitializationEvent ); // ControllableVerticalLeftSignalSolidRed
        initTabBlock( BlockControllableVerticalRailSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRailSignal
        initTabBlock( BlockControllableVerticalRight2Signal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRight2Signal
        initTabBlock( BlockControllableVerticalRightAddOnFYASignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightAddOnFYASignal
        initTabBlock( BlockControllableVerticalRightAddOnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightAddOnSignal
        initTabBlock( BlockControllableVerticalRightDoubleAddOnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightDoubleAddOnSignal
        initTabBlock( BlockControllableVerticalRightFlashYellowAddOnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightFlashYellowAddOnSignal
        initTabBlock( BlockControllableVerticalRightFlashYellowSRSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightFlashYellowSRSignal
        initTabBlock( BlockControllableVerticalRightFlashYellowSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightFlashYellowSignal
        initTabBlock( BlockControllableVerticalRightSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightSignal
        initTabBlock( BlockControllableVerticalRightSignal8812Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalRightSignal8812Inch
        initTabBlock( BlockControllableVerticalSolidFlashGreenSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidFlashGreenSignal
        initTabBlock( BlockControllableVerticalSolidFlashRedSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidFlashRedSignal
        initTabBlock( BlockControllableVerticalSolidFlashYellowSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidFlashYellowSignal
        initTabBlock( BlockControllableVerticalSolidSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignal
        initTabBlock( BlockControllableVerticalSolidSignal1288Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignal1288Inch
        initTabBlock( BlockControllableVerticalSolidSignal8812Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignal8812Inch
        initTabBlock( BlockControllableVerticalSolidSignal8Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignal8Inch
        initTabBlock( BlockControllableVerticalSolidSignalBarlo.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalBarlo
        initTabBlock( BlockControllableVerticalSolidSignalLED.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalLED
        initTabBlock( BlockControllableVerticalSolidSignalLED1288Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalLED1288Inch
        initTabBlock( BlockControllableVerticalSolidSignalLED8812Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalLED8812Inch
        initTabBlock( BlockControllableVerticalSolidSignalLED8Inch.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalLED8Inch
        initTabBlock( BlockControllableVerticalSolidSignalNoRedVisor.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalNoRedVisor
        initTabBlock( BlockControllableVerticalSolidSignalNoVisors.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalNoVisors
        initTabBlock( BlockControllableVerticalSolidSignalReversed.class,
                      fmlPreInitializationEvent ); // ControllableVerticalSolidSignalReversed
        initTabBlock( BlockControllableVerticalUpLeftAddOnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalUpLeftAddOnSignal
        initTabBlock( BlockControllableVerticalUpLeftSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalUpLeftSignal
        initTabBlock( BlockControllableVerticalUturnSignal.class,
                      fmlPreInitializationEvent ); // ControllableVerticalUturnSignal
        initTabBlock( BlockTrafficLightSensor.class, fmlPreInitializationEvent ); // TrafficLightSensor
        initTabBlock( BlockTrafficLightSensorBell.class, fmlPreInitializationEvent ); // TrafficLightSensorBell
        initTabBlock( BlockTrafficLightSensorBelowGround.class,
                      fmlPreInitializationEvent ); // TrafficLightSensorBelowGround
        initTabBlock( BlockTrafficLightSensorBox.class, fmlPreInitializationEvent ); // TrafficLightSensorBox
        initTabBlock( BlockTrafficLightSensorModern.class, fmlPreInitializationEvent ); // TrafficLightSensorModern
        initTabBlock( BlockTrafficLightSensorShort.class, fmlPreInitializationEvent ); // TrafficLightSensorShort
        initTabBlock( BlockTrafficSignalController.class, fmlPreInitializationEvent ); // TrafficSignalController
    }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.*;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for life safety blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabLifeSafety extends CsmTab
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
        return "tablifesafety";
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
        return CsmRegistry.getBlock( "mclacodeapprovedexitsignisa" );
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
    public void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent ) {
        initTabBlock( BlockEdwardsEmergencyPhone.class, fmlPreInitializationEvent ); // EdwardsEmergencyPhone
        initTabBlock( BlockElight.class, fmlPreInitializationEvent ); // Elight
        initTabBlock( BlockExitSignSingleSided.class, fmlPreInitializationEvent ); // ExitSignSingleSided
        initTabBlock( BlockFireAlarmControlPanel.class, fmlPreInitializationEvent ); // FireAlarmControlPanel
        initTabBlock( BlockFireAlarmESTAdaptahornGray.class, fmlPreInitializationEvent ); // FireAlarmESTAdaptahornGray
        initTabBlock( BlockFireAlarmESTAdaptahornRed.class, fmlPreInitializationEvent ); // FireAlarmESTAdaptahornRed
        initTabBlock( BlockFireAlarmESTGenesisRed.class, fmlPreInitializationEvent ); // FireAlarmESTGenesisRed
        initTabBlock( BlockFireAlarmESTGenesisWhite.class, fmlPreInitializationEvent ); // FireAlarmESTGenesisWhite
        initTabBlock( BlockFireAlarmESTIntegrityHornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmESTIntegrityHornStrobeRed
        initTabBlock( BlockFireAlarmESTIntegrityHornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmESTIntegrityHornStrobeWhite
        initTabBlock( BlockFireAlarmESTIntegritySpeakerStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmESTIntegritySpeakerStrobeRed
        initTabBlock( BlockFireAlarmESTIntegritySpeakerStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmESTIntegritySpeakerStrobeWhite
        initTabBlock( BlockFireAlarmESTPull.class, fmlPreInitializationEvent ); // FireAlarmESTPull
        initTabBlock( BlockFireAlarmEdwardsGlassRodPullStation.class,
                      fmlPreInitializationEvent ); // FireAlarmEdwardsGlassRodPullStation
        initTabBlock( BlockFireAlarmFCIPull.class, fmlPreInitializationEvent ); // FireAlarmFCIPull
        initTabBlock( BlockFireAlarmFireLiteBG12.class, fmlPreInitializationEvent ); // FireAlarmFireLiteBG12
        initTabBlock( BlockFireAlarmFireLiteBG6.class, fmlPreInitializationEvent ); // FireAlarmFireLiteBG6
        initTabBlock( BlockFireAlarmFireLiteBG8.class, fmlPreInitializationEvent ); // FireAlarmFireLiteBG8
        initTabBlock( BlockFireAlarmGamewellCenturyPull.class,
                      fmlPreInitializationEvent ); // FireAlarmGamewellCenturyPull
        initTabBlock( BlockFireAlarmGenericPullStation.class,
                      fmlPreInitializationEvent ); // FireAlarmGenericPullStation
        initTabBlock( BlockFireAlarmGentexCommander3Red.class,
                      fmlPreInitializationEvent ); // FireAlarmGentexCommander3Red
        initTabBlock( BlockFireAlarmGentexCommander3White.class,
                      fmlPreInitializationEvent ); // FireAlarmGentexCommander3White
        initTabBlock( BlockFireAlarmHeatDetector.class, fmlPreInitializationEvent ); // FireAlarmHeatDetector
        initTabBlock( BlockFireAlarmHoneywellAddressableModule.class,
                      fmlPreInitializationEvent ); // FireAlarmHoneywellAddressableModule
        initTabBlock( BlockFireAlarmKACCallPoint.class, fmlPreInitializationEvent ); // FireAlarmKACCallPoint
        initTabBlock( BlockFireAlarmKACSounderRed.class, fmlPreInitializationEvent ); // FireAlarmKACSounderRed
        initTabBlock( BlockFireAlarmKiddeSmokeDetector.class,
                      fmlPreInitializationEvent ); // FireAlarmKiddeSmokeDetector
        initTabBlock( BlockFireAlarmNestProtectGen2.class, fmlPreInitializationEvent ); // FireAlarmNestProtectGen2
        initTabBlock( BlockFireAlarmSimplex2901HornRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplex2901HornRed
        initTabBlock( BlockFireAlarmSimplex2901HornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplex2901HornStrobeRed
        initTabBlock( BlockFireAlarmSimplex4050Red.class, fmlPreInitializationEvent ); // FireAlarmSimplex4050Red
        initTabBlock( BlockFireAlarmSimplex4051Red.class, fmlPreInitializationEvent ); // FireAlarmSimplex4051Red
        initTabBlock( BlockFireAlarmSimplex4903HornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplex4903HornStrobeRed
        initTabBlock( BlockFireAlarmSimplex4903HornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplex4903HornStrobeWhite
        initTabBlock( BlockFireAlarmSimplex4903SpeakerStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplex4903SpeakerStrobeRed
        initTabBlock( BlockFireAlarmSimplex4903SpeakerStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplex4903SpeakerStrobeWhite
        initTabBlock( BlockFireAlarmSimplexChevronPull.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexChevronPull
        initTabBlock( BlockFireAlarmSimplexTBarPull.class, fmlPreInitializationEvent ); // FireAlarmSimplexTBarPull
        initTabBlock( BlockFireAlarmSimplexTrueAlertHornRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertHornRed
        initTabBlock( BlockFireAlarmSimplexTrueAlertHornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertHornStrobeRed
        initTabBlock( BlockFireAlarmSimplexTrueAlertHornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertHornStrobeWhite
        initTabBlock( BlockFireAlarmSimplexTrueAlertHornWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertHornWhite
        initTabBlock( BlockFireAlarmSimplexTrueAlertLEDRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertLEDRed
        initTabBlock( BlockFireAlarmSimplexTrueAlertSpeakerRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertSpeakerRed
        initTabBlock( BlockFireAlarmSimplexTrueAlertSpeakerStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertSpeakerStrobeRed
        initTabBlock( BlockFireAlarmSimplexTrueAlertSpeakerStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertSpeakerStrobeWhite
        initTabBlock( BlockFireAlarmSimplexTrueAlertSpeakerWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSimplexTrueAlertSpeakerWhite
        initTabBlock( BlockFireAlarmSpaceAgeAV32Red.class, fmlPreInitializationEvent ); // FireAlarmSpaceAgeAV32Red
        initTabBlock( BlockFireAlarmSprinklerBlack.class, fmlPreInitializationEvent ); // FireAlarmSprinklerBlack
        initTabBlock( BlockFireAlarmSprinklerSilver.class, fmlPreInitializationEvent ); // FireAlarmSprinklerSilver
        initTabBlock( BlockFireAlarmSprinklerWhite.class, fmlPreInitializationEvent ); // FireAlarmSprinklerWhite
        initTabBlock( BlockFireAlarmSystemSensorAdvanceCeilingHornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceCeilingHornStrobeRed
        initTabBlock( BlockFireAlarmSystemSensorAdvanceCeilingHornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceCeilingHornStrobeWhite
        initTabBlock( BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceHornStrobeOutdoorRed
        initTabBlock( BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceHornStrobeOutdoorWhite
        initTabBlock( BlockFireAlarmSystemSensorAdvanceHornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceHornStrobeRed
        initTabBlock( BlockFireAlarmSystemSensorAdvanceHornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceHornStrobeWhite
        initTabBlock( BlockFireAlarmSystemSensorAdvanceSpeakerStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceSpeakerStrobeRed
        initTabBlock( BlockFireAlarmSystemSensorAdvanceSpeakerStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorAdvanceSpeakerStrobeWhite
        initTabBlock( BlockFireAlarmSystemSensorLSeriesCeilingHornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesCeilingHornStrobeRed
        initTabBlock( BlockFireAlarmSystemSensorLSeriesCeilingHornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesCeilingHornStrobeWhite
        initTabBlock( BlockFireAlarmSystemSensorLSeriesCeilingSpeakerRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesCeilingSpeakerRed
        initTabBlock( BlockFireAlarmSystemSensorLSeriesCeilingSpeakerStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesCeilingSpeakerStrobeRed
        initTabBlock( BlockFireAlarmSystemSensorLSeriesCeilingSpeakerStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesCeilingSpeakerStrobeWhite
        initTabBlock( BlockFireAlarmSystemSensorLSeriesCeilingSpeakerWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesCeilingSpeakerWhite
        initTabBlock( BlockFireAlarmSystemSensorLSeriesHornRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesHornRed
        initTabBlock( BlockFireAlarmSystemSensorLSeriesHornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesHornStrobeRed
        initTabBlock( BlockFireAlarmSystemSensorLSeriesHornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesHornStrobeWhite
        initTabBlock( BlockFireAlarmSystemSensorLSeriesHornWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesHornWhite
        initTabBlock( BlockFireAlarmSystemSensorLSeriesSpeakerRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesSpeakerRed
        initTabBlock( BlockFireAlarmSystemSensorLSeriesSpeakerStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesSpeakerStrobeRed
        initTabBlock( BlockFireAlarmSystemSensorLSeriesSpeakerStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesSpeakerStrobeWhite
        initTabBlock( BlockFireAlarmSystemSensorLSeriesSpeakerWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmSystemSensorLSeriesSpeakerWhite
        initTabBlock( BlockFireAlarmWheelock7002TRed.class, fmlPreInitializationEvent ); // FireAlarmWheelock7002TRed
        initTabBlock( BlockFireAlarmWheelockASRed.class, fmlPreInitializationEvent ); // FireAlarmWheelockASRed
        initTabBlock( BlockFireAlarmWheelockASWhite.class, fmlPreInitializationEvent ); // FireAlarmWheelockASWhite
        initTabBlock( BlockFireAlarmWheelockE50Red.class, fmlPreInitializationEvent ); // FireAlarmWheelockE50Red
        initTabBlock( BlockFireAlarmWheelockE50White.class, fmlPreInitializationEvent ); // FireAlarmWheelockE50White
        initTabBlock( BlockFireAlarmWheelockE60White.class, fmlPreInitializationEvent ); // FireAlarmWheelockE60White
        initTabBlock( BlockFireAlarmWheelockE70ChimeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockE70ChimeRed
        initTabBlock( BlockFireAlarmWheelockE70ChimeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockE70ChimeWhite
        initTabBlock( BlockFireAlarmWheelockE70Red.class, fmlPreInitializationEvent ); // FireAlarmWheelockE70Red
        initTabBlock( BlockFireAlarmWheelockE70SpeakerRed.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockE70SpeakerRed
        initTabBlock( BlockFireAlarmWheelockE70SpeakerWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockE70SpeakerWhite
        initTabBlock( BlockFireAlarmWheelockE70White.class, fmlPreInitializationEvent ); // FireAlarmWheelockE70White
        initTabBlock( BlockFireAlarmWheelockET70Red.class, fmlPreInitializationEvent ); // FireAlarmWheelockET70Red
        initTabBlock( BlockFireAlarmWheelockET70WPRed.class, fmlPreInitializationEvent ); // FireAlarmWheelockET70WPRed
        initTabBlock( BlockFireAlarmWheelockET70WPWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockET70WPWhite
        initTabBlock( BlockFireAlarmWheelockET70White.class, fmlPreInitializationEvent ); // FireAlarmWheelockET70White
        initTabBlock( BlockFireAlarmWheelockET80Red.class, fmlPreInitializationEvent ); // FireAlarmWheelockET80Red
        initTabBlock( BlockFireAlarmWheelockExceederRed.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockExceederRed
        initTabBlock( BlockFireAlarmWheelockExceederWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockExceederWhite
        initTabBlock( BlockFireAlarmWheelockMTHornRed.class, fmlPreInitializationEvent ); // FireAlarmWheelockMTHornRed
        initTabBlock( BlockFireAlarmWheelockMTHornStrobeRed.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockMTHornStrobeRed
        initTabBlock( BlockFireAlarmWheelockMTHornStrobeWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockMTHornStrobeWhite
        initTabBlock( BlockFireAlarmWheelockMTHornStrobeWhiteBlue.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockMTHornStrobeWhiteBlue
        initTabBlock( BlockFireAlarmWheelockMTHornWhite.class,
                      fmlPreInitializationEvent ); // FireAlarmWheelockMTHornWhite
        initTabBlock( BlockFireAlarmWheelockRSSStrobe.class, fmlPreInitializationEvent ); // FireAlarmWheelockRSSStrobe
        initTabBlock( BlockGamewellFireBox.class, fmlPreInitializationEvent ); // GamewellFireBox
        initTabBlock( BlockGreenManExitSignDownArrow.class, fmlPreInitializationEvent ); // GreenManExitSignDownArrow
        initTabBlock( BlockGreenManExitSignDownArrowSingleSided.class,
                      fmlPreInitializationEvent ); // GreenManExitSignDownArrowSingleSided
        initTabBlock( BlockGreenManExitSignLeftArrow.class, fmlPreInitializationEvent ); // GreenManExitSignLeftArrow
        initTabBlock( BlockGreenManExitSignRightArrow.class, fmlPreInitializationEvent ); // GreenManExitSignRightArrow
        initTabBlock( BlockGreenManExitSignUpLeftArrow.class,
                      fmlPreInitializationEvent ); // GreenManExitSignUpLeftArrow
        initTabBlock( BlockGreenManExitSignUpRightArrow.class,
                      fmlPreInitializationEvent ); // GreenManExitSignUpRightArrow
        initTabBlock( BlockMCLACodeApprovedExitSign.class, fmlPreInitializationEvent ); // MCLACodeApprovedExitSign
        initTabBlock( BlockMCLACodeApprovedExitSignDual.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignDual
        initTabBlock( BlockMCLACodeApprovedExitSignDualISA.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignDualISA
        initTabBlock( BlockMCLACodeApprovedExitSignISA.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignISA
        initTabBlock( BlockMCLACodeApprovedExitSignISASingleSided.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignISASingleSided
        initTabBlock( BlockMCLACodeApprovedExitSignLeft.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignLeft
        initTabBlock( BlockMCLACodeApprovedExitSignLeftISA.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignLeftISA
        initTabBlock( BlockMCLACodeApprovedExitSignRight.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignRight
        initTabBlock( BlockMCLACodeApprovedExitSignRightISA.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedExitSignRightISA
        initTabBlock( BlockMCLACodeApprovedStairsSign.class, fmlPreInitializationEvent ); // MCLACodeApprovedStairsSign
        initTabBlock( BlockMCLACodeApprovedStairsSignDual.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedStairsSignDual
        initTabBlock( BlockMCLACodeApprovedStairsSignLeft.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedStairsSignLeft
        initTabBlock( BlockMCLACodeApprovedStairsSignRight.class,
                      fmlPreInitializationEvent ); // MCLACodeApprovedStairsSignRight
        initTabBlock( BlockOldFireSprinkler.class, fmlPreInitializationEvent ); // OldFireSprinkler
        initTabBlock( BlockOldFireSprinkler2.class, fmlPreInitializationEvent ); // OldFireSprinkler2
        initTabBlock( BlockOldFireSprinkler3.class, fmlPreInitializationEvent ); // OldFireSprinkler3
        initTabBlock( BlockOldFireSprinkler4.class, fmlPreInitializationEvent ); // OldFireSprinkler4
        initTabBlock( BlockOldFireSprinkler5.class, fmlPreInitializationEvent ); // OldFireSprinkler5
        initTabBlock( BlockOldFireSprinkler6.class, fmlPreInitializationEvent ); // OldFireSprinkler6
        initTabBlock( BlockSslstrobe.class, fmlPreInitializationEvent ); // Sslstrobe
        initTabBlock( BlockStairsSignOneSided.class, fmlPreInitializationEvent ); // StairsSignOneSided
        initTabItem( ItemFireAlarmLinker.class, fmlPreInitializationEvent ); // FireAlarmLinker
    }
}

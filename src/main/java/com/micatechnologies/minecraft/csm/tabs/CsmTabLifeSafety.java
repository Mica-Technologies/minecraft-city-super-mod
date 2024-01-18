package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEdwardsEmergencyPhone;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockExitSignSingleSided;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmControlPanel;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTAdaptahornGray;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTAdaptahornRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTGenesisRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTGenesisWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTIntegrityHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTIntegrityHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTIntegritySpeakerStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTIntegritySpeakerStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmESTPull;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmEdwardsGlassRodPullStation;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmFCIPull;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmFireLiteBG12;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmFireLiteBG6;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmFireLiteBG8;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmGamewellCenturyPull;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmGenericPullStation;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmGentexCommander3Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmGentexCommander3White;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmHeatDetector;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmHoneywellAddressableModule;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmKACCallPoint;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmKACSounderRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmKiddeSmokeDetector;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmNestProtectGen2;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex2901HornRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex2901HornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4050Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4051Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4903HornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4903HornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4903SpeakerStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4903SpeakerStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexChevronPull;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTBarPull;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertHornRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertHornWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertLEDRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertSpeakerRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertSpeakerStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertSpeakerStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTrueAlertSpeakerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSpaceAgeAV32Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerSilver;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceCeilingHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceCeilingHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceSpeakerStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorAdvanceSpeakerStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesCeilingHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesCeilingHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesCeilingSpeakerRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesCeilingSpeakerStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesCeilingSpeakerStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesCeilingSpeakerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesHornRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesHornWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesSpeakerRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesSpeakerStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesSpeakerStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSystemSensorLSeriesSpeakerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelock7002TRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockASRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockASWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE50Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE50White;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE60White;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE70ChimeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE70ChimeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE70Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE70SpeakerRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE70SpeakerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockE70White;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockET70Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockET70WPRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockET70WPWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockET70White;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockET80Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockExceederRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockExceederWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornStrobeWhiteBlue;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockRSSStrobe;
import com.micatechnologies.minecraft.csm.lifesafety.BlockGamewellFireBox;
import com.micatechnologies.minecraft.csm.lifesafety.BlockGreenManExitSignDownArrow;
import com.micatechnologies.minecraft.csm.lifesafety.BlockGreenManExitSignDownArrowSingleSided;
import com.micatechnologies.minecraft.csm.lifesafety.BlockGreenManExitSignLeftArrow;
import com.micatechnologies.minecraft.csm.lifesafety.BlockGreenManExitSignRightArrow;
import com.micatechnologies.minecraft.csm.lifesafety.BlockGreenManExitSignUpLeftArrow;
import com.micatechnologies.minecraft.csm.lifesafety.BlockGreenManExitSignUpRightArrow;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSign;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignDual;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignDualISA;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignISA;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignISASingleSided;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignLeft;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignLeftISA;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignRight;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignRightISA;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedStairsSign;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedStairsSignDual;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedStairsSignLeft;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedStairsSignRight;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler2;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler3;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler4;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler5;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler6;
import com.micatechnologies.minecraft.csm.lifesafety.BlockSslstrobe;
import com.micatechnologies.minecraft.csm.lifesafety.BlockStairsSignOneSided;
import com.micatechnologies.minecraft.csm.lifesafety.ItemFireAlarmLinker;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for life safety blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 3)
public class CsmTabLifeSafety extends CsmTab {

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
    return CsmRegistry.getBlock("mclacodeapprovedexitsignisa");
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
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockEdwardsEmergencyPhone.class,
        fmlPreInitializationEvent); // EdwardsEmergencyPhone
    initTabBlock(BlockEmergencyLightWhite.class, fmlPreInitializationEvent); // Elight
    initTabBlock(BlockEmergencyLightBlack.class, fmlPreInitializationEvent); // ElightBlack
    initTabBlock(BlockExitSignSingleSided.class, fmlPreInitializationEvent); // ExitSignSingleSided
    initTabBlock(BlockFireAlarmControlPanel.class,
        fmlPreInitializationEvent); // FireAlarmControlPanel
    initTabBlock(BlockFireAlarmESTAdaptahornGray.class,
        fmlPreInitializationEvent); // FireAlarmESTAdaptahornGray
    initTabBlock(BlockFireAlarmESTAdaptahornRed.class,
        fmlPreInitializationEvent); // FireAlarmESTAdaptahornRed
    initTabBlock(BlockFireAlarmESTGenesisRed.class,
        fmlPreInitializationEvent); // FireAlarmESTGenesisRed
    initTabBlock(BlockFireAlarmESTGenesisWhite.class,
        fmlPreInitializationEvent); // FireAlarmESTGenesisWhite
    initTabBlock(BlockFireAlarmESTIntegrityHornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmESTIntegrityHornStrobeRed
    initTabBlock(BlockFireAlarmESTIntegrityHornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmESTIntegrityHornStrobeWhite
    initTabBlock(BlockFireAlarmESTIntegritySpeakerStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmESTIntegritySpeakerStrobeRed
    initTabBlock(BlockFireAlarmESTIntegritySpeakerStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmESTIntegritySpeakerStrobeWhite
    initTabBlock(BlockFireAlarmESTPull.class, fmlPreInitializationEvent); // FireAlarmESTPull
    initTabBlock(BlockFireAlarmEdwardsGlassRodPullStation.class,
        fmlPreInitializationEvent); // FireAlarmEdwardsGlassRodPullStation
    initTabBlock(BlockFireAlarmFCIPull.class, fmlPreInitializationEvent); // FireAlarmFCIPull
    initTabBlock(BlockFireAlarmFireLiteBG12.class,
        fmlPreInitializationEvent); // FireAlarmFireLiteBG12
    initTabBlock(BlockFireAlarmFireLiteBG6.class,
        fmlPreInitializationEvent); // FireAlarmFireLiteBG6
    initTabBlock(BlockFireAlarmFireLiteBG8.class,
        fmlPreInitializationEvent); // FireAlarmFireLiteBG8
    initTabBlock(BlockFireAlarmGamewellCenturyPull.class,
        fmlPreInitializationEvent); // FireAlarmGamewellCenturyPull
    initTabBlock(BlockFireAlarmGenericPullStation.class,
        fmlPreInitializationEvent); // FireAlarmGenericPullStation
    initTabBlock(BlockFireAlarmGentexCommander3Red.class,
        fmlPreInitializationEvent); // FireAlarmGentexCommander3Red
    initTabBlock(BlockFireAlarmGentexCommander3White.class,
        fmlPreInitializationEvent); // FireAlarmGentexCommander3White
    initTabBlock(BlockFireAlarmHeatDetector.class,
        fmlPreInitializationEvent); // FireAlarmHeatDetector
    initTabBlock(BlockFireAlarmHoneywellAddressableModule.class,
        fmlPreInitializationEvent); // FireAlarmHoneywellAddressableModule
    initTabBlock(BlockFireAlarmKACCallPoint.class,
        fmlPreInitializationEvent); // FireAlarmKACCallPoint
    initTabBlock(BlockFireAlarmKACSounderRed.class,
        fmlPreInitializationEvent); // FireAlarmKACSounderRed
    initTabBlock(BlockFireAlarmKiddeSmokeDetector.class,
        fmlPreInitializationEvent); // FireAlarmKiddeSmokeDetector
    initTabBlock(BlockFireAlarmNestProtectGen2.class,
        fmlPreInitializationEvent); // FireAlarmNestProtectGen2
    initTabBlock(BlockFireAlarmSimplex2901HornRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplex2901HornRed
    initTabBlock(BlockFireAlarmSimplex2901HornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplex2901HornStrobeRed
    initTabBlock(BlockFireAlarmSimplex4050Red.class,
        fmlPreInitializationEvent); // FireAlarmSimplex4050Red
    initTabBlock(BlockFireAlarmSimplex4051Red.class,
        fmlPreInitializationEvent); // FireAlarmSimplex4051Red
    initTabBlock(BlockFireAlarmSimplex4903HornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplex4903HornStrobeRed
    initTabBlock(BlockFireAlarmSimplex4903HornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSimplex4903HornStrobeWhite
    initTabBlock(BlockFireAlarmSimplex4903SpeakerStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplex4903SpeakerStrobeRed
    initTabBlock(BlockFireAlarmSimplex4903SpeakerStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSimplex4903SpeakerStrobeWhite
    initTabBlock(BlockFireAlarmSimplexChevronPull.class,
        fmlPreInitializationEvent); // FireAlarmSimplexChevronPull
    initTabBlock(BlockFireAlarmSimplexTBarPull.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTBarPull
    initTabBlock(BlockFireAlarmSimplexTrueAlertHornRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertHornRed
    initTabBlock(BlockFireAlarmSimplexTrueAlertHornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertHornStrobeRed
    initTabBlock(BlockFireAlarmSimplexTrueAlertHornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertHornStrobeWhite
    initTabBlock(BlockFireAlarmSimplexTrueAlertHornWhite.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertHornWhite
    initTabBlock(BlockFireAlarmSimplexTrueAlertLEDRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertLEDRed
    initTabBlock(BlockFireAlarmSimplexTrueAlertSpeakerRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertSpeakerRed
    initTabBlock(BlockFireAlarmSimplexTrueAlertSpeakerStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertSpeakerStrobeRed
    initTabBlock(BlockFireAlarmSimplexTrueAlertSpeakerStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertSpeakerStrobeWhite
    initTabBlock(BlockFireAlarmSimplexTrueAlertSpeakerWhite.class,
        fmlPreInitializationEvent); // FireAlarmSimplexTrueAlertSpeakerWhite
    initTabBlock(BlockFireAlarmSpaceAgeAV32Red.class,
        fmlPreInitializationEvent); // FireAlarmSpaceAgeAV32Red
    initTabBlock(BlockFireAlarmSprinklerBlack.class,
        fmlPreInitializationEvent); // FireAlarmSprinklerBlack
    initTabBlock(BlockFireAlarmSprinklerSilver.class,
        fmlPreInitializationEvent); // FireAlarmSprinklerSilver
    initTabBlock(BlockFireAlarmSprinklerWhite.class,
        fmlPreInitializationEvent); // FireAlarmSprinklerWhite
    initTabBlock(BlockFireAlarmSystemSensorAdvanceCeilingHornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceCeilingHornStrobeRed
    initTabBlock(BlockFireAlarmSystemSensorAdvanceCeilingHornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceCeilingHornStrobeWhite
    initTabBlock(BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceHornStrobeOutdoorRed
    initTabBlock(BlockFireAlarmSystemSensorAdvanceHornStrobeOutdoorWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceHornStrobeOutdoorWhite
    initTabBlock(BlockFireAlarmSystemSensorAdvanceHornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceHornStrobeRed
    initTabBlock(BlockFireAlarmSystemSensorAdvanceHornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceHornStrobeWhite
    initTabBlock(BlockFireAlarmSystemSensorAdvanceSpeakerStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceSpeakerStrobeRed
    initTabBlock(BlockFireAlarmSystemSensorAdvanceSpeakerStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorAdvanceSpeakerStrobeWhite
    initTabBlock(BlockFireAlarmSystemSensorLSeriesCeilingHornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesCeilingHornStrobeRed
    initTabBlock(BlockFireAlarmSystemSensorLSeriesCeilingHornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesCeilingHornStrobeWhite
    initTabBlock(BlockFireAlarmSystemSensorLSeriesCeilingSpeakerRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesCeilingSpeakerRed
    initTabBlock(BlockFireAlarmSystemSensorLSeriesCeilingSpeakerStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesCeilingSpeakerStrobeRed
    initTabBlock(BlockFireAlarmSystemSensorLSeriesCeilingSpeakerStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesCeilingSpeakerStrobeWhite
    initTabBlock(BlockFireAlarmSystemSensorLSeriesCeilingSpeakerWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesCeilingSpeakerWhite
    initTabBlock(BlockFireAlarmSystemSensorLSeriesHornRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesHornRed
    initTabBlock(BlockFireAlarmSystemSensorLSeriesHornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesHornStrobeRed
    initTabBlock(BlockFireAlarmSystemSensorLSeriesHornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesHornStrobeWhite
    initTabBlock(BlockFireAlarmSystemSensorLSeriesHornWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesHornWhite
    initTabBlock(BlockFireAlarmSystemSensorLSeriesSpeakerRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesSpeakerRed
    initTabBlock(BlockFireAlarmSystemSensorLSeriesSpeakerStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesSpeakerStrobeRed
    initTabBlock(BlockFireAlarmSystemSensorLSeriesSpeakerStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesSpeakerStrobeWhite
    initTabBlock(BlockFireAlarmSystemSensorLSeriesSpeakerWhite.class,
        fmlPreInitializationEvent); // FireAlarmSystemSensorLSeriesSpeakerWhite
    initTabBlock(BlockFireAlarmWheelock7002TRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelock7002TRed
    initTabBlock(BlockFireAlarmWheelockASRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelockASRed
    initTabBlock(BlockFireAlarmWheelockASWhite.class,
        fmlPreInitializationEvent); // FireAlarmWheelockASWhite
    initTabBlock(BlockFireAlarmWheelockE50Red.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE50Red
    initTabBlock(BlockFireAlarmWheelockE50White.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE50White
    initTabBlock(BlockFireAlarmWheelockE60White.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE60White
    initTabBlock(BlockFireAlarmWheelockE70ChimeRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE70ChimeRed
    initTabBlock(BlockFireAlarmWheelockE70ChimeWhite.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE70ChimeWhite
    initTabBlock(BlockFireAlarmWheelockE70Red.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE70Red
    initTabBlock(BlockFireAlarmWheelockE70SpeakerRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE70SpeakerRed
    initTabBlock(BlockFireAlarmWheelockE70SpeakerWhite.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE70SpeakerWhite
    initTabBlock(BlockFireAlarmWheelockE70White.class,
        fmlPreInitializationEvent); // FireAlarmWheelockE70White
    initTabBlock(BlockFireAlarmWheelockET70Red.class,
        fmlPreInitializationEvent); // FireAlarmWheelockET70Red
    initTabBlock(BlockFireAlarmWheelockET70WPRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelockET70WPRed
    initTabBlock(BlockFireAlarmWheelockET70WPWhite.class,
        fmlPreInitializationEvent); // FireAlarmWheelockET70WPWhite
    initTabBlock(BlockFireAlarmWheelockET70White.class,
        fmlPreInitializationEvent); // FireAlarmWheelockET70White
    initTabBlock(BlockFireAlarmWheelockET80Red.class,
        fmlPreInitializationEvent); // FireAlarmWheelockET80Red
    initTabBlock(BlockFireAlarmWheelockExceederRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelockExceederRed
    initTabBlock(BlockFireAlarmWheelockExceederWhite.class,
        fmlPreInitializationEvent); // FireAlarmWheelockExceederWhite
    initTabBlock(BlockFireAlarmWheelockMTHornRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelockMTHornRed
    initTabBlock(BlockFireAlarmWheelockMTHornStrobeRed.class,
        fmlPreInitializationEvent); // FireAlarmWheelockMTHornStrobeRed
    initTabBlock(BlockFireAlarmWheelockMTHornStrobeWhite.class,
        fmlPreInitializationEvent); // FireAlarmWheelockMTHornStrobeWhite
    initTabBlock(BlockFireAlarmWheelockMTHornStrobeWhiteBlue.class,
        fmlPreInitializationEvent); // FireAlarmWheelockMTHornStrobeWhiteBlue
    initTabBlock(BlockFireAlarmWheelockMTHornWhite.class,
        fmlPreInitializationEvent); // FireAlarmWheelockMTHornWhite
    initTabBlock(BlockFireAlarmWheelockRSSStrobe.class,
        fmlPreInitializationEvent); // FireAlarmWheelockRSSStrobe
    initTabBlock(BlockGamewellFireBox.class, fmlPreInitializationEvent); // GamewellFireBox
    initTabBlock(BlockGreenManExitSignDownArrow.class,
        fmlPreInitializationEvent); // GreenManExitSignDownArrow
    initTabBlock(BlockGreenManExitSignDownArrowSingleSided.class,
        fmlPreInitializationEvent); // GreenManExitSignDownArrowSingleSided
    initTabBlock(BlockGreenManExitSignLeftArrow.class,
        fmlPreInitializationEvent); // GreenManExitSignLeftArrow
    initTabBlock(BlockGreenManExitSignRightArrow.class,
        fmlPreInitializationEvent); // GreenManExitSignRightArrow
    initTabBlock(BlockGreenManExitSignUpLeftArrow.class,
        fmlPreInitializationEvent); // GreenManExitSignUpLeftArrow
    initTabBlock(BlockGreenManExitSignUpRightArrow.class,
        fmlPreInitializationEvent); // GreenManExitSignUpRightArrow
    initTabBlock(BlockMCLACodeApprovedExitSign.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSign
    initTabBlock(BlockMCLACodeApprovedExitSignDual.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignDual
    initTabBlock(BlockMCLACodeApprovedExitSignDualISA.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignDualISA
    initTabBlock(BlockMCLACodeApprovedExitSignISA.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignISA
    initTabBlock(BlockMCLACodeApprovedExitSignISASingleSided.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignISASingleSided
    initTabBlock(BlockMCLACodeApprovedExitSignLeft.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignLeft
    initTabBlock(BlockMCLACodeApprovedExitSignLeftISA.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignLeftISA
    initTabBlock(BlockMCLACodeApprovedExitSignRight.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignRight
    initTabBlock(BlockMCLACodeApprovedExitSignRightISA.class,
        fmlPreInitializationEvent); // MCLACodeApprovedExitSignRightISA
    initTabBlock(BlockMCLACodeApprovedStairsSign.class,
        fmlPreInitializationEvent); // MCLACodeApprovedStairsSign
    initTabBlock(BlockMCLACodeApprovedStairsSignDual.class,
        fmlPreInitializationEvent); // MCLACodeApprovedStairsSignDual
    initTabBlock(BlockMCLACodeApprovedStairsSignLeft.class,
        fmlPreInitializationEvent); // MCLACodeApprovedStairsSignLeft
    initTabBlock(BlockMCLACodeApprovedStairsSignRight.class,
        fmlPreInitializationEvent); // MCLACodeApprovedStairsSignRight
    initTabBlock(BlockOldFireSprinkler.class, fmlPreInitializationEvent); // OldFireSprinkler
    initTabBlock(BlockOldFireSprinkler2.class, fmlPreInitializationEvent); // OldFireSprinkler2
    initTabBlock(BlockOldFireSprinkler3.class, fmlPreInitializationEvent); // OldFireSprinkler3
    initTabBlock(BlockOldFireSprinkler4.class, fmlPreInitializationEvent); // OldFireSprinkler4
    initTabBlock(BlockOldFireSprinkler5.class, fmlPreInitializationEvent); // OldFireSprinkler5
    initTabBlock(BlockOldFireSprinkler6.class, fmlPreInitializationEvent); // OldFireSprinkler6
    initTabBlock(BlockSslstrobe.class, fmlPreInitializationEvent); // Sslstrobe
    initTabBlock(BlockStairsSignOneSided.class, fmlPreInitializationEvent); // StairsSignOneSided
    initTabItem(ItemFireAlarmLinker.class, fmlPreInitializationEvent); // FireAlarmLinker
  }
}

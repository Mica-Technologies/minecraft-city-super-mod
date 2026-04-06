package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockEmergencyLightWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmControlPanel;
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
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmKACCallPoint;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmKACSounderRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4050Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4051Red;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4903HornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplex4903HornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexChevronPull;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSimplexTBarPull;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSounderFactory;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSounderStrobeFactory;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerSilver;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmVoiceEvacFactory;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmVoiceEvacStrobeFactory;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelock7002TRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockASRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockASWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockExceederRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockExceederWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornStrobeRed;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornStrobeWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornStrobeWhiteBlue;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmWheelockMTHornWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler2;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler3;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler4;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler5;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler6;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler;
import com.micatechnologies.minecraft.csm.lifesafety.ItemFireAlarmConfigTool;
import com.micatechnologies.minecraft.csm.lifesafety.ItemFireAlarmLinker;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
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
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(new BlockRotatableNSEWUDFactory("eep", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.125000, 0.062500, 0.937500, 0.875000, 0.937500, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(BlockEmergencyLightWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockEmergencyLightBlack.class, fmlPreInitializationEvent);
    initTabBlock(new BlockRotatableNSEWUDFactory("exitsignsinglesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(BlockFireAlarmControlPanel.class, fmlPreInitializationEvent);
    initTabBlock(new BlockFireAlarmSounderFactory("firealarmestadaptahorngray", "csm:edwards_adaptahorn_code44", new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderFactory("firealarmestadaptahornred", "csm:edwards_adaptahorn_code44", new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmestgenesisred", "csm:est_genesis", new AxisAlignedBB(0.312500, 0.375000, 0.937500, 0.687500, 1.000000, 1.000000), new float[]{5f, 7f, 14.25f}, new float[]{11f, 9f, 15f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmestgenesiswhite", "csm:est_genesis", new AxisAlignedBB(0.312500, 0.375000, 0.937500, 0.687500, 1.000000, 1.000000), new float[]{5f, 7f, 14.25f}, new float[]{11f, 9f, 15f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmestintegrityhornstrobered", "csm:est_integrity", new AxisAlignedBB(0.187500, 0.312500, 0.687500, 0.875000, 1.000000, 1.000000), new float[]{7f, 6f, 11f}, new float[]{10f, 15f, 14f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmestintegrityhornstrobewhite", "csm:est_integrity", new AxisAlignedBB(0.187500, 0.312500, 0.687500, 0.875000, 1.000000, 1.000000), new float[]{7f, 6f, 11f}, new float[]{10f, 15f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmestintegrityspeakerstrobered", new AxisAlignedBB(0.187500, 0.312500, 0.687500, 0.875000, 1.000000, 1.000000), new float[]{7f, 6f, 11f}, new float[]{10f, 15f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmestintegrityspeakerstrobewhite", new AxisAlignedBB(0.187500, 0.312500, 0.687500, 0.875000, 1.000000, 1.000000), new float[]{7f, 6f, 11f}, new float[]{10f, 15f, 14f}));
    initTabBlock(BlockFireAlarmESTPull.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmEdwardsGlassRodPullStation.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmFCIPull.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmFireLiteBG12.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmFireLiteBG6.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmFireLiteBG8.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmGamewellCenturyPull.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmGenericPullStation.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmGentexCommander3Red.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmGentexCommander3White.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmHeatDetector.class, fmlPreInitializationEvent);
    initTabBlock(new BlockRotatableNSEWUDFactory("hwam", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.125000, 0.312500, 0.937500, 0.875000, 0.937500, 1.000000), false, false, true, BlockRenderLayer.SOLID, false, false));
    initTabBlock(BlockFireAlarmKACCallPoint.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmKACSounderRed.class, fmlPreInitializationEvent);
    initTabBlock(new BlockRotatableNSEWUDFactory("kiddesmoke", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, true, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("nestprotect", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0.1F, 0, new AxisAlignedBB(0.125000, 0.900000, 0.125000, 0.875000, 1.000000, 0.875000), false, false, true, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockFireAlarmSounderFactory("firealarmsimplex2901hornred", "csm:2910calcode", new AxisAlignedBB(0.187500, 0.312500, 0.875000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsimplex2901hornstrobered", "csm:2910calcode", new AxisAlignedBB(0.187500, 0.125000, 0.750000, 0.812500, 1.000000, 1.000000), new float[]{3.8f, 12.5f, 12f}, new float[]{12f, 15.5f, 14f}));
    initTabBlock(BlockFireAlarmSimplex4050Red.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSimplex4051Red.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSimplex4903HornStrobeRed.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSimplex4903HornStrobeWhite.class, fmlPreInitializationEvent);
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsimplex4903speakerstrobered", new AxisAlignedBB(0.062500, 0.437500, 0.687500, 0.937500, 1.000000, 1.000000), new float[]{1.3f, 7.3f, 11f}, new float[]{4.7f, 15.8f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsimplex4903speakerstrobewhite", new AxisAlignedBB(0.062500, 0.437500, 0.687500, 0.937500, 1.000000, 1.000000), new float[]{1.3f, 7.3f, 11f}, new float[]{4.7f, 15.8f, 14f}));
    initTabBlock(BlockFireAlarmSimplexChevronPull.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSimplexTBarPull.class, fmlPreInitializationEvent);
    initTabBlock(new BlockFireAlarmSounderFactory("firealarmsimplextruealerthornred", "csm:stahorn", new AxisAlignedBB(0.187500, 0.375000, 0.900000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsimplextruealerthornstrobered", "csm:stahorn", new AxisAlignedBB(0.125000, 0.187500, 0.812500, 0.875000, 1.000000, 1.000000), new float[]{2.7f, 5.5f, 13f}, new float[]{13.2f, 9.5f, 15f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsimplextruealerthornstrobewhite", "csm:stahorn", new AxisAlignedBB(0.125000, 0.187500, 0.812500, 0.875000, 1.000000, 1.000000), new float[]{2.7f, 5.5f, 13f}, new float[]{13.2f, 9.5f, 15f}));
    initTabBlock(new BlockFireAlarmSounderFactory("firealarmsimplextruealerthornwhite", "csm:stahorn", new AxisAlignedBB(0.187500, 0.375000, 0.900000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsimplextruealertledred", "csm:stahorn", new AxisAlignedBB(0.250000, 0.250000, 0.875000, 0.750000, 1.000000, 1.000000), new float[]{6.5f, 6f, 14f}, new float[]{9.5f, 8.5f, 15f}));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmsimplextruealertspeakerred", new AxisAlignedBB(0.187500, 0.375000, 0.900000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsimplextruealertspeakerstrobered", new AxisAlignedBB(0.125000, 0.187500, 0.812500, 0.875000, 1.000000, 1.000000), new float[]{2.7f, 5.5f, 13f}, new float[]{13.2f, 9.5f, 15f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsimplextruealertspeakerstrobewhite", new AxisAlignedBB(0.125000, 0.187500, 0.812500, 0.875000, 1.000000, 1.000000), new float[]{2.7f, 5.5f, 13f}, new float[]{13.2f, 9.5f, 15f}));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmsimplextruealertspeakerwhite", new AxisAlignedBB(0.187500, 0.375000, 0.900000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmspaceageav32red", "csm:sae_marchtime", new AxisAlignedBB(0.125000, 0.062500, 0.875000, 0.875000, 0.937500, 1.000000), new float[]{4.8f, 2.1f, 13.5f}, new float[]{11.4f, 5.4f, 14f}));
    initTabBlock(BlockFireAlarmSprinklerBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSprinklerSilver.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSprinklerWhite.class, fmlPreInitializationEvent);
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensoradvanceceilinghornstrobered", "csm:spectralert", new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000), new float[]{6f, 6f, 15f}, new float[]{10f, 10f, 16f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensoradvanceceilinghornstrobewhite", "csm:spectralert", new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000), new float[]{6f, 6f, 15f}, new float[]{10f, 10f, 16f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensoradvancehornstrobeoutdoorred", "csm:spectralert", new AxisAlignedBB(0.000000, 0.125000, 0.812500, 1.000000, 1.000000, 1.000000), new float[]{6f, 6.7f, 13f}, new float[]{10f, 11.2f, 14f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensoradvancehornstrobeoutdoorwhite", "csm:spectralert", new AxisAlignedBB(0.000000, 0.125000, 0.812500, 1.000000, 1.000000, 1.000000), new float[]{6f, 6.7f, 13f}, new float[]{10f, 11.2f, 14f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensoradvancehornstrobered", "csm:spectralert", new AxisAlignedBB(0.187500, 0.250000, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{6f, 6.7f, 13f}, new float[]{10f, 11.2f, 14f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensoradvancehornstrobewhite", "csm:spectralert", new AxisAlignedBB(0.187500, 0.250000, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{6f, 6.7f, 13f}, new float[]{10f, 11.2f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsystemsensoradvancespeakerstrobered", new AxisAlignedBB(0.187500, 0.250000, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{6f, 7.7f, 13f}, new float[]{10f, 12.2f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsystemsensoradvancespeakerstrobewhite", new AxisAlignedBB(0.187500, 0.250000, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{6f, 7.7f, 13f}, new float[]{10f, 12.2f, 14f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensorlseriesceilinghornstrobered", "csm:spectralert", new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000), new float[]{6f, 6f, 15f}, new float[]{10f, 10f, 16f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensorlseriesceilinghornstrobewhite", "csm:spectralert", new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000), new float[]{6f, 6f, 15f}, new float[]{10f, 10f, 16f}));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmsystemsensorlseriesceilingspeakerred", new AxisAlignedBB(0.062500, 0.062500, 0.900000, 0.937500, 0.937500, 1.000000)));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsystemsensorlseriesceilingspeakerstrobered", new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000), new float[]{6f, 6f, 15f}, new float[]{10f, 10f, 16f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsystemsensorlseriesceilingspeakerstrobewhite", new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000), new float[]{6f, 6f, 15f}, new float[]{10f, 10f, 16f}));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmsystemsensorlseriesceilingspeakerwhite", new AxisAlignedBB(0.062500, 0.062500, 0.900000, 0.937500, 0.937500, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderFactory("firealarmsystemsensorlserieshornred", "csm:spectralert", new AxisAlignedBB(0.187500, 0.250000, 0.875000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensorlserieshornstrobered", "csm:spectralert", new AxisAlignedBB(0.187500, 0.187500, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{5.2f, 5f, 13f}, new float[]{11.2f, 9f, 14f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmsystemsensorlserieshornstrobewhite", "csm:spectralert", new AxisAlignedBB(0.187500, 0.187500, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{5.2f, 5f, 13f}, new float[]{11.2f, 9f, 14f}));
    initTabBlock(new BlockFireAlarmSounderFactory("firealarmsystemsensorlserieshornwhite", "csm:spectralert", new AxisAlignedBB(0.187500, 0.250000, 0.875000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmsystemsensorlseriesspeakerred", new AxisAlignedBB(0.187500, 0.250000, 0.875000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsystemsensorlseriesspeakerstrobered", new AxisAlignedBB(0.187500, 0.187500, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{5f, 5.5f, 13f}, new float[]{11f, 9.5f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmsystemsensorlseriesspeakerstrobewhite", new AxisAlignedBB(0.187500, 0.187500, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{5f, 5.5f, 13f}, new float[]{11f, 9.5f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmsystemsensorlseriesspeakerwhite", new AxisAlignedBB(0.187500, 0.250000, 0.875000, 0.812500, 1.000000, 1.000000)));
    initTabBlock(BlockFireAlarmWheelock7002TRed.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockASRed.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockASWhite.class, fmlPreInitializationEvent);
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocke50red", new AxisAlignedBB(0.187500, 0.250000, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{3.5f, 13.3f, 13f}, new float[]{12.5f, 15.3f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocke50white", new AxisAlignedBB(0.187500, 0.250000, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{3.5f, 13.3f, 13f}, new float[]{12.5f, 15.3f, 14f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocke60white", new AxisAlignedBB(0.000000, 0.000000, 0.875000, 1.000000, 1.000000, 1.000000), new float[]{4f, 7f, 14f}, new float[]{12f, 9f, 16f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmwheelocke70chimered", "csm:et70_chime", new AxisAlignedBB(0.125000, 0.250000, 0.750000, 0.875000, 1.000000, 0.937500), new float[]{3f, 9f, 13f}, new float[]{13f, 12f, 15f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmwheelocke70chimewhite", "csm:et70_chime", new AxisAlignedBB(0.125000, 0.250000, 0.750000, 0.875000, 1.000000, 0.937500), new float[]{3f, 9f, 13f}, new float[]{13f, 12f, 15f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocke70red", new AxisAlignedBB(0.125000, 0.250000, 0.750000, 0.875000, 1.000000, 0.937500), new float[]{3f, 9f, 13f}, new float[]{13f, 12f, 15f}));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmwheelocke70speakerred", new AxisAlignedBB(0.125000, 0.250000, 0.937500, 0.875000, 1.000000, 1.000000)));
    initTabBlock(new BlockFireAlarmVoiceEvacFactory("firealarmwheelocke70speakerwhite", new AxisAlignedBB(0.125000, 0.250000, 0.875000, 0.875000, 1.000000, 0.937500)));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocke70white", new AxisAlignedBB(0.125000, 0.250000, 0.750000, 0.875000, 1.000000, 0.937500), new float[]{3f, 9f, 13f}, new float[]{13f, 12f, 15f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocket70red", new AxisAlignedBB(0.125000, 0.250000, 0.812500, 0.875000, 1.000000, 1.000000), new float[]{6.4f, 5f, 13f}, new float[]{9.4f, 15f, 15f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocket70wpred", new AxisAlignedBB(0.125000, 0.250000, 0.500000, 0.875000, 1.000000, 1.000000), new float[]{2.9f, 10.7f, 8f}, new float[]{12.9f, 13.7f, 12f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocket70wpwhite", new AxisAlignedBB(0.125000, 0.250000, 0.500000, 0.875000, 1.000000, 1.000000), new float[]{2.9f, 10.7f, 8f}, new float[]{12.9f, 13.7f, 12f}));
    initTabBlock(new BlockFireAlarmVoiceEvacStrobeFactory("firealarmwheelocket70white", new AxisAlignedBB(0.125000, 0.250000, 0.812500, 0.875000, 1.000000, 1.000000), new float[]{6.4f, 5f, 13f}, new float[]{9.4f, 15f, 15f}));
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("firealarmwheelocket80red", "csm:simplex_4051_marchtime", new AxisAlignedBB(0.125000, 0.250000, 0.687500, 0.875000, 1.000000, 1.000000), new float[]{6.6f, 5f, 11f}, new float[]{9.6f, 15f, 14f}));
    initTabBlock(BlockFireAlarmWheelockExceederRed.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockExceederWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockMTHornRed.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockMTHornStrobeRed.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockMTHornStrobeWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockMTHornStrobeWhiteBlue.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmWheelockMTHornWhite.class, fmlPreInitializationEvent);
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("rssstrobe", null, new AxisAlignedBB(0.187500, 0.375000, 0.750000, 0.812500, 1.000000, 1.000000), new float[]{3.4f, 9.7f, 12f}, new float[]{12.9f, 12.2f, 14f}));
    initTabBlock(new BlockRotatableNSEWUDFactory("gamewellfirebox", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsigndownarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsigndownarrowsinglesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignleftarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignrightarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignupleftarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("greenmanexitsignuprightarrow", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsign", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsigndual", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsigndualisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignisasinglesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignleft", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignleftisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignright", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedexitsignrightisa", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(-0.187500, 0.250000, 0.800000, 1.187500, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssign", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssigndual", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssignleft", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mclacodeapprovedstairssignright", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(BlockOldFireSprinkler.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler2.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler3.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler4.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler5.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler6.class, fmlPreInitializationEvent);
    initTabBlock(new BlockFireAlarmSounderStrobeFactory("sslstrobe", null, new AxisAlignedBB(0.187500, 0.187500, 0.812500, 0.812500, 1.000000, 1.000000), new float[]{5.2f, 4.6f, 13f}, new float[]{11.2f, 8.6f, 14f}));
    initTabBlock(new BlockRotatableNSEWUDFactory("stairssignonesided", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0.85F, 0, new AxisAlignedBB(0.000000, 0.250000, 0.812500, 1.000000, 1.062500, 1.000000), false, false, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabItem(ItemFireAlarmLinker.class, fmlPreInitializationEvent);
    initTabItem(ItemFireAlarmConfigTool.class, fmlPreInitializationEvent);
  }
}

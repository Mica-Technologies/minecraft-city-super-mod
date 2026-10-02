package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ItemDecorativeFactory;
import com.micatechnologies.minecraft.csm.technology.BlockImac;
import com.micatechnologies.minecraft.csm.technology.BlockImacPro;
import com.micatechnologies.minecraft.csm.technology.BlockMacBookPro;
import com.micatechnologies.minecraft.csm.technology.BlockDeskDeviceFactory;
import com.micatechnologies.minecraft.csm.technology.BlockSpeakerFactory;
import com.micatechnologies.minecraft.csm.technology.ItemApplePencil;
import com.micatechnologies.minecraft.csm.technology.school.BlockBellController;
import com.micatechnologies.minecraft.csm.technology.school.BlockSchoolBell;
import com.micatechnologies.minecraft.csm.technology.school.BlockSchoolClock;
import com.micatechnologies.minecraft.csm.technology.school.BlockSchoolClockSpeakerPanel;
import com.micatechnologies.minecraft.csm.technology.school.ItemBellLinker;
import com.micatechnologies.minecraft.csm.technology.school.SchoolClockDial;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for technology blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 8)
public class CsmTabTechnology extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabtechnology";
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
    return CsmRegistry.getBlock("imacpro");
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
    initTabBlock(new BlockSpeakerFactory("atls1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("atls5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("appletv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.125000, 0.687500), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("bose1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("bose2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("altec_lansing_speaker_white", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("altec_lansing_speaker_black", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("bosch_speaker_1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("bosch_speaker_2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("boston_acoustics_speaker_white", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("boston_acoustics_speaker_black", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("polk_audio_speaker_white", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("polk_audio_speaker_black", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("fjs1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("fjs2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(BlockImac.class, fmlPreInitializationEvent);
    initTabBlock(BlockImacPro.class, fmlPreInitializationEvent);
    initTabBlock(new BlockSpeakerFactory("jblc1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("jblc2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(BlockMacBookPro.class, fmlPreInitializationEvent);
    initTabBlock(new BlockDeskDeviceFactory("macbook_air_closed", new double[]{2, 0, 3.75, 14, 1, 12.25}));
    initTabBlock(new BlockDeskDeviceFactory("mac_studio", new double[]{3, 0, 3, 13, 5.25, 13}));
    initTabBlock(new BlockDeskDeviceFactory("mac_keyboard", new double[]{1, 0, 6, 15, 0.6, 9.85}));
    initTabBlock(new BlockRotatableNSEWUDFactory("stbox", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.250000, 1.000000, 0.187500, 0.750000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("tvdish", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.000000, 0.250000, 0.812500, 1.000000, 0.812500), false, true, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("tvdishside", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.375000, 0.125000, 0.812500, 1.000000, 1.000000), false, true, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("vcs1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs6", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs7", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs8", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockSpeakerFactory("vcs9", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, true, true));
    initTabBlock(new BlockRotatableNSEWUDFactory("waptpl225", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.250000, 0.937500, 0.750000, 0.750000, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("wapac", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.250000, 0.937500, 0.750000, 0.750000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("wapn", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.250000, 0.937500, 0.750000, 0.750000, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("wg", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.000000, 0.187500, 0.687500, 0.562500, 0.812500), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory.PoleFitted("nema_enclosure", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.140625, 0.093750, 0.687500, 0.859375, 0.906250, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("micarolla_ont", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.062500, 0.750000, 0.812500, 0.937500, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    // The Redstone TTS Module and its linker belong to this tab but ship in the optional Text to
    // Speech module, which requires this one. Naming their classes here would point Technology at
    // a module that depends on it, so they are resolved by name and only when that module is
    // installed. Keeping the calls in place means the tab's order is the same either way: with
    // Text to Speech installed the entries appear exactly here, and without it they are absent.
    initTabBlockIfLoaded("csm_tts",
        "com.micatechnologies.minecraft.csm.tts.BlockRedstoneTTS", fmlPreInitializationEvent);
    initTabItemIfLoaded("csm_tts",
        "com.micatechnologies.minecraft.csm.tts.ItemTtsLinker", fmlPreInitializationEvent);
    // The school set: PA speakers, clocks, clock/speaker panels, the bell schedule controller,
    // the hallway bell and the linker (gen_technology_school.py --fragments).
    initTabBlock(new BlockSpeakerFactory("school_pa_speaker_cube", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.156250, 0.156250, 0.437500, 0.843750, 0.843750, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSpeakerFactory("school_pa_speaker_wallbox", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.062500, 0.062500, 0.593750, 0.937500, 0.937500, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, false));
    initTabBlock(new BlockSchoolClock("school_wall_clock", new double[]{1.4, 1.4, 12.8, 14.6, 14.6, 16.0}, new SchoolClockDial(EnumFacing.NORTH, 8.0, 8.0, 13.0, 6.6, true)));
    initTabBlock(new BlockSchoolClock("school_double_clock", new double[]{5.2, 2.8, 0.6, 10.8, 14.4, 16.0}, new SchoolClockDial(EnumFacing.WEST, 5.4, 8.6, 6.4, 5.8, true), new SchoolClockDial(EnumFacing.EAST, 10.6, 8.6, 6.4, 5.8, true)));
    initTabBlock(new BlockSchoolClock("school_hanging_clock", new double[]{2.2, 1.6, 5.4, 13.8, 16.0, 10.6}, new SchoolClockDial(EnumFacing.NORTH, 8.0, 7.4, 5.6, 5.8, true), new SchoolClockDial(EnumFacing.SOUTH, 8.0, 7.4, 10.4, 5.8, true)));
    initTabBlock(new BlockSchoolClockSpeakerPanel("school_clock_speaker_panel", false, new double[]{0.0, 1.7, 13.2, 16.0, 14.3, 16.0}, new double[]{0.0, 2.0, 15.0, 16.0, 14.0, 16.0}, new SchoolClockDial(EnumFacing.NORTH, 8.0, 8.0, 13.4, 6.3, true)));
    initTabBlock(new BlockSchoolClockSpeakerPanel("school_clock_speaker_panel_vertical", true, new double[]{1.7, 0.0, 13.2, 14.3, 16.0, 16.0}, new double[]{2.0, 0.0, 15.0, 14.0, 16.0, 16.0}, new SchoolClockDial(EnumFacing.NORTH, 8.0, 8.0, 13.4, 6.3, true)));
    initTabBlock(new BlockBellController("school_bell_controller", new double[]{3.0, 2.0, 13.5, 13.0, 14.0, 16.0}));
    initTabBlock(new BlockSchoolBell("school_bell_gong", new double[]{2.2, 1.0, 8.6, 13.8, 13.8, 16.0}));
    initTabItem(ItemBellLinker.class, fmlPreInitializationEvent);
    initTabItem(new ItemDecorativeFactory("appleipadpro", "This iPad does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appleiphonese2020", "This iPhone does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appleiphonexr", "This iPhone does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("rabbitr1", "This Rabbit R1 does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appleiphonexs", "This iPhone does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("appletvremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("applewatch", "This Apple Watch does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("directvremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("dishremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("fiosremote", "This remote does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("spectrumremote", "This remote does nothing and is only for looks!"));
    initTabItem(ItemApplePencil.class, fmlPreInitializationEvent);
  }
}

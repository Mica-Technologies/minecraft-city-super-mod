package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacCooler;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacVentFactory;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacCoolerBlack;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacCoolerSilver;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacHeater;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacHeaterBlack;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacHeaterSilver;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacRtuCooler;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacRtuCoolerBlack;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacRtuCoolerSilver;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacRtuHeater;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacRtuHeaterBlack;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacRtuHeaterSilver;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacThermostat;
import com.micatechnologies.minecraft.csm.hvac.BlockExposedDuct;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacCrac;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacExhaustFan;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacExhaustGrille;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacLabExhaustFan;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacZoneBoundary;
import com.micatechnologies.minecraft.csm.hvac.BlockExposedServiceHanger;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacVentRelay;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacZoneThermostat;
import com.micatechnologies.minecraft.csm.hvac.ItemHvacLinker;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for HVAC blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 2)
public class CsmTabHvac extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabhvac";
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
    return CsmRegistry.getBlock("sv4");
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
    initTabBlock(new BlockHvacVentFactory("art1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("art2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("artd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("artd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("dfv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("dfv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("dfvd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("dfvd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("lcv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockHvacVentFactory("xlcv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockHvacVentFactory("mv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("mv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("mv3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.874375, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("mvd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("mvd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("pbf", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("pv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("pvd", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("rv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockHvacVentFactory("rv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockHvacVentFactory("scv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockHvacVentFactory("sv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("sv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("sv3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("sv4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("sv5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("svd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("svd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("svd3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("svd4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockHvacVentFactory("svd5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));

    // Functional HVAC blocks — Standard units
    initTabBlock(BlockHvacHeater.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacHeaterBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacHeaterSilver.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacCooler.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacCoolerBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacCoolerSilver.class, fmlPreInitializationEvent);

    // Functional HVAC blocks — Rooftop units
    initTabBlock(BlockHvacRtuHeater.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacRtuHeaterBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacRtuHeaterSilver.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacRtuCooler.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacRtuCoolerBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacRtuCoolerSilver.class, fmlPreInitializationEvent);
    // Data hall CRAC/CRAH units (#305; gen_hvac_data_hall.py --fragments)
    initTabBlock(new BlockHvacCrac("hvac_crac_downflow")); // CRAC Unit (Downflow)
    initTabBlock(new BlockHvacCrac("hvac_crac_upflow")); // CRAC Unit (Upflow)
    initTabBlock(new BlockHvacCrac("hvac_crah")); // CRAH Unit (Chilled Water)
    // Rooftop exhaust fans (#314; gen_hvac_exhaust.py --fragments)
    initTabBlock(new BlockHvacLabExhaustFan("hvac_lab_exhaust_fan")); // Lab Exhaust Fan (High Plume)
    initTabBlock(new BlockHvacExhaustFan("hvac_upblast_exhaust_fan")); // Upblast Exhaust Fan
    initTabBlock(new BlockHvacExhaustFan("hvac_mushroom_exhaust_fan")); // Mushroom Exhaust Fan

    // Functional HVAC blocks — Controls and distribution
    initTabBlock(BlockHvacThermostat.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacZoneThermostat.class, fmlPreInitializationEvent);
    initTabBlock(new BlockHvacZoneBoundary()); // HVAC Zone Boundary (#305)
    initTabBlock(BlockHvacVentRelay.class, fmlPreInitializationEvent);
    initTabBlock(new BlockHvacExhaustGrille()); // HVAC Exhaust Grille (#314)
    initTabBlock(new BlockExposedDuct("duct_round_galvanized")); // Exposed Spiral Duct (Galvanized)
    initTabBlock(new BlockExposedDuct("duct_round_black")); // Exposed Spiral Duct (Black)
    initTabBlock(new BlockExposedDuct("duct_round_white")); // Exposed Spiral Duct (White)
    initTabBlock(new BlockExposedDuct("duct_round_charcoal")); // Exposed Spiral Duct (Charcoal)
    initTabBlock(new BlockExposedDuct("duct_rect_galvanized")); // Exposed Rectangular Duct (Galvanized)
    initTabBlock(new BlockExposedDuct("duct_rect_black")); // Exposed Rectangular Duct (Black)
    initTabBlock(new BlockExposedDuct("pipe_black")); // Exposed Pipe (Black)
    initTabBlock(new BlockExposedDuct("pipe_white")); // Exposed Pipe (White)
    initTabBlock(new BlockExposedDuct("pipe_red")); // Exposed Pipe (Red)
    initTabBlock(new BlockExposedDuct("pipe_silver")); // Exposed Pipe (Silver)
    initTabBlock(new BlockExposedServiceHanger()); // Threaded Rod Hanger
    initTabItem(ItemHvacLinker.class, fmlPreInitializationEvent);
  }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacCooler;
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
import com.micatechnologies.minecraft.csm.hvac.BlockHvacVentRelay;
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
    initTabBlock(new BlockRotatableNSEWUDFactory("art1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("art2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("artd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("artd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("dfv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("dfv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("dfvd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("dfvd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("lcv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockRotatableNSEWUDFactory("mv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mv3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.874375, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mvd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mvd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pbf", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pvd", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("rv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockRotatableNSEWUDFactory("rv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockRotatableNSEWUDFactory("scv", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.900000, 0.000000, 1.000000, 1.000000, 1.000000), false, false, false, BlockRenderLayer.CUTOUT_MIPPED, false, true));
    initTabBlock(new BlockRotatableNSEWUDFactory("sv1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("sv2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("sv3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("sv4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("sv5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("svd1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("svd2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("svd3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("svd4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("svd5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255, new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), true, true, false, BlockRenderLayer.SOLID, false, false));

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

    // Functional HVAC blocks — Controls and distribution
    initTabBlock(BlockHvacThermostat.class, fmlPreInitializationEvent);
    initTabBlock(BlockHvacVentRelay.class, fmlPreInitializationEvent);
    initTabItem(ItemHvacLinker.class, fmlPreInitializationEvent);
  }
}

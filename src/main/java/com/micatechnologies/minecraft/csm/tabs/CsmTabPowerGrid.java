package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.BlockRotatableNSEWUDFactory;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyProducer;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyToRedstone;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for power grid blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 6)
public class CsmTabPowerGrid extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabpowergrid";
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
    return CsmRegistry.getBlock("rftors");
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
    initTabBlock(new BlockRotatableNSEWUDFactory("afei", Material.GLASS, SoundType.GLASS, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 2.000000, 0.687500), false, true, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("afeis", Material.GLASS, SoundType.GLASS, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.312500, 0.000000, 0.687500, 0.687500, 2.000000), false, true, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("fgphvsign", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("fgpolebottom", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("fgpolemiddle", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, -0.312500, 1.000000, 1.000000, 1.312500), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("fgpoletop", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.437500, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mluvmb1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.312500, 0.000000, 0.812500, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mluvmb2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mluvmb3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mluvmb4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mluvmb5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.062500), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("mphvsign", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("newbrooksxarm1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.062500, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("newbrooksxarm2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("newbrooksxarm3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("newbrooksxarm4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("newesbrooksxarm1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.062500, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("newesbrooksxarm2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("newesbrooksxarm3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldbrooksxarm1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.062500, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldbrooksxarm2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldbrooksxarm3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldbrooksxarm4", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.062500, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldbrooksxarm5", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldbrooksxarm6", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldbrooksxarm7", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("oldesbrooksxarm", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.062500, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pcab1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.312500, 0.000000, 0.812500, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pcab2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pcab3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pcaw1", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.187500, 0.312500, 0.000000, 0.812500, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pcaw2", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.500000, 0.000000, 0.750000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pcaw3", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("polehvsign", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("polevisstrips", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.000000, 0.000000, 0.900000, 1.000000, 1.000000, 1.000000), false, true, false, BlockRenderLayer.TRANSLUCENT, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("polewiremount", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(-0.750000, 0.000000, 0.000000, -0.250000, 0.687500, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("pullymount", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 0.687500, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("scelightmount", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.187500, -1.000000, 0.687500, 1.187500, 1.187500), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("scelightmountsmall", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.187500, 0.000000, 0.687500, 1.187500, 1.187500), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("teic", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.125000, 0.000000, -1.000000, 0.875000, 0.750000, 0.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("teicde", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.125000, 0.000000, -1.000000, 0.875000, 1.062500, 0.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("tepg", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.125000, -0.312500, 0.375000, 0.812500, 1.000000, 0.500000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("tsc", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.312500, 0.187500, 0.750000, 0.687500, 1.812500, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(new BlockRotatableNSEWUDFactory("transformermount", Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0, new AxisAlignedBB(0.250000, 0.000000, 0.000000, 0.750000, 0.500000, 1.000000), false, true, false, BlockRenderLayer.SOLID, false, false));
    initTabBlock(BlockForgeEnergyProducer.class, fmlPreInitializationEvent);
    initTabBlock(BlockForgeEnergyToRedstone.class, fmlPreInitializationEvent);
  }
}

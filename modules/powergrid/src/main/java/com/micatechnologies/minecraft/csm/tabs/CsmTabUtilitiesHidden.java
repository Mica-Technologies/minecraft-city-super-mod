package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTankBand;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTankPart;
import com.micatechnologies.minecraft.csm.powergrid.water.TankShapes;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The hidden tab holding the Utilities blocks that never belong in the creative inventory: the
 * water tower tanks' band tiles, which the tank's own item places, and the invisible part that
 * fills a tank's or a pedestal column's cells for collision.
 *
 * <p>Hidden tabs have no creative-inventory presence at all: {@link CsmTab} gives them a
 * {@code null} {@code CreativeTabs}, which is also what keeps their blocks out of the
 * Fabricator.</p>
 *
 * @since 2026.9
 */
@CsmTab.Load(order = -6)
public class CsmTabUtilitiesHidden extends CsmTab {

  @Override
  public String getTabId() {
    return null;
  }

  @Override
  public Block getTabIcon() {
    return null;
  }

  @Override
  public boolean getTabSearchable() {
    return false;
  }

  @Override
  public boolean getTabHidden() {
    return true;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // --- Water system (gen_utilities_water.py --fragments) ---
    initTabBlock(new BlockTankBand("water_tower_bowl_small_band", TankShapes.BOWL_SMALL));
    initTabBlock(new BlockTankBand("water_tower_bowl_medium_band", TankShapes.BOWL_MEDIUM));
    initTabBlock(new BlockTankBand("water_tower_spheroid_small_band", TankShapes.SPHEROID_SMALL));
    initTabBlock(new BlockTankBand("water_tower_spheroid_medium_band", TankShapes.SPHEROID_MEDIUM));
    initTabBlock(new BlockTankPart());
  }
}

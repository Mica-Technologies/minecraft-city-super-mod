package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.buildingmaterials.BlockAutoDoorMat;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockAutoDoorTop;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The building module's hidden tab: blocks that stay registered so placed ones keep working, but
 * that nobody should pick from the inventory any more.
 *
 * <p>The automatic door activation mat: the sliding doors open with their approach sensor now
 * ({@code AutoDoorSensors}), and a mat already laid still opens them.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
@CsmTab.Load(order = -13)
public class CsmTabBuildingHidden extends CsmTab {

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
    initTabBlock(BlockAutoDoorMat.class, fmlPreInitializationEvent); // Automatic Door Activation Mat
    initTabBlock(new BlockAutoDoorTop("auto_door_tall_top_clear")); // Tall Sliding Door Header (Clear Aluminium)
    initTabBlock(new BlockAutoDoorTop("auto_door_tall_top_bronze")); // Tall Sliding Door Header (Dark Bronze)
    initTabBlock(new BlockAutoDoorTop("auto_door_tall_top_black")); // Tall Sliding Door Header (Black Anodised)
    initTabBlock(new BlockAutoDoorTop("auto_door_tall_top_white")); // Tall Sliding Door Header (White)
    initTabBlock(new BlockAutoDoorTop("auto_door_tall_top_stainless")); // Tall Sliding Door Header (Stainless Steel)
    initTabBlock(new BlockAutoDoorTop("auto_door_tall_top_frameless")); // Tall Sliding Door Header (Frameless Glass)
  }
}

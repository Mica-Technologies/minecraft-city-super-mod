package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerBlack;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerSilver;
import com.micatechnologies.minecraft.csm.lifesafety.BlockFireAlarmSprinklerWhite;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler2;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler3;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler4;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler5;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler6;
import com.micatechnologies.minecraft.csm.lifesafety.BlockOldFireSprinkler;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Fire Protection tab of the Life Safety module: sprinklers, and the rest of what a
 * building has to fight a fire with besides its alarm. The sprinklers moved here from the one
 * Life Safety tab with their registry names unchanged.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 21)
public class CsmTabFireProtection extends CsmTab {

  @Override
  public String getTabId() {
    return "tabfireprotection";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("firealarmsprinklersilver");
  }

  @Override
  public boolean getTabSearchable() {
    return true;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockFireAlarmSprinklerBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSprinklerSilver.class, fmlPreInitializationEvent);
    initTabBlock(BlockFireAlarmSprinklerWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler2.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler3.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler4.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler5.class, fmlPreInitializationEvent);
    initTabBlock(BlockOldFireSprinkler6.class, fmlPreInitializationEvent);
  }
}

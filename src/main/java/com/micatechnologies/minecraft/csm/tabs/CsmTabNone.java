package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lighting.BlockLightupAir;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalGreenGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalRedGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalYellowAdvanceFlashGrayA;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalYellowAdvanceFlashGrayB;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableSingleSolidSignalYellowGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAheadSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2AheadSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2BikeSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2LeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2RailSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2Right2Signal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2RightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2SolidSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2UTurnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngle2UpLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleAheadSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleBikeSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleRailSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleRight2Signal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleRightSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleSolidSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleUTurnSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalAngleUpLeftSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalBikeSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalHybridLeftAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalHybridLeftSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftAddOnFYASignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftDoubleAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalLeftSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRailSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightAddOnFYASignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightDoubleAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalRightSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashGreenSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashRedSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidFlashYellowSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalSolidSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalUpLeftAddOnSignalGray;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableVerticalUpLeftSignalGray;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The "none" tab for blocks which don't belong to a tab.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 0)
public class CsmTabNone extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return null;
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
    return null;
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
    return true;
  }

  /**
   * Initializes all the items belonging to the tab.
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockLightupAir.class, fmlPreInitializationEvent); // Lightup Air

    // Deprecated angled signal blocks (auto-convert to non-angled equivalents via ICsmRetiringBlock)
    initTabBlock(BlockControllableVerticalAngleAheadSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleBikeSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleLeftSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleRailSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleRight2Signal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleRightSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleSolidSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleUTurnSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngleUpLeftSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2AheadSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2BikeSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2LeftSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2RailSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2Right2Signal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2RightSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2SolidSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2UTurnSignal.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAngle2UpLeftSignal.class, fmlPreInitializationEvent);

    // Deprecated gray signal head blocks (auto-convert to non-gray equivalents via ICsmRetiringBlock)
    initTabBlock(BlockControllableVerticalSolidSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalSolidFlashGreenSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalSolidFlashYellowSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalSolidFlashRedSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalLeftSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalRightSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalAheadSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalBikeSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalRailSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalHybridLeftSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalHybridLeftAddOnSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalLeftAddOnSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalRightAddOnSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalLeftAddOnFYASignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalRightAddOnFYASignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalLeftDoubleAddOnSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalRightDoubleAddOnSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalUpLeftAddOnSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableVerticalUpLeftSignalGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableSingleSolidSignalRedGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableSingleSolidSignalYellowGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableSingleSolidSignalGreenGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableSingleSolidSignalYellowAdvanceFlashGrayA.class, fmlPreInitializationEvent);
    initTabBlock(BlockControllableSingleSolidSignalYellowAdvanceFlashGrayB.class, fmlPreInitializationEvent);
  }
}

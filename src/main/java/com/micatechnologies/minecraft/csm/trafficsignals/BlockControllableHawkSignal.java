package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * HAWK (High-intensity Activated crossWalK) beacon signal block. A pedestrian-activated beacon
 * used at unsignalized crossings that displays flashing and steady signals to approaching traffic.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockControllableHawkSignal extends AbstractBlockControllableSignalHead {

  public BlockControllableHawkSignal() {
    super(Material.ROCK);
  }

  @Override
  public SIGNAL_SIDE getSignalSide(World world, BlockPos blockPos) {
    return SIGNAL_SIDE.PEDESTRIAN_BEACON;
  }

  @Override
  public boolean doesFlash() {
    return true;
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllablehawksignal";
  }

  // HAWK layout: 2 red bulbs side-by-side on top, 1 yellow centered below
  // Section 0 = left red, Section 1 = right red, Section 2 = yellow

  @Override
  public float[] getSectionYPositions(int sectionCount) {
    return new float[] {0.0f, 0.0f, -12.0f};
  }

  @Override
  public float[] getSectionXPositions(int sectionCount) {
    return new float[] {-6.0f, 6.0f, 0.0f};
  }

  /**
   * HAWK color state mapping with wigwag support for color=2.
   *
   * color=0: Both reds ON solid, yellow OFF
   * color=1: Reds OFF, yellow ON
   * color=2: Reds WIGWAG (alternating flash — left and right alternate at 500ms)
   * color=3: All OFF
   *
   * Wigwag is implemented by using the pause-aware game clock to alternate which
   * red section is lit. This avoids changes to the SectionInfo flash system.
   */
  @Override
  public boolean shouldLightBulb(int colorState, TrafficSignalBulbColor bulbColor) {
    if (colorState == 0 && bulbColor == TrafficSignalBulbColor.RED) return true;
    if (colorState == 1 && bulbColor == TrafficSignalBulbColor.YELLOW) return true;
    // color=2 (wigwag) is handled in shouldLightAllSections — we don't use this path
    return false;
  }

  /**
   * For color=2 (wigwag), we light sections individually based on timing.
   * This method returns false — wigwag is handled per-section in the TE via
   * the shouldLightHawkWigwag method below.
   */
  @Override
  public boolean shouldLightAllSections(int colorState) {
    return false;
  }

  /**
   * Returns whether a specific section index should be lit for the HAWK wigwag
   * state (color=2). Left red (section 0) and right red (section 1) alternate
   * at 500ms intervals. Yellow (section 2) stays off.
   *
   * @param sectionIndex the section to check
   * @param gameMillis   a pause-aware game clock in milliseconds (see
   *                     {@code CsmRenderUtils.gameMillis}); threaded from the TE's
   *                     getSectionInfos so we avoid a per-frame JNI
   *                     {@code System.currentTimeMillis} call
   */
  public boolean shouldLightWigwagSection(int sectionIndex, long gameMillis) {
    boolean firstHalf = (gameMillis % 1000L) < 500L;
    if (sectionIndex == 0) return firstHalf;
    if (sectionIndex == 1) return !firstHalf;
    return false; // yellow stays off during wigwag
  }

  @Override
  public TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo() {
    return new TrafficSignalSectionInfo[] {
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
        new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false)
    };
  }
}

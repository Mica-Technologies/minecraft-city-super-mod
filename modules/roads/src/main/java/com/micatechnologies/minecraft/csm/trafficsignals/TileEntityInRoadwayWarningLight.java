package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightLinkMode;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.InRoadwayLightPattern;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Configuration for a {@link BlockInRoadwayWarningLight}: which of a controller's device lists it
 * joins, and which flash sequence it runs.
 *
 * <p>The link mode is not finish, unlike the RRFB's paint — it decides what the fixture
 * <em>is</em> to a controller, because the block reports it from {@code getSignalSide}. It also
 * decides how the fixture reads the colour the controller sends, which is why the two modes
 * cannot be one behaviour: a beacon is lit on everything but off, while a crosswalk fixture must
 * be dark on don't-walk, and don't-walk is that same colour.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntityInRoadwayWarningLight extends AbstractTileEntity {

  /** Short NBT keys, matching the convention the other signal tile entities use. */
  private static final String KEY_PATTERN = "pat";
  private static final String KEY_LINK_MODE = "lnk";

  private InRoadwayLightPattern pattern = InRoadwayLightPattern.RRFB;
  private InRoadwayLightLinkMode linkMode = InRoadwayLightLinkMode.BEACON;

  public InRoadwayLightPattern getPattern() {
    return pattern;
  }

  public void setPattern(InRoadwayLightPattern pattern) {
    this.pattern = pattern == null ? InRoadwayLightPattern.RRFB : pattern;
    sync();
  }

  /**
   * Advances to the next flash pattern and returns it. Invoked by the config GUI.
   *
   * @return the pattern now running
   */
  public InRoadwayLightPattern getNextPattern() {
    setPattern(pattern.getNext());
    return pattern;
  }

  public InRoadwayLightLinkMode getLinkMode() {
    return linkMode;
  }

  /**
   * Sets which list the fixture links into.
   *
   * <p>Changing this on a fixture that is already linked does not move it: the controller stores
   * the link by position in one list, and it was put there by the mode in force when it was
   * linked. Re-link it after changing the mode, which is also the moment a player would expect
   * to have to.</p>
   *
   * @param linkMode the mode to use for future links
   */
  public void setLinkMode(InRoadwayLightLinkMode linkMode) {
    this.linkMode = linkMode == null ? InRoadwayLightLinkMode.BEACON : linkMode;
    sync();
  }

  /**
   * Advances to the next link mode and returns it. Invoked by the config GUI.
   *
   * @return the mode now set
   */
  public InRoadwayLightLinkMode getNextLinkMode() {
    setLinkMode(linkMode.getNext());
    return linkMode;
  }

  /**
   * Pushes a change to the client, guarded on there being a world to push into. A tile entity
   * can be configured before it is placed, and syncing then dereferences a world that does not
   * exist yet — the school zone beacon's setters guard the same way, for the same reason.
   */
  private void sync() {
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    pattern = InRoadwayLightPattern.fromNBT(compound.getInteger(KEY_PATTERN));
    linkMode = InRoadwayLightLinkMode.fromNBT(compound.getInteger(KEY_LINK_MODE));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_PATTERN, pattern.toNBT());
    compound.setInteger(KEY_LINK_MODE, linkMode.toNBT());
    return compound;
  }
}

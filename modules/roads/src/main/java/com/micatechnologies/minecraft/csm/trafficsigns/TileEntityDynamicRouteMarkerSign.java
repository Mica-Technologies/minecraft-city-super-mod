package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

/**
 * What one route marker says: which shield it wears and what route number is printed on it.
 *
 * <p>Two values and nothing else. The shield is also a block property (see
 * {@link BlockDynamicRouteMarkerSign#SHIELD}), which is what lets the marker's face be an
 * ordinary block model; changing it therefore has to make the client rebuild the chunk section,
 * not merely re-read the tile entity, so the setter goes through the {@code markDirtySync}
 * overload that notifies a block update as well.</p>
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class TileEntityDynamicRouteMarkerSign extends AbstractTileEntity {

  /** Longest route number the plate will take. Three digits plus a letter suffix ("101A"). */
  public static final int MAX_ROUTE_LENGTH = 4;

  // Short NBT keys: this is written for every marker in a chunk, and the mod's convention is
  // that a tile entity's own keys are as short as they can be and still be told apart.
  private static final String NBT_SHIELD = "sh";
  private static final String NBT_ROUTE = "rt";

  private GuideSignShieldType shield = GuideSignShieldType.INTERSTATE;
  private String routeNumber = "95";

  @Override
  public void readNBT(NBTTagCompound compound) {
    shield = GuideSignShieldType.fromOrdinal(compound.getInteger(NBT_SHIELD));
    routeNumber = clampRoute(compound.getString(NBT_ROUTE));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_SHIELD, shield.ordinal());
    compound.setString(NBT_ROUTE, routeNumber);
    return compound;
  }

  public GuideSignShieldType getShield() {
    return shield;
  }

  /**
   * Sets the marker and rebuilds the section that draws it.
   *
   * @param shield the marker to wear; null is the default marker
   *
   * @since 1.0
   */
  public void setShield(GuideSignShieldType shield) {
    this.shield = shield == null ? GuideSignShieldType.INTERSTATE : shield;
    sync();
  }

  public String getRouteNumber() {
    return routeNumber;
  }

  /**
   * Sets the route number. Anything too long, or made of characters the sign font cannot set, is
   * trimmed here rather than at the caller, so a crafted packet cannot produce a marker the
   * renderer would have to cope with.
   *
   * @param routeNumber the number to print
   *
   * @since 1.0
   */
  public void setRouteNumber(String routeNumber) {
    this.routeNumber = clampRoute(routeNumber);
    sync();
  }

  /**
   * A route number as the sign will actually carry it: upper case, digits and letters only, and
   * no longer than the plate takes.
   *
   * @param value the raw value
   *
   * @return the value the sign will print
   *
   * @since 1.0
   */
  public static String clampRoute(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(MAX_ROUTE_LENGTH);
    for (int i = 0; i < value.length() && out.length() < MAX_ROUTE_LENGTH; i++) {
      char c = Character.toUpperCase(value.charAt(i));
      if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')) {
        out.append(c);
      }
    }
    return out.toString();
  }

  private void sync() {
    if (getWorld() == null) {
      return;
    }
    // The state overload: the shield is a block property, so clients have to rebuild the
    // section rather than only re-read this tile entity.
    markDirtySync(getWorld(), getPos(), getWorld().getBlockState(getPos()), true);
  }

  /**
   * Rebuilds the section when a marker changes under a client that was not told by a block
   * update -- a tile entity packet on its own does not invalidate the chunk mesh, and the face
   * is baked into it.
   */
  @Override
  public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
    GuideSignShieldType before = shield;
    super.onDataPacket(net, pkt);
    if (world != null && world.isRemote && before != shield) {
      world.markBlockRangeForRenderUpdate(pos, pos);
    }
  }
}

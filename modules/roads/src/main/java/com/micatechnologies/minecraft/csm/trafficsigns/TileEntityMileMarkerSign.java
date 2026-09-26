package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

/**
 * What one mile marker is set to: its mile number and tenth, and on an enhanced plate the
 * direction, route shield and route number.
 *
 * <p>Everything here is drawn into the chunk mesh by {@code MileMarkerBakedModel}, so there is no
 * renderer and no per-frame cost; the price is that a change has to rebuild the section, which the
 * setter asks for through the {@code markDirtySync} overload that notifies a block update, and
 * {@link #onDataPacket} asks for again on a client told only by the entity packet.</p>
 *
 * <p>The raw values are kept as set and clamped only when the legend is built, against the plate
 * the tile entity stands in ({@link MileMarkerLegend#of}), so one tile entity class serves all
 * eight plates.</p>
 *
 * @since 2026.9
 */
public class TileEntityMileMarkerSign extends AbstractTileEntity {

  private static final String NBT_MILE = "mi";
  private static final String NBT_TENTH = "tn";
  private static final String NBT_SHIELD = "sh";
  private static final String NBT_ROUTE = "rt";
  private static final String NBT_DIRECTION = "dr";

  /** Below zero: the plate's default number. */
  private int mile = -1;
  private int tenth = 2;
  private GuideSignShieldType shield = GuideSignShieldType.US_ROUTE;
  private String route = "12";
  private int direction = 3;

  @Override
  public void readNBT(NBTTagCompound compound) {
    mile = compound.hasKey(NBT_MILE) ? compound.getInteger(NBT_MILE) : -1;
    tenth = compound.hasKey(NBT_TENTH) ? compound.getInteger(NBT_TENTH) : 2;
    shield = compound.hasKey(NBT_SHIELD)
        ? GuideSignShieldType.fromOrdinal(compound.getInteger(NBT_SHIELD))
        : GuideSignShieldType.US_ROUTE;
    route = compound.hasKey(NBT_ROUTE)
        ? MileMarkerLegend.clampRoute(compound.getString(NBT_ROUTE)) : "12";
    direction = compound.hasKey(NBT_DIRECTION) ? compound.getInteger(NBT_DIRECTION) : 3;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_MILE, mile);
    compound.setInteger(NBT_TENTH, tenth);
    compound.setInteger(NBT_SHIELD, shield.ordinal());
    compound.setString(NBT_ROUTE, route);
    compound.setInteger(NBT_DIRECTION, direction);
    return compound;
  }

  /**
   * The legend the given plate shows for these settings.
   *
   * @param layout the plate this tile entity stands in
   *
   * @return the legend
   */
  public MileMarkerLegend legend(MileMarkerLayout layout) {
    return MileMarkerLegend.of(layout, mile, tenth, shield, route, direction);
  }

  /** The plate this tile entity stands in, or null if the block is not a mile marker. */
  @Nullable
  public MileMarkerLayout layout() {
    if (world == null) {
      return null;
    }
    Block block = world.getBlockState(pos).getBlock();
    return block instanceof BlockMileMarkerSign ? ((BlockMileMarkerSign) block).getLayout() : null;
  }

  /**
   * Sets every value at once, clamped to the plate, and rebuilds the section that draws them.
   *
   * @param layout    the plate
   * @param mile      the mile number
   * @param tenth     tenths of a mile
   * @param shield    the route shield; null keeps the current one
   * @param route     the route number
   * @param direction 0 to 3
   */
  public void configure(MileMarkerLayout layout, int mile, int tenth,
      @Nullable GuideSignShieldType shield, @Nullable String route, int direction) {
    this.mile = mile < 0 ? -1
        : Math.max(layout.getMinMile(), Math.min(layout.getMaxMile(), mile));
    this.tenth = Math.max(0, Math.min(9, tenth));
    if (shield != null) {
      this.shield = shield;
    }
    this.route = MileMarkerLegend.clampRoute(route);
    this.direction = Math.max(0, Math.min(MileMarkerLegend.DIRECTIONS.length - 1, direction));
    if (world != null) {
      markDirtySync(world, pos, world.getBlockState(pos), true);
    }
  }

  public int getMile() {
    return mile;
  }

  public int getTenth() {
    return tenth;
  }

  public GuideSignShieldType getShield() {
    return shield;
  }

  public String getRoute() {
    return route;
  }

  public int getDirection() {
    return direction;
  }

  /**
   * Rebuilds the section when the settings change under a client that was not told by a block
   * update: the legend is baked into the chunk mesh, and an entity packet alone does not redraw
   * it.
   */
  @Override
  public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
    int m = mile;
    int t = tenth;
    GuideSignShieldType s = shield;
    String r = route;
    int d = direction;
    super.onDataPacket(net, pkt);
    if (world != null && world.isRemote && (m != mile || t != tenth || s != shield
        || !r.equals(route) || d != direction)) {
      world.markBlockRangeForRenderUpdate(pos, pos);
    }
  }
}

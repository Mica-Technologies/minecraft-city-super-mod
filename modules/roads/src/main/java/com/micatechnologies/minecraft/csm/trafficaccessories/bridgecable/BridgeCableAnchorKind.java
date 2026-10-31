package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The four anchor blocks: what each looks like is its model's business, but where a cable meets
 * it and how much pipe it wears there is decided here, since both ends of a cable need it and only
 * one of them draws it.
 *
 * <p>Every number below is for the model as drawn, facing north (out of a wall on its south side,
 * or, for the two that sit on top of things, with the cable running north and south). The
 * numbers are {@code gen_bridge_cables.py}'s, which draws the models; keep the two together.
 */
public enum BridgeCableAnchorKind {

  /**
   * A stay's socket on a pylon face: a bearing plate with a boss the anchor pipe leaves from.
   * The cable meets the boss's face, and wears a guide pipe for its first block, at whatever
   * angle it leaves.
   */
  SOCKET("bridge_cable_socket", true, -3.0 / 16.0, 1.0,
      box(3, 3, 11, 13, 13, 16)),

  /** A forked clevis and pin: an open end, so only a short socket on the cable. */
  CLEVIS("bridge_cable_clevis", true, 1.0 / 16.0, 0.35,
      box(4, 4, 3, 12, 12, 16)),

  /**
   * A suspension tower's saddle: the cable passes over it in a groove. Sits on top of the tower;
   * its facing only says which way the groove runs.
   */
  SADDLE("bridge_cable_saddle", false, 13.0 / 16.0, 0.0,
      box(1, 0, 1, 15, 14, 15)),

  /**
   * A deck anchor on top of a deck edge beam, where a stay or a hanger comes down. Wears the same
   * guide pipe a socket does.
   */
  DECK("bridge_cable_deck_anchor", false, 6.0 / 16.0, 1.0,
      box(2, 0, 2, 14, 7, 14));

  private final String registryName;
  private final boolean wallMounted;
  /**
   * For a wall-mounted kind, how far the cable's end sits out from the block's centre along its
   * facing (negative is back toward the wall). For a kind that sits on top, the end's height in
   * the block.
   */
  private final double attach;
  private final double sleeveLength;
  private final AxisAlignedBB northBox;

  BridgeCableAnchorKind(String registryName, boolean wallMounted, double attach,
      double sleeveLength, AxisAlignedBB northBox) {
    this.registryName = registryName;
    this.wallMounted = wallMounted;
    this.attach = attach;
    this.sleeveLength = sleeveLength;
    this.northBox = northBox;
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
  }

  public String getRegistryName() {
    return registryName;
  }

  /**
   * Whether the block bolts to any face it was placed on (and so faces any of six ways), rather
   * than sitting on top of something and facing one of four.
   */
  public boolean isWallMounted() {
    return wallMounted;
  }

  /**
   * How long the guide pipe a cable wears where it meets this anchor is, in blocks; 0 for none.
   * A short cable's pipe is cut to a quarter of its length so the pipes never meet.
   */
  public double getSleeveLength() {
    return sleeveLength;
  }

  /** The hitbox as drawn, facing north; the block class turns it. */
  public AxisAlignedBB getNorthBox() {
    return northBox;
  }

  /**
   * Where a cable meets this anchor, in world coordinates.
   *
   * @param pos    the anchor's block
   * @param facing its facing
   */
  public Vec3d attachPoint(BlockPos pos, EnumFacing facing) {
    if (!wallMounted) {
      return new Vec3d(pos.getX() + 0.5, pos.getY() + attach, pos.getZ() + 0.5);
    }
    return new Vec3d(
        pos.getX() + 0.5 + facing.getXOffset() * attach,
        pos.getY() + 0.5 + facing.getYOffset() * attach,
        pos.getZ() + 0.5 + facing.getZOffset() * attach);
  }

  /** The kind a registry name names, or null. */
  public static BridgeCableAnchorKind byRegistryName(String name) {
    for (BridgeCableAnchorKind kind : values()) {
      if (kind.registryName.equals(name)) {
        return kind;
      }
    }
    return null;
  }
}

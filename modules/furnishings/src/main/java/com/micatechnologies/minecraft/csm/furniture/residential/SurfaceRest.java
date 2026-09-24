package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * What a small piece set on top of other furniture stands on, which decides how far below its
 * own block it is drawn: a kitchen countertop's top is at 14.5 sixteenths, a dining table's at
 * 12, so a toaster or a plate on one drops that far to sit on it instead of hovering over it.
 * {@code gen_furniture_appliances.py} draws each countertop piece once per rest, moved down by
 * {@link #getDrop()}; the piece's box moves with it.
 *
 * @since 2026.9
 */
public enum SurfaceRest implements IStringSerializable {
  /** The floor, or anything full height: no drop. */
  FLOOR(0),
  /** A kitchen countertop, an island, the range, the dishwasher, the chest freezer's lid. */
  COUNTER(1.5),
  /** A sideboard, a dresser. */
  SIDEBOARD(2),
  /** A dining table, a cafe table, a desk. */
  TABLE(4),
  /** A side table, a nightstand. */
  SIDE_TABLE(7),
  /** A TV stand. */
  TV_STAND(8),
  /** A coffee table, a blanket chest. */
  COFFEE_TABLE(9);

  private final double drop;

  SurfaceRest(double drop) {
    this.drop = drop;
  }

  /**
   * How far below its block the piece is drawn, in sixteenths.
   *
   * @return the drop
   */
  public double getDrop() {
    return drop;
  }

  /**
   * What the piece at {@code pos} stands on.
   *
   * @param world the world
   * @param pos   the piece
   *
   * @return its rest
   */
  public static SurfaceRest under(IBlockAccess world, BlockPos pos) {
    IBlockState below = world.getBlockState(pos.down());
    Block block = below.getBlock();
    if (block instanceof BlockKitchenCabinet) {
      return ((BlockKitchenCabinet) block).getLine() == KitchenLine.WALL ? FLOOR : COUNTER;
    }
    if (block instanceof BlockBuiltInAppliance) {
      return ((BlockBuiltInAppliance) block).getLine() == KitchenLine.BASE ? COUNTER : FLOOR;
    }
    if (block instanceof BlockDiningTable) {
      return TABLE;
    }
    if (block instanceof BlockResidentialFurniture) {
      String name = ((BlockResidentialFurniture) block).getBlockRegistryName();
      if (name.startsWith("chest_freezer_")) {
        return COUNTER;
      }
      if (name.startsWith("cafe_table_")) {
        return TABLE;
      }
      if (name.startsWith("side_table_")) {
        return SIDE_TABLE;
      }
      if (name.startsWith("coffee_table_")) {
        return COFFEE_TABLE;
      }
      if (name.startsWith("tv_stand_")) {
        return TV_STAND;
      }
      if (name.startsWith("sideboard_")) {
        return SIDEBOARD;
      }
      // The bedroom's tops are drawn at heights the living room already has.
      if (name.startsWith("nightstand_")) {
        return SIDE_TABLE;
      }
      if (name.startsWith("dresser_") && !name.startsWith("dresser_mirror_")) {
        return SIDEBOARD;
      }
      if (name.startsWith("desk_") && !name.startsWith("desk_chair_")) {
        return TABLE;
      }
      if (name.startsWith("blanket_chest_")) {
        return COFFEE_TABLE;
      }
    }
    return FLOOR;
  }

  @Nonnull
  @Override
  public String getName() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}

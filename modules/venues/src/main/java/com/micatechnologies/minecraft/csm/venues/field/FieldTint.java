package com.micatechnologies.minecraft.csm.venues.field;

import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The tints of a football or soccer field: the mowing stripe's tone and the end zone's colour,
 * which every field picture is drawn without, so one picture serves both stripes and any colour.
 *
 * <p>A painted cell's top is cut into parts by tint index ({@code gen_venues_football.py}):</p>
 * <ul>
 *   <li>0: on the field, one tone: the tone of the nearest {@code field_turf} (light or dark, not
 *   paint or surround) along the stripe, the picture's north-south axis turned with the cell,
 *   which never crosses a stripe edge;</li>
 *   <li>1, 2: on the field, in a cell a stripe edge crosses (under a 5 yard line): the part west
 *   (1) or east (2) of the edge in the picture's frame, each taking the tone found along the
 *   stripe in the column beside the cell on its side;</li>
 *   <li>3: the end zone, the colour of a {@code football_end_zone} among the cell's eight
 *   neighbours.</li>
 * </ul>
 * <p>Parts off the field (the border, the surround) take no tint. With no turf found the tone is
 * light; with no end zone found the colour is the turf's, as if the end zone were grass.</p>
 *
 * <p>Called as a chunk section is built, with the cell's actual state: up to 17 block reads a
 * part. A tone changed further than a block from a cell across a section line is not seen there
 * until that section is built again.</p>
 *
 * @since 2026.10
 */
public final class FieldTint {

  /** The light stripe: the pictures as drawn. */
  public static final int LIGHT = 0xFFFFFF;
  /** The dark stripe, as a multiplier of the light. */
  public static final int DARK = 0xCBD8C8;
  /** An end zone with no colour block beside it: the zone paint tinted to the turf's green. */
  public static final int ZONE_GRASS = 0x63A840;

  /** How far along a stripe a cell looks for turf. */
  private static final int SEARCH = 8;

  private static final EnumFacing[] WEST_TURNED = {EnumFacing.WEST, EnumFacing.NORTH,
      EnumFacing.EAST, EnumFacing.SOUTH};

  private FieldTint() {
  }

  /**
   * The colour of a {@code field_turf} or {@code football_end_zone} block's tinted top.
   *
   * @param state the block's state
   *
   * @return the multiplier, -1 for none
   */
  public static int plain(IBlockState state) {
    Block b = state.getBlock();
    if (!(b instanceof BlockFieldTurf)) {
      return -1;
    }
    BlockFieldTurf t = (BlockFieldTurf) b;
    if (t.isEndZone()) {
      return VenueColours.rgb(t.value(state));
    }
    return t.value(state) == BlockFieldTurf.DARK ? DARK : LIGHT;
  }

  /**
   * The colour of a painted cell's or anchor's tinted part.
   *
   * @param state     the cell's actual state
   * @param world     the world, or null for an item
   * @param pos       the cell, or null for an item
   * @param tintIndex the part
   *
   * @return the multiplier, -1 for none
   */
  public static int cell(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex) {
    if (tintIndex < 0 || tintIndex > 3) {
      return -1;
    }
    if (world == null || pos == null) {
      return tintIndex == 3 ? VenueColours.rgb(VenueColours.NAVY) : LIGHT;
    }
    if (tintIndex == 3) {
      return zone(world, pos);
    }
    int rot = state.getBlock() instanceof IFieldCell
        ? ((IFieldCell) state.getBlock()).tintRotation(state) & 3 : 0;
    EnumFacing along = (rot & 1) == 0 ? EnumFacing.SOUTH : EnumFacing.WEST;
    BlockPos start = pos;
    if (tintIndex > 0) {
      EnumFacing west = WEST_TURNED[rot];
      start = pos.offset(tintIndex == 1 ? west : west.getOpposite());
    }
    return tone(world, start, along);
  }

  /** The tone of the first light or dark turf at {@code start} or along the stripe from it. */
  private static int tone(IBlockAccess world, BlockPos start, EnumFacing along) {
    int t = turf(world.getBlockState(start));
    if (t >= 0) {
      return t;
    }
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    for (int d = 1; d <= SEARCH; d++) {
      for (int sign = -1; sign <= 1; sign += 2) {
        at.setPos(start.getX() + along.getXOffset() * d * sign, start.getY(),
            start.getZ() + along.getZOffset() * d * sign);
        t = turf(world.getBlockState(at));
        if (t >= 0) {
          return t;
        }
      }
    }
    return LIGHT;
  }

  private static int turf(IBlockState s) {
    Block b = s.getBlock();
    if (!(b instanceof BlockFieldTurf) || ((BlockFieldTurf) b).isEndZone()) {
      return -1;
    }
    int k = ((BlockFieldTurf) b).value(s);
    return k == BlockFieldTurf.LIGHT ? LIGHT : k == BlockFieldTurf.DARK ? DARK : -1;
  }

  private static int zone(IBlockAccess world, BlockPos pos) {
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        at.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
        IBlockState s = world.getBlockState(at);
        if (s.getBlock() instanceof BlockFieldTurf && ((BlockFieldTurf) s.getBlock()).isEndZone()) {
          return VenueColours.rgb(((BlockFieldTurf) s.getBlock()).value(s));
        }
      }
    }
    return ZONE_GRASS;
  }
}

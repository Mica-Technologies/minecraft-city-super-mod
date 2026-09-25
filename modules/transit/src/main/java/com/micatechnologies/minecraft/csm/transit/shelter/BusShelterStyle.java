package com.micatechnologies.minecraft.csm.transit.shelter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * The three bus shelters and what differs between them in code: the render layer (glass is
 * translucent, a perforated screen is cut out) and what a player bumps into. The look is all
 * {@code gen_transit_shelters.py}'s; the numbers here are its, in sixteenths, facing north (the
 * open front at z = 0), measured in each half's own block.
 *
 * @since 2026.9
 */
public enum BusShelterStyle {
  /**
   * Glass and steel: glass back and end walls, a roof with a fascia, a bench, and at the
   * sitter's left end of the back row an empty frame where a Signage ad panel is set.
   */
  GLASS(BlockRenderLayer.TRANSLUCENT),
  /** A canopy on one column a block, open front and ends, a perforated screen and a bench. */
  CANTILEVER(BlockRenderLayer.CUTOUT),
  /** A thin flat roof on slim posts at its corners, and a lean rail. */
  FLAT(BlockRenderLayer.CUTOUT);

  private final BlockRenderLayer layer;

  BusShelterStyle(BlockRenderLayer layer) {
    this.layer = layer;
  }

  /** The layer the shelter is drawn in. */
  public BlockRenderLayer getLayer() {
    return layer;
  }

  /**
   * What a player collides with in one half of a shelter, facing north.
   *
   * @param upper  whether this is the upper half
   * @param left   the run continues to the sitter's left
   * @param right  the run continues to the sitter's right
   * @param ahead  the shelter continues in front: this is not the front row
   * @param behind the shelter continues behind: this is not the back row
   *
   * @return the boxes, in blocks
   */
  public List<AxisAlignedBB> collision(boolean upper, boolean left, boolean right,
      boolean ahead, boolean behind) {
    List<AxisAlignedBB> out = new ArrayList<>();
    switch (this) {
      case GLASS:
        // the walls stop under the roof; the roof deck is the top of the upper half
        double top = upper ? 13.5 : 16;
        if (!behind) {
          out.add(px(0, 0, 14.5, 16, top, 16));
          if (!upper) {
            out.add(px(0, 0, 10, 16, 8, 14.5)); // the bench
          }
        }
        if (!left) {
          out.add(px(0, 0, ahead ? 0 : 0.5, behind ? 1.5 : 2.75, top, 16));
        }
        if (!right) {
          out.add(px(14.5, 0, ahead ? 0 : 0.5, 16, top, 16));
        }
        if (upper) {
          out.add(px(0, 13.5, 0, 16, 16, 16));
        }
        break;
      case CANTILEVER:
        if (!behind) {
          out.add(px(7, 0, 14, 9, upper ? 13.5 : 16, 16)); // the column
          out.add(px(0, upper ? 0 : 1.5, 14.6, 16, upper ? 4 : 16, 15)); // the screen
          if (!upper) {
            out.add(px(0, 0, 10, 16, 8, 14)); // the bench
          }
        }
        if (upper) {
          out.add(px(0, 13.5, 0, 16, 15, 16)); // the canopy
        }
        break;
      default: // FLAT
        double h = upper ? 13 : 16;
        if (!behind && !upper) {
          out.add(px(0, 0, 13.3, 16, 13, 15)); // the lean rail
        }
        for (double x0 : new double[]{left ? -1 : 0.5, right ? -1 : 14.3}) {
          if (x0 < 0) {
            continue;
          }
          if (!ahead) {
            out.add(px(x0, 0, 1, x0 + 1.2, h, 2.2)); // a front post
          }
          if (!behind) {
            out.add(px(x0, 0, 13.8, x0 + 1.2, h, 15)); // a back post
          }
        }
        if (upper) {
          out.add(px(0, 13, 0, 16, 14.5, 16)); // the roof
        }
        break;
    }
    return out;
  }

  private static AxisAlignedBB px(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }
}

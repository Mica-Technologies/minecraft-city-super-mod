package com.micatechnologies.minecraft.csm.transit.shelter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * The three bus shelters and what differs between them in code: the render layer (glass is
 * translucent, a perforated screen is cut out) and what a player bumps into. The look is all
 * {@code gen_transit_shelters.py}'s; the numbers here are its, in sixteenths, facing north (the
 * open front at z = 0), in its "column coordinates": y from 0 to 48 over a three-section column
 * (BOTTOM 0..16, SECOND 16..32, TOP 32..48), each box cut to the section asked for. A box that
 * {@code runs} (a wall, a post) fills a MIDDLE section top to bottom.
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

  /** Where the glass shelter's frame stops at its ad panel end ({@code SLOT_X}). */
  private static final double SLOT_X = 1.25;

  private final BlockRenderLayer layer;

  BusShelterStyle(BlockRenderLayer layer) {
    this.layer = layer;
  }

  /** The layer the shelter is drawn in. */
  public BlockRenderLayer getLayer() {
    return layer;
  }

  /**
   * What a player collides with in one section of a shelter, facing north.
   *
   * @param section which section of its column
   * @param left    the run continues to the sitter's left
   * @param right   the run continues to the sitter's right
   * @param ahead   the shelter continues in front: this is not the front row
   * @param behind  the shelter continues behind: this is not the back row
   *
   * @return the boxes, in blocks
   */
  public List<AxisAlignedBB> collision(BusShelterSection section, boolean left, boolean right,
      boolean ahead, boolean behind) {
    Boxes b = new Boxes(section);
    switch (this) {
      case GLASS:
        if (!behind) {
          b.run(left ? 0 : SLOT_X, 0, 14.5, 16, 44, 16); // the back wall
          b.add(0, 0, 10, 16, 8, 14.5); // the bench
        }
        if (!right) {
          if (!behind) {
            b.run(14.5, 0, ahead ? 0 : 0.5, 16, 44, 16);
          } else if (!ahead) {
            b.run(14.5, 0, 0.5, 16, 44, 2);
          }
        }
        if (!left) {
          if (!behind) {
            b.run(SLOT_X, 0, ahead ? 0 : 0.5, SLOT_X + 1.5, 44, 16); // the ad panel frame
          } else if (!ahead) {
            b.run(0, 0, 0.5, 1.5, 44, 2);
          }
        }
        b.add(0, 44, 0, 16, 46, 16); // the roof
        break;
      case CANTILEVER:
        if (!behind) {
          b.run(7, 0, 14, 9, 44, 16); // the column
          b.add(0, 1, 14.4, 16, 23, 15.2); // the screen
          b.add(0, 0, 10, 16, 8, 14); // the bench
        }
        b.add(0, 44, 0, 16, 45.5, 16); // the canopy
        break;
      default: // FLAT
        if (!behind) {
          b.add(0, 0, 13.3, 16, 12.8, 14.9); // the lean rail
        }
        for (double x0 : new double[]{left ? -1 : 0.5, right ? -1 : 14.3}) {
          if (x0 < 0) {
            continue;
          }
          if (!ahead) {
            b.run(x0, 0, 1, x0 + 1.2, 43.5, 2.2); // a front post
          }
          if (!behind) {
            b.run(x0, 0, 13.8, x0 + 1.2, 43.5, 15); // a back post
          }
        }
        b.add(0, 43.5, 0, 16, 45, 16); // the roof
        break;
    }
    return b.out;
  }

  /** Boxes in column coordinates, cut to one section. */
  private static final class Boxes {

    private final BusShelterSection section;
    private final List<AxisAlignedBB> out = new ArrayList<>();

    private Boxes(BusShelterSection section) {
      this.section = section;
    }

    /** A box that does not reach into a MIDDLE section. */
    private void add(double x0, double y0, double z0, double x1, double y1, double z1) {
      put(x0, y0, z0, x1, y1, z1, false);
    }

    /** A box a MIDDLE section has the full height of: a wall or a post. */
    private void run(double x0, double y0, double z0, double x1, double y1, double z1) {
      put(x0, y0, z0, x1, y1, z1, true);
    }

    private void put(double x0, double y0, double z0, double x1, double y1, double z1,
        boolean runs) {
      double lo;
      switch (section) {
        case MIDDLE:
          if (runs) {
            out.add(px(x0, 0, z0, x1, 16, z1));
          }
          return;
        case BOTTOM:
          lo = 0;
          break;
        case SECOND:
          lo = 16;
          break;
        default:
          lo = 32;
          break;
      }
      double a = Math.max(y0, lo);
      double c = Math.min(y1, lo + 16);
      if (c > a) {
        out.add(px(x0, a - lo, z0, x1, c - lo, z1));
      }
    }
  }

  private static AxisAlignedBB px(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }
}

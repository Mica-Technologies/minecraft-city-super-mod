package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPoleDiagonal;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleMastArmCurve;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;

/**
 * What an unlinked signal cluster hanger hangs from: the block directly above it, read once and
 * turned into the one piece of hardware that meets it.
 *
 * <ul>
 *   <li>{@link Kind#ARM}: a pipe runs through the block above -- a straight or diagonal arm (a
 *       traffic pole lying on its side), or a curved or truss arm whose own cell does not grow a
 *       stub down. The hanger clamps a band round that pipe, along its run, and drops its pipe
 *       from the band.</li>
 *   <li>{@link Kind#FLANGE}: something flat to bolt to -- a mast arm curve cell that grows its own
 *       stub and plate down to the hanger, an upright pole's end, or any other solid block. A
 *       flange on the block boundary and the pipe below it.</li>
 *   <li>{@link Kind#NONE}: nothing above. The hub hangs on its own.</li>
 * </ul>
 *
 * <p>The geometry is in blocks, in world space. Kept free of rendering so it can be tested.
 */
public final class SignalClusterUpperMount {

  /** What kind of hardware meets the block above. */
  public enum Kind {
    NONE,
    FLANGE,
    ARM
  }

  /** The diagonal arms are all drawn from one thin model: a 4 px radius, whatever the finish. */
  static final double DIAGONAL_RADIUS_PX = 4.0;

  /** How far sideways of a curve's rail the hanger may be and still clamp it, past its radius. */
  static final double RAIL_GRIP_TOLERANCE_PX = 1.0;

  /** Nothing above. */
  public static final SignalClusterUpperMount NONE =
      new SignalClusterUpperMount(Kind.NONE, null, null, 0.0);

  private final Kind kind;
  private final Vec3d centre;
  private final Vec3d axis;
  private final double radius;

  private SignalClusterUpperMount(Kind kind, Vec3d centre, Vec3d axis, double radius) {
    this.kind = kind;
    this.centre = centre;
    this.axis = axis;
    this.radius = radius;
  }

  /**
   * A flange on the underside of the block above the hanger.
   *
   * @param hangerPos the hanger's position
   *
   * @return the mount, centred on the block's bottom face
   */
  public static SignalClusterUpperMount flange(BlockPos hangerPos) {
    return new SignalClusterUpperMount(Kind.FLANGE,
        new Vec3d(hangerPos.getX() + 0.5, hangerPos.getY() + 1.0, hangerPos.getZ() + 0.5), null,
        0.0);
  }

  /**
   * A clamp round a pipe.
   *
   * @param centre   a point on the pipe's centreline, over the hanger
   * @param axis     the pipe's direction (normalised here)
   * @param radiusPx the pipe's radius, in sixteenths
   *
   * @return the mount
   */
  public static SignalClusterUpperMount arm(Vec3d centre, Vec3d axis, double radiusPx) {
    return new SignalClusterUpperMount(Kind.ARM, centre, axis.normalize(), radiusPx / 16.0);
  }

  /**
   * Reads the block above a hanger.
   *
   * @param world     the world
   * @param hangerPos the hanger's position
   *
   * @return the mount; never null
   */
  public static SignalClusterUpperMount find(IBlockAccess world, BlockPos hangerPos) {
    final BlockPos above = hangerPos.up();
    final IBlockState state = world.getBlockState(above);
    final Block block = state.getBlock();

    if (block instanceof BlockTrafficPoleMastArmCurve) {
      final BlockTrafficPoleMastArmCurve curve = (BlockTrafficPoleMastArmCurve) block;
      // The cell grows its band, bracket and plate down to an unlinked hanger
      // (BlockTrafficPoleMastArmCurve.isHangingWireMount), so meet its plate with a flange.
      if (curve.hangsHardwareBelow(world, above)) {
        return flange(hangerPos);
      }
      final SignalClusterUpperMount rail =
          fromRails(curve.getWorldRails(world, above, state), above);
      return rail != null ? rail : flange(hangerPos);
    }

    if (block instanceof AbstractBlockTrafficPole
        && state.getPropertyKeys().contains(AbstractBlockRotatableNSEWUD.FACING)) {
      final EnumFacing facing = state.getValue(AbstractBlockRotatableNSEWUD.FACING);
      if (facing.getAxis() == EnumFacing.Axis.Y) {
        // An upright pole's end over the hanger: bolt to it.
        return flange(hangerPos);
      }
      final Vec3d centre = new Vec3d(above.getX() + 0.5, above.getY() + 0.5, above.getZ() + 0.5);
      if (block instanceof AbstractBlockTrafficPoleDiagonal) {
        return arm(centre, diagonalAxis(facing), DIAGONAL_RADIUS_PX);
      }
      return arm(centre, straightAxis(facing),
          ((AbstractBlockTrafficPole) block).getPoleRadius());
    }

    if (AbstractBlockTrafficPole.isMountableAdjacent(world, above,
        AbstractBlockTrafficPole.IGNORE_BLOCK)) {
      return flange(hangerPos);
    }
    return NONE;
  }

  /**
   * The run of a straight arm: a horizontal pole's tube lies along its facing.
   *
   * @param facing a horizontal facing
   *
   * @return a unit vector along the arm
   */
  static Vec3d straightAxis(EnumFacing facing) {
    return new Vec3d(facing.getXOffset(), 0.0, facing.getZOffset());
  }

  /**
   * The run of a diagonal arm. Its model is a tube along Z turned -45 degrees about Y, which puts
   * it along (-1, 1) in X and Z (that is, (1, -1)) when facing north; the blockstate turns that by
   * 90 degrees for east and west.
   *
   * @param facing a horizontal facing
   *
   * @return a unit vector along the arm
   */
  static Vec3d diagonalAxis(EnumFacing facing) {
    final double h = Math.sqrt(0.5);
    return facing.getAxis() == EnumFacing.Axis.Z
        ? new Vec3d(h, 0.0, -h)
        : new Vec3d(h, 0.0, h);
  }

  /**
   * The lowest pipe of a curved arm's cell that passes over the middle of the block under it.
   *
   * @param rails the cell's segments, {@code {x0, y0, z0, x1, y1, z1, radius}} in sixteenths from
   *              its lower north-west corner, as {@link BlockTrafficPoleMastArmCurve#getWorldRails}
   *              gives them; may be null
   * @param cell  the cell's position
   *
   * @return a clamp on that pipe, or null when none passes over
   */
  static SignalClusterUpperMount fromRails(float[][] rails, BlockPos cell) {
    if (rails == null) {
      return null;
    }
    SignalClusterUpperMount best = null;
    double bestY = Double.POSITIVE_INFINITY;
    for (float[] r : rails) {
      final double ax = r[0];
      final double az = r[2];
      final double dx = r[3] - ax;
      final double dy = r[4] - r[1];
      final double dz = r[5] - az;
      final double lengthSq = dx * dx + dz * dz;
      if (lengthSq < 1.0e-6) {
        continue;
      }
      // Nearest point of the segment's plan to the block's middle.
      double t = ((8.0 - ax) * dx + (8.0 - az) * dz) / lengthSq;
      t = Math.max(0.0, Math.min(1.0, t));
      final double px = ax + dx * t;
      final double pz = az + dz * t;
      final double off = Math.sqrt((px - 8.0) * (px - 8.0) + (pz - 8.0) * (pz - 8.0));
      if (off > r[6] + RAIL_GRIP_TOLERANCE_PX) {
        continue;
      }
      final double py = r[1] + dy * t;
      if (py - r[6] < bestY) {
        bestY = py - r[6];
        best = arm(new Vec3d(cell.getX() + px / 16.0, cell.getY() + py / 16.0,
            cell.getZ() + pz / 16.0), new Vec3d(dx, dy, dz), r[6]);
      }
    }
    return best;
  }

  public Kind getKind() {
    return kind;
  }

  /** For an arm, a point on the pipe's centreline; for a flange, the middle of the face. */
  public Vec3d getCentre() {
    return centre;
  }

  /** For an arm, the unit direction of the pipe. */
  public Vec3d getAxis() {
    return axis;
  }

  /** For an arm, the pipe's radius in blocks. */
  public double getRadius() {
    return radius;
  }

  /** Folds this mount into a display list key. */
  public long stateKey() {
    long key = kind.ordinal();
    if (centre != null) {
      key = key * 31L + Math.round(centre.x * 64.0);
      key = key * 31L + Math.round(centre.y * 64.0);
      key = key * 31L + Math.round(centre.z * 64.0);
    }
    if (axis != null) {
      key = key * 31L + Math.round(axis.x * 64.0);
      key = key * 31L + Math.round(axis.y * 64.0);
      key = key * 31L + Math.round(axis.z * 64.0);
    }
    return key * 31L + Math.round(radius * 256.0);
  }
}

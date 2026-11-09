package com.micatechnologies.minecraft.csm.codeutils.light;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The light-up air helpers every light shares: Lighting's one-per-fixture column projection, moved
 * here unchanged from {@code AbstractBrightLight}; the floodlights' beam ({@link #projectBeam}), the
 * same rule along any aim; and the scans the stadium light footprints ({@link CsmLightFootprints})
 * are built from. None of them loads a chunk.
 */
public final class CsmLightupAir {

  /** How far below a fixture its column projection looks. */
  public static final int COLUMN_DEPTH = 16;

  private CsmLightupAir() {
  }

  /**
   * The light-up air block.
   *
   * @return the block, or {@code null} before Core's hidden tab has built it
   */
  @Nullable
  public static Block block() {
    return BlockLightupAir.get();
  }

  /**
   * Whether a state is light-up air.
   *
   * @param state a block state
   *
   * @return {@code true} if it is light-up air
   */
  public static boolean is(IBlockState state) {
    Block air = block();
    return air != null && state.getBlock() == air;
  }

  /**
   * A fixture's column projection, Lighting's rule: turning on, it places one light-up air in the
   * lowest air cell of the column below the fixture (offset by {@code xOffset}, {@code zOffset}),
   * down to {@link #COLUMN_DEPTH}, unless that is the cell straight under it or the column already
   * holds one; turning off, it removes the first light-up air in the column. A column in an
   * unloaded chunk is left alone.
   *
   * @param on      {@code true} to light, {@code false} to remove
   * @param world   the world
   * @param pos     the fixture's position
   * @param xOffset the column's x offset from the fixture
   * @param zOffset the column's z offset from the fixture
   *
   * @return {@code true} if a light-up air was placed
   */
  public static boolean projectColumn(boolean on, World world, BlockPos pos, int xOffset,
      int zOffset) {
    Block lightupAir = block();
    if (lightupAir == null) {
      return false;
    }
    final int xWithOffset = pos.getX() + xOffset;
    final int zWithOffset = pos.getZ() + zOffset;
    // Add light
    if (on) {
      BlockPos doAddAt = null;
      for (int findy = -1; findy >= -COLUMN_DEPTH; findy--) {
        final int yWithOffset = pos.getY() + findy;
        BlockPos test = new BlockPos(xWithOffset, yWithOffset, zWithOffset);
        // An offset column can reach into a neighbouring chunk; never load one to light it
        if (!world.isBlockLoaded(test)) {
          return false;
        }
        // Already lit: a lightupair is not air, so without this the scan would stop on it and
        // place a second one in the cell above, and every later call another
        if (world.getBlockState(test).getBlock() == lightupAir) {
          return false;
        }
        // found block that is not air
        if (world.isAirBlock(test)) {
          // dont add light if block is right below street light
          if (findy < -1) {
            doAddAt = test;
          }
        } else {
          break;
        }
      }
      // add if marked for add
      if (doAddAt != null) {
        world.setBlockState(doAddAt, lightupAir.getDefaultState(), 3);
        return true;
      }
    }
    // Remove light
    else {
      for (int findy = -1; findy >= -COLUMN_DEPTH; findy--) {
        final int yWithOffset = pos.getY() + findy;
        BlockPos test = new BlockPos(xWithOffset, yWithOffset, zWithOffset);
        // Verify the position is in a loaded chunk before accessing block state
        if (!world.isBlockLoaded(test)) {
          continue;
        }
        IBlockState bs = world.getBlockState(test);
        // stop removing light once hit block
        if (bs.getBlock() == lightupAir) {
          world.setBlockToAir(test);
          break;
        }
      }
    }
    return false;
  }

  /** The longest beam {@link #projectBeam} follows, in blocks. */
  public static final int BEAM_MAX_RANGE = 48;

  /** How near the fixture a beam's light-up air may go: never in the cells round it. */
  public static final double BEAM_MIN_DISTANCE = 2.0;

  /**
   * The cells a beam passes through, the column projection generalised from straight down to any
   * aim: from the fixture's centre along {@code (dx, dy, dz)}, every cell the line enters
   * (Amanatides and Woo), until it meets something that is neither air nor light-up air or runs
   * {@code range} blocks. Light-up air is passed through, so a beam's own cells never stop it.
   *
   * @param world the world
   * @param pos   the fixture's position (its own cell is not part of the path)
   * @param dx    the aim's x
   * @param dy    the aim's y
   * @param dz    the aim's z
   * @param range how far the beam reaches, in blocks
   *
   * @return the cells passed, in order from the fixture; {@code null} if the beam reached an
   *     unloaded chunk or left the world first (a beam is never half-lit) or met nothing in range
   */
  @Nullable
  public static List<BlockPos> beamPath(World world, BlockPos pos, double dx, double dy, double dz,
      int range) {
    Block lightupAir = block();
    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (lightupAir == null || len < 1e-9) {
      return null;
    }
    dx /= len;
    dy /= len;
    dz /= len;
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    int stepX = dx > 0 ? 1 : dx < 0 ? -1 : 0;
    int stepY = dy > 0 ? 1 : dy < 0 ? -1 : 0;
    int stepZ = dz > 0 ? 1 : dz < 0 ? -1 : 0;
    // From the centre of the cell, the first boundary is half a block away on every axis
    double tDeltaX = stepX == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(dx);
    double tDeltaY = stepY == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(dy);
    double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(dz);
    double tMaxX = tDeltaX / 2;
    double tMaxY = tDeltaY / 2;
    double tMaxZ = tDeltaZ / 2;
    List<BlockPos> path = new ArrayList<>();
    BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos();
    while (true) {
      double t;
      if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
        t = tMaxX;
        x += stepX;
        tMaxX += tDeltaX;
      } else if (tMaxY <= tMaxZ) {
        t = tMaxY;
        y += stepY;
        tMaxY += tDeltaY;
      } else {
        t = tMaxZ;
        z += stepZ;
        tMaxZ += tDeltaZ;
      }
      if (t > range) {
        return null;
      }
      if (y < 0 || y > 255) {
        return null;
      }
      test.setPos(x, y, z);
      if (!world.isBlockLoaded(test)) {
        return null;
      }
      IBlockState state = world.getBlockState(test);
      if (state.getBlock() != lightupAir && !state.getBlock().isAir(state, world, test)) {
        return path;
      }
      path.add(test.toImmutable());
    }
  }

  /**
   * Where a beam's light-up air goes: the last cell of its path, the one just in front of what
   * it lights, then a cell every {@code spacing} blocks back along the beam toward the fixture,
   * {@code count} in all, none nearer the fixture than {@link #BEAM_MIN_DISTANCE}.
   *
   * @param path    a beam's path ({@link #beamPath})
   * @param pos     the fixture's position
   * @param count   how many cells, at most
   * @param spacing how far apart along the beam, in blocks
   *
   * @return the cells, from the far end back
   */
  public static List<BlockPos> beamTargets(List<BlockPos> path, BlockPos pos, int count,
      double spacing) {
    List<BlockPos> targets = new ArrayList<>(count);
    BlockPos last = null;
    for (int i = path.size() - 1; i >= 0 && targets.size() < count; i--) {
      BlockPos cell = path.get(i);
      if (Math.sqrt(cell.distanceSq(pos)) < BEAM_MIN_DISTANCE) {
        break;
      }
      if (last == null || Math.sqrt(cell.distanceSq(last)) >= spacing - 1e-9) {
        targets.add(cell);
        last = cell;
      }
    }
    return targets;
  }

  /**
   * Where a beam's row light settles: on a beam aimed below level, the row behind the lit spot
   * lies along the floor under the beam rather than in the air, so each row cell drops down its
   * column to just above whatever is below it (the column rule), stopping on a light-up air
   * already there. The lit spot itself (the first target) and every cell of a beam aimed level or
   * up stay where they are.
   *
   * @param world the world
   * @param cell  a target on the beam
   *
   * @return the settled cell (air, or the light-up air already lighting it); {@code null} if the
   *     column runs into an unloaded chunk
   */
  @Nullable
  public static BlockPos settle(World world, BlockPos cell) {
    Block lightupAir = block();
    BlockPos at = cell;
    for (int i = 0; i < COLUMN_DEPTH && at.getY() > 0; i++) {
      BlockPos below = at.down();
      if (!world.isBlockLoaded(below)) {
        return null;
      }
      IBlockState state = world.getBlockState(below);
      if (state.getBlock() == lightupAir) {
        return below;
      }
      if (!state.getBlock().isAir(state, world, below)) {
        break;
      }
      at = below;
    }
    return at;
  }

  /**
   * The cells a lit beam keeps light-up air in: its {@link #beamTargets}, the row behind the lit
   * spot {@link #settle}d onto the floor when the beam aims below level.
   *
   * @return the cells, the lit spot first; a row cell over an unloaded column is left out
   */
  public static List<BlockPos> beamLights(World world, List<BlockPos> path, BlockPos pos,
      double dy, int count, double spacing) {
    List<BlockPos> targets = beamTargets(path, pos, count, spacing);
    if (dy >= 0) {
      return targets;
    }
    List<BlockPos> lights = new ArrayList<>(targets.size());
    for (int i = 0; i < targets.size(); i++) {
      BlockPos cell = i == 0 ? targets.get(0) : settle(world, targets.get(i));
      if (cell != null) {
        lights.add(cell);
      }
    }
    return lights;
  }

  /**
   * A beam's projection, the floodlights' rule: turning on, it places light-up air in the beam's
   * {@link #beamLights} that are still air; turning off, it removes every light-up air along the
   * beam up to what it lights, and the first one down the column under each row cell. Called on a
   * state change and from the heal only, never on a tick, as the column projection is. A beam
   * that reaches an unloaded chunk, or meets nothing within {@code range}, places nothing.
   *
   * @param on      {@code true} to light, {@code false} to remove
   * @param world   the world
   * @param pos     the fixture's position
   * @param dx      the aim's x
   * @param dy      the aim's y
   * @param dz      the aim's z
   * @param range   how far the beam reaches, in blocks
   * @param count   how many light-up air the beam keeps lit
   * @param spacing how far apart along the beam, in blocks
   *
   * @return how many light-up air were placed or removed
   */
  public static int projectBeam(boolean on, World world, BlockPos pos, double dx, double dy,
      double dz, int range, int count, double spacing) {
    Block lightupAir = block();
    if (lightupAir == null) {
      return 0;
    }
    List<BlockPos> path = beamPath(world, pos, dx, dy, dz, range);
    if (path == null) {
      return 0;
    }
    int changed = 0;
    if (on) {
      for (BlockPos cell : beamLights(world, path, pos, dy, count, spacing)) {
        if (world.isAirBlock(cell)) {
          world.setBlockState(cell, lightupAir.getDefaultState(), 3);
          changed++;
        }
      }
    } else {
      for (BlockPos cell : path) {
        if (world.getBlockState(cell).getBlock() == lightupAir) {
          world.setBlockToAir(cell);
          changed++;
        }
      }
      if (dy < 0) {
        List<BlockPos> targets = beamTargets(path, pos, count, spacing);
        for (int i = 1; i < targets.size(); i++) {
          BlockPos below = firstBelow(world, targets.get(i).getX(), targets.get(i).getY() - 1,
              targets.get(i).getZ(), COLUMN_DEPTH);
          if (below != null) {
            world.setBlockToAir(below);
            changed++;
          }
        }
      }
    }
    return changed;
  }

  /**
   * The light-up air a scan down a column meets before anything else that is not air.
   *
   * @param world the world
   * @param x     the column's x
   * @param fromY the first cell scanned
   * @param z     the column's z
   * @param depth how many cells to scan
   *
   * @return its position, or {@code null} if the scan meets something else first, runs out, or
   *     the column is not loaded
   */
  @Nullable
  public static BlockPos firstBelow(World world, int x, int fromY, int z, int depth) {
    Block lightupAir = block();
    if (lightupAir == null) {
      return null;
    }
    BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos(x, fromY, z);
    if (!world.isBlockLoaded(test)) {
      return null;
    }
    int bottom = Math.max(0, fromY - depth + 1);
    for (int y = Math.min(255, fromY); y >= bottom; y--) {
      test.setY(y);
      IBlockState state = world.getBlockState(test);
      if (state.getBlock() == lightupAir) {
        return test.toImmutable();
      }
      if (!state.getBlock().isAir(state, world, test)) {
        return null;
      }
    }
    return null;
  }

  /**
   * Whether a fixture ({@link ICsmLightupAirSource}) above a light-up air still relies on it.
   * Reads up to {@code (2 * SEARCH_X + 1) * SEARCH_UP} cells, loaded chunks only.
   *
   * @param world the world
   * @param air   the light-up air's position
   *
   * @return {@code true} if some lit fixture claims it
   */
  public static boolean claimedBySource(World world, BlockPos air) {
    BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos();
    for (int dx = -ICsmLightupAirSource.SEARCH_X; dx <= ICsmLightupAirSource.SEARCH_X; dx++) {
      test.setPos(air.getX() + dx, air.getY(), air.getZ());
      if (!world.isBlockLoaded(test)) {
        continue;
      }
      int top = Math.min(255, air.getY() + ICsmLightupAirSource.SEARCH_UP);
      for (int y = air.getY() + 1; y <= top; y++) {
        test.setY(y);
        IBlockState state = world.getBlockState(test);
        if (state.getBlock() instanceof ICsmLightupAirSource
            && ((ICsmLightupAirSource) state.getBlock()).claimsLightupAir(world,
            test.toImmutable(), state, air)) {
          return true;
        }
      }
    }
    return false;
  }
}

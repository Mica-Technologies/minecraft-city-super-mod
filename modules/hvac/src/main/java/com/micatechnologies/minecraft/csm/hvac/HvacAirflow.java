package com.micatechnologies.minecraft.csm.hvac;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Which blocks air passes through, for the thermal scanner.
 *
 * <p>The rule has to be generic: this module may reference Core and vanilla only, yet the rooms it
 * measures are built from every module's blocks (the building module's doors, glazing and wall
 * finishes above all). So a block is judged by its shape and its state, not its class:</p>
 * <ul>
 *   <li>Air, replaceable blocks (grass, snow layers) and blocks with no collision box (torches,
 *   signs) pass air.</li>
 *   <li>Anything with an {@code open} property set, or a {@code motion} of {@code open}, passes
 *   air: vanilla and CSM doors, fence gates, trapdoors and the garage doors.</li>
 *   <li>Closed trapdoors, slabs and stairs block it (they are floors and roofs).</li>
 *   <li>A full cube blocks it.</li>
 *   <li>A box spanning the cell in two directions is a plane. A vertical plane blocks air: glass
 *   panes, closed doors, glazing. A horizontal plane blocks it only when it is thicker than a
 *   quarter block, so carpets, floor finishes and flush ceiling vents stay part of the room.</li>
 *   <li>Everything else (furniture, fences, posts, lights, the thermostats themselves) passes
 *   air, so a table does not cut a room in two.</li>
 * </ul>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class HvacAirflow {

  private static final double FULL = 0.99;
  private static final double THIN_HORIZONTAL = 0.25;

  private HvacAirflow() {
  }

  /**
   * Whether air passes through the block at {@code pos}.
   *
   * @param world the world
   * @param pos   the position; must be loaded
   *
   * @return true when the cell counts as air for the thermal scanner
   */
  public static boolean passesAir(World world, BlockPos pos) {
    return passesAir(world, pos, world.getBlockState(pos));
  }

  /**
   * Whether air passes through {@code state} standing at {@code pos}. The block listener asks this
   * of the state a block had before a change as well as of the one it has now.
   */
  public static boolean passesAir(World world, BlockPos pos, IBlockState state) {
    Material material = state.getMaterial();
    if (material == Material.AIR) {
      return true;
    }
    if (material.isLiquid()) {
      return false;
    }
    Block block = state.getBlock();
    Boolean open = openState(world, pos, state);
    if (open != null && open) {
      return true;
    }
    if (block instanceof BlockTrapDoor) {
      return false;
    }
    if (material.isReplaceable()) {
      return true;
    }
    if (block instanceof BlockSlab || block instanceof BlockStairs) {
      return false;
    }
    if (block instanceof BlockFence || block instanceof BlockWall) {
      return true;
    }
    AxisAlignedBB box;
    try {
      box = state.getCollisionBoundingBox(world, pos);
    } catch (RuntimeException e) {
      box = Block.FULL_BLOCK_AABB; // a block that cannot answer is treated as solid
    }
    if (box == null) {
      return true;
    }
    boolean fullX = box.maxX - box.minX >= FULL;
    boolean fullY = box.maxY - box.minY >= FULL;
    boolean fullZ = box.maxZ - box.minZ >= FULL;
    if (fullX && fullY && fullZ) {
      return false;
    }
    if (fullX && fullZ) {
      return box.maxY - box.minY <= THIN_HORIZONTAL;
    }
    return !(fullY && (fullX || fullZ));
  }

  /**
   * Reads an {@code open} boolean or a {@code motion=open} enum from the block's actual state,
   * or returns null when the block has neither. The actual state is needed for a vanilla door's
   * upper half, whose stored state never says it is open.
   */
  private static Boolean openState(World world, BlockPos pos, IBlockState state) {
    boolean has = false;
    for (IProperty<?> p : state.getPropertyKeys()) {
      String name = p.getName();
      if ("open".equals(name) || "motion".equals(name)) {
        has = true;
        break;
      }
    }
    if (!has) {
      return null;
    }
    IBlockState actual = state;
    try {
      actual = state.getActualState(world, pos);
    } catch (RuntimeException ignored) {
      // keep the stored state
    }
    for (IProperty<?> p : actual.getPropertyKeys()) {
      String name = p.getName();
      Comparable<?> value = actual.getValue(p);
      if ("open".equals(name) && value instanceof Boolean) {
        return (Boolean) value;
      }
      if ("motion".equals(name) && value instanceof IStringSerializable) {
        String v = ((IStringSerializable) value).getName();
        return "open".equals(v) || "moving".equals(v);
      }
    }
    return null;
  }

  /** How readily heat crosses a wall of this block, relative to stone. */
  public static float wallFactor(IBlockState state) {
    Material m = state.getMaterial();
    if (m == Material.GLASS || m == Material.ICE || m == Material.PACKED_ICE) {
      return 2.0f;
    }
    if (m == Material.CLOTH || m == Material.CARPET || m == Material.SNOW
        || m == Material.CRAFTED_SNOW) {
      return 0.35f;
    }
    if (m == Material.WOOD) {
      return 0.8f;
    }
    return 1.0f;
  }

  /**
   * The world as the scanner sees it. A passable cell is enclosed air when something that blocks
   * airflow lies somewhere above it in its column, and sky air otherwise.
   *
   * <p>The roof test deliberately uses the airflow rule rather than Minecraft's own sky and
   * precipitation heights: those count a thermostat, a lamp or a sign as a roof over the air
   * beneath it (so a thermostat on a post outdoors would make a two-block "room" of its own), and a
   * light-based test would count a glass roof as open sky. The precipitation height is used only
   * to start the downward search, since nothing above it blocks anything. Each column's answer is
   * cached for the life of the source, so make a new source for each scan.</p>
   */
  public static ThermalCellSource worldSource(World world) {
    return new ThermalCellSource() {
      private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
      private final Long2IntOpenHashMap roofCache = new Long2IntOpenHashMap();

      {
        roofCache.defaultReturnValue(Integer.MIN_VALUE);
      }

      @Override
      public byte classify(int x, int y, int z) {
        if (y < 0) {
          return WALL;
        }
        if (y >= world.getHeight()) {
          return SKY;
        }
        probe.setPos(x, y, z);
        if (!world.isBlockLoaded(probe)) {
          return UNLOADED;
        }
        if (!passesAir(world, probe)) {
          return WALL;
        }
        return y < roofOf(x, z) ? AIR : SKY;
      }

      /** Highest airflow-blocking block in the column, or -1. */
      private int roofOf(int x, int z) {
        long key = ThermalScanner.pack(x, 0, z);
        int cached = roofCache.get(key);
        if (cached != Integer.MIN_VALUE) {
          return cached;
        }
        probe.setPos(x, 0, z);
        int top = world.getPrecipitationHeight(probe).getY() - 1;
        int roof = -1;
        for (int yy = top; yy >= 0; yy--) {
          probe.setPos(x, yy, z);
          if (!passesAir(world, probe)) {
            roof = yy;
            break;
          }
        }
        roofCache.put(key, roof);
        return roof;
      }

      @Override
      public float wallFactor(int x, int y, int z) {
        probe.setPos(x, y, z);
        return HvacAirflow.wallFactor(world.getBlockState(probe));
      }
    };
  }
}

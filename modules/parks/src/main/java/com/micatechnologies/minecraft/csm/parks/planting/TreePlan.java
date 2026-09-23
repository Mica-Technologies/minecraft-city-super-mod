package com.micatechnologies.minecraft.csm.parks.planting;

import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import com.micatechnologies.minecraft.csm.parks.trees.TreeWood;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * A tree as a generator lays it out, before any block is placed: which part goes in each cell,
 * relative to the base of the trunk. Parts are named, not block states, so a plan can be grown and
 * tested without a running game; {@code ItemTreePlantingTool} resolves them when it plants.
 *
 * <p>Logs win over leaves, and leaves over hanging moss; a log never replaces a log already
 * placed, so the trunk laid first keeps its width where a limb leaves it.</p>
 *
 * @since 2026.9
 */
public final class TreePlan {

  /** What a cell holds. */
  public enum Kind {
    LOG, LEAVES, HANGING
  }

  /** One cell's part. */
  public static final class Part {

    public final Kind kind;
    /** Block registry name (without namespace). */
    public final String block;
    /** Logs only: the axis the bark runs along. */
    public final EnumFacing.Axis axis;

    Part(Kind kind, String block, EnumFacing.Axis axis) {
      this.kind = kind;
      this.block = block;
      this.axis = axis;
    }
  }

  private final Map<BlockPos, Part> parts = new LinkedHashMap<>();

  /** Lays a log, unless a log is already there. Replaces leaves or moss. */
  public void log(BlockPos pos, TreeWood wood, TreeLogWidth width, EnumFacing.Axis axis) {
    Part there = parts.get(pos);
    if (there != null && there.kind == Kind.LOG) {
      return;
    }
    parts.put(pos, new Part(Kind.LOG, "tree_log_" + wood.getId() + "_" + width.getId(), axis));
  }

  /** Lays leaves where there is nothing, or only moss. */
  public void leaves(BlockPos pos, String block) {
    Part there = parts.get(pos);
    if (there == null || there.kind == Kind.HANGING) {
      parts.put(pos, new Part(Kind.LEAVES, block, null));
    }
  }

  /** Hangs moss where there is nothing. */
  public void hanging(BlockPos pos, String block) {
    parts.putIfAbsent(pos, new Part(Kind.HANGING, block, null));
  }

  public Part get(BlockPos pos) {
    return parts.get(pos);
  }

  public boolean isEmpty(BlockPos pos) {
    return !parts.containsKey(pos);
  }

  public Map<BlockPos, Part> parts() {
    return parts;
  }
}

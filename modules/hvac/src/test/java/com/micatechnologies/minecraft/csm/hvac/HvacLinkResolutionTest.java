package com.micatechnologies.minecraft.csm.hvac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * A thermostat's links resolved for a control step ({@link HvacSystemControl#resolveLinks}).
 * The case that mattered: a build by command writes the thermostat's links first and places the
 * units, zones and vents after, and the first step used to drop every link to a place still
 * empty, so the system came up with {@code tU 0} and no vents and saved its links that way.
 */
class HvacLinkResolutionTest {

  private static final BlockPos A = new BlockPos(1, 64, 1);
  private static final BlockPos B = new BlockPos(2, 64, 1);
  private static final BlockPos C = new BlockPos(3, 64, 1);

  /** A world of loaded positions and the tile entities at them. */
  private final Set<BlockPos> loaded = new HashSet<>();
  private final Map<BlockPos, Object> tiles = new HashMap<>();

  private HvacSystemControl.Links<TileEntityHvacHeater> resolve(List<BlockPos> links) {
    return HvacSystemControl.resolveLinks(links, loaded::contains, tiles::get,
        TileEntityHvacHeater.class);
  }

  @Test
  void aLinkToAPlaceNotBuiltYetIsKept() {
    List<BlockPos> links = new ArrayList<>(Arrays.asList(A, B));
    loaded.addAll(links);
    TileEntityHvacHeater a = new TileEntityHvacHeater();
    tiles.put(A, a);
    HvacSystemControl.Links<TileEntityHvacHeater> r = resolve(links);
    assertEquals(Arrays.asList(A, B), links, "the empty place's link survives the step");
    assertEquals(1, r.found.size());
    assertSame(a, r.found.get(0));
    assertEquals(0, r.unloaded, "an empty loaded place does not hold the system back");
    assertFalse(r.dropped, "nothing to save");

    // the unit is placed a step later: it joins with no relinking
    TileEntityHvacCooler b = new TileEntityHvacCooler();
    tiles.put(B, b);
    r = resolve(links);
    assertEquals(2, r.found.size());
  }

  @Test
  void anUnloadedLinkIsKeptAndCounted() {
    List<BlockPos> links = new ArrayList<>(Arrays.asList(A, B));
    loaded.add(A);
    tiles.put(A, new TileEntityHvacHeater());
    HvacSystemControl.Links<TileEntityHvacHeater> r = resolve(links);
    assertEquals(2, links.size());
    assertEquals(1, r.found.size());
    assertEquals(1, r.unloaded);
  }

  @Test
  void aLinkToSomethingElseIsDropped() {
    List<BlockPos> links = new ArrayList<>(Arrays.asList(A, B, C));
    loaded.addAll(links);
    tiles.put(A, new TileEntityHvacHeater());
    tiles.put(B, new TileEntityHvacThermostat()); // a thermostat built where a unit was
    HvacSystemControl.Links<TileEntityHvacHeater> r = resolve(links);
    assertEquals(Arrays.asList(A, C), links, "B dropped, the empty C kept");
    assertEquals(1, r.found.size());
    assertTrue(r.dropped, "the owner saves the shorter list");
  }

  @Test
  void combinationUnitsAreUnitsToo() {
    List<BlockPos> links = new ArrayList<>(Arrays.asList(A, B));
    loaded.addAll(links);
    tiles.put(A, new TileEntityHvacPtac());
    tiles.put(B, new TileEntityHvacPackagedRtu());
    assertEquals(2, resolve(links).found.size());
  }
}

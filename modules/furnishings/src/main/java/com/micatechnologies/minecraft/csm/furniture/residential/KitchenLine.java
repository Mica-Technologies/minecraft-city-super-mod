package com.micatechnologies.minecraft.csm.furniture.residential;

/**
 * Which run a kitchen cabinet belongs to. Cabinets join only their own line: base cabinets
 * (with the sink and the corner) into a countertop run, wall cabinets and open shelves into a
 * row, islands into an island.
 *
 * @since 2026.9
 */
public enum KitchenLine {
  /** Base cabinets under a countertop, the sink base and the corner base. */
  BASE,
  /** Wall cabinets and open shelves. */
  WALL,
  /** The double-sided island. */
  ISLAND
}

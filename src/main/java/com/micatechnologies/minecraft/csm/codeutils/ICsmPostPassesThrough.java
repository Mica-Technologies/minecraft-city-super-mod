package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marker interface for a low roadside block that a pole or sign post stood on top of passes down
 * through to the ground, rather than stopping on.
 *
 * <p>A guardrail is the case this exists for. Signs and poles are put up behind and among the rail
 * all the time, and a builder places them the obvious way, on top of the run. Stopping the post on
 * the rail leaves it hovering a whole block above the ground everything else stands on. So a
 * vertical {@link AbstractBlockTrafficPole} and a sign post both look at the block under them, and
 * when it carries this marker they draw one more block of themselves reaching down through it.</p>
 *
 * <p>Type-based, and in Core rather than beside the guardrail, for the same reason as
 * {@link ICsmTrafficPoleIgnored}: the pole and the sign are in different subsystems from the
 * barrier, and a marker is the one thing all three can see without importing each other.</p>
 *
 * <p>Only a block that stops short of the top of its own cell should carry it. The reach is
 * exactly one block, from the bottom of the post to the floor of the cell below.</p>
 *
 * @see AbstractBlockTrafficPole#EXTEND_DOWN
 * @since 2026.9
 */
public interface ICsmPostPassesThrough {
}

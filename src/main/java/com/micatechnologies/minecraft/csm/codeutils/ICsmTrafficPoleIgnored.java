package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Marker interface for blocks a traffic pole must never sprout a mount stub into, whatever
 * subsystem they belong to.
 *
 * <p>A pole decides which of its faces grow a mount by looking at what is next to them
 * ({@link AbstractBlockTrafficPole#isMountableAdjacent}). Blocks that already draw their own
 * mounting hardware — crosswalk signal mounts, signal heads, sensor housings, mast arm curves,
 * beacons — would end up with two contradictory connections on the same joint if the pole added
 * one as well, so they are ignored.
 *
 * <p>This interface is what {@link AbstractBlockTrafficPole#IGNORE_BLOCK} used to spell out as a
 * list of concrete block classes. That list is matched by assignability, so implementing this on
 * an abstract base class covers every subclass exactly as listing the base class did — which is
 * how the signal head and new-style crosswalk signal families are covered. The remaining entries
 * in {@code IGNORE_BLOCK} are vanilla blocks, which cannot implement a CSM interface.
 *
 * <p>The pole additionally honours the user's {@code trafficPoleIgnoreBlocks} config list and
 * {@link ICsmTrafficPoleStateIgnored}, for blocks that are only ignored in some of their states.
 *
 * @see AbstractBlockTrafficPole
 * @see ICsmTrafficPoleStateIgnored
 * @since 2026.9
 */
public interface ICsmTrafficPoleIgnored {
}

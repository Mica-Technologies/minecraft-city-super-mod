package com.micatechnologies.minecraft.csm.furniture.residential;

/**
 * A Furniture &amp; Novelties block that switches with redstone -- a lamp, a light, a ceiling fan,
 * candles, the fireplace -- and so is something a light switch can be linked to by clicking it
 * with the switch in hand ({@link SwitchLinks#isTarget}). It reads its power the normal way, from
 * its neighbours, so a linked switch reaches it through a relay beside it
 * ({@link BlockSwitchRelay}) as it would any vanilla block.
 *
 * @since 2026.9
 */
public interface ISwitchable {
}

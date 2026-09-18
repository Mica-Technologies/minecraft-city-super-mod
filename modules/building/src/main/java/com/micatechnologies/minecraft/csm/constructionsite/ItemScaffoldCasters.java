package com.micatechnologies.minecraft.csm.constructionsite;

/**
 * Casters: fit a placed scaffold bay with wheels in place of its screw jacks, so a tower
 * built on them reads as a rolling tower.
 *
 * @version 1.0
 * @see ItemScaffoldAddon
 * @since 2026.9
 */
public class ItemScaffoldCasters extends ItemScaffoldAddon {

  @Override
  public ScaffoldAddon getAddon() {
    return ScaffoldAddon.CASTERS;
  }
}

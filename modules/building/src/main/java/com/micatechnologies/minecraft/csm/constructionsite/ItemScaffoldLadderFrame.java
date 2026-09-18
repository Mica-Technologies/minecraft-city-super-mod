package com.micatechnologies.minecraft.csm.constructionsite;

/**
 * A ladder frame: fits a placed scaffold bay with rungs in place of its walk-through
 * frames, so a run shows its way up.
 *
 * @version 1.0
 * @see ItemScaffoldAddon
 * @since 2026.9
 */
public class ItemScaffoldLadderFrame extends ItemScaffoldAddon {

  @Override
  public ScaffoldAddon getAddon() {
    return ScaffoldAddon.LADDER_FRAME;
  }
}

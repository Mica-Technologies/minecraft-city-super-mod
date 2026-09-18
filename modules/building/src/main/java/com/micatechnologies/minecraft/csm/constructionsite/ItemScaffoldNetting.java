package com.micatechnologies.minecraft.csm.constructionsite;

/**
 * Debris netting: fits a placed scaffold bay with green mesh on every open face.
 *
 * @version 1.0
 * @see ItemScaffoldAddon
 * @since 2026.9
 */
public class ItemScaffoldNetting extends ItemScaffoldAddon {

  @Override
  public ScaffoldAddon getAddon() {
    return ScaffoldAddon.NETTING;
  }
}

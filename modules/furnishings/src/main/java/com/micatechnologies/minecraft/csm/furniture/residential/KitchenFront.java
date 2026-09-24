package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nullable;

/**
 * What a kitchen cabinet opens with, which decides the sound it makes.
 *
 * @since 2026.9
 */
public enum KitchenFront {
  /** Hinged doors. */
  DOORS(FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE),
  /** Drawers on runners. */
  DRAWERS(FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE),
  /** Nothing: an open shelf, which holds nothing either. */
  OPEN(null, null);

  private final ICsmSound open;
  private final ICsmSound close;

  KitchenFront(@Nullable ICsmSound open, @Nullable ICsmSound close) {
    this.open = open;
    this.close = close;
  }

  /**
   * The sound of opening it.
   *
   * @return the sound, or null
   */
  @Nullable
  public ICsmSound getOpenSound() {
    return open;
  }

  /**
   * The sound of closing it.
   *
   * @return the sound, or null
   */
  @Nullable
  public ICsmSound getCloseSound() {
    return close;
  }
}

package com.micatechnologies.minecraft.csm.hvac;

/**
 * An exhaust grille (#314): a vent the air leaves a room through rather than enters it. It is
 * linked, saved and anchored exactly as a {@link TileEntityHvacVentRelay} is, and sits in its
 * thermostat's vent list (so linking, unlinking, sleeping and loading need nothing new);
 * {@link HvacSystemControl} tells it apart by type, delivers no heat through it, and hands it a
 * share of the system's exhaust fans instead.
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class TileEntityHvacExhaustGrille extends TileEntityHvacVentRelay {

  /** Air drawn out in the last step, cubic feet a minute; for {@code /csmhvac}. */
  transient float exhaustCfm;
}

package com.micatechnologies.minecraft.csm.hvac;

/**
 * A combination unit: one that both heats and cools, as a PTAC or a packaged rooftop unit (a
 * cooling section and a gas heat section in one cabinet) does. Never both at once: linked, it runs
 * in whichever mode its system is in ({@link HvacSystemControl#runsIn}); unlinked and powered, it
 * heats its room below 70°F and cools it above 74°F, idle between
 * ({@link HvacSystemControl#standaloneDelivery}).
 *
 * <p>It is a {@link TileEntityHvacHeater} so the linker, the thermostats and the controllers find
 * it as a unit, and like every unit it runs on redstone or Forge Energy.</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class TileEntityHvacComboUnit extends TileEntityHvacHeater {

  @Override
  public boolean isCoolingUnit() {
    return false;
  }

  @Override
  public boolean canHeat() {
    return true;
  }

  @Override
  public boolean canCool() {
    return true;
  }
}

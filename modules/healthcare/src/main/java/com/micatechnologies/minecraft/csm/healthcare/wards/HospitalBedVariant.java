package com.micatechnologies.minecraft.csm.healthcare.wards;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * The hospital beds: four blanket colours on the standard bed, the ICU bed with its air
 * mattress and pump, the wider bariatric bed, and the labour and delivery bed with its split foot
 * section and leg supports. Each is its own head block
 * ({@code hospital_bed_<name>}); the one foot block they share reads which bed it belongs to
 * from the head beside it. The order is the foot's {@code variant} property's, so append only.
 *
 * @since 2026.10
 */
public enum HospitalBedVariant implements IStringSerializable {
  WHITE,
  BLUE,
  GREEN,
  MAUVE,
  ICU,
  BARIATRIC,
  /** The labour and delivery bed: a split foot section and leg supports. */
  LABOR,
  /**
   * The patient transport stretcher (pre-op, PACU, urgent care): a narrow litter on a wheeled
   * base, one long rail a side, push handles and an IV pole in its socket at the head.
   */
  STRETCHER,
  /** The same stretcher with the emergency department's black trauma pad and no sheet. */
  STRETCHER_TRAUMA;

  @Override
  @Nonnull
  public String getName() {
    return name().toLowerCase();
  }

  /**
   * Whether this is a stretcher rather than a bed.
   *
   * @return true for the stretchers
   */
  public boolean isStretcher() {
    return this == STRETCHER || this == STRETCHER_TRAUMA;
  }

  /**
   * The registry name of this bed's head block.
   *
   * @return {@code hospital_bed_<name>}, or {@code hospital_<name>} for a stretcher
   */
  public String registryName() {
    return (isStretcher() ? "hospital_" : "hospital_bed_") + getName();
  }
}

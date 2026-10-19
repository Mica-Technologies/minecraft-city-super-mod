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
  LABOR;

  @Override
  @Nonnull
  public String getName() {
    return name().toLowerCase();
  }

  /**
   * The registry name of this bed's head block.
   *
   * @return {@code hospital_bed_<name>}
   */
  public String registryName() {
    return "hospital_bed_" + getName();
  }
}

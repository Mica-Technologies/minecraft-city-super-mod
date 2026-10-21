package com.micatechnologies.minecraft.csm.technology.network;

import java.util.Locale;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.IStringSerializable;

/**
 * The status lights a click changes on Micafi wireless gear: an access point's ring and a radio's
 * link light. Each is two bits of metadata beside the facing, the blockstate swapping the lit
 * texture, so nothing ticks and there is no tile entity.
 *
 * <p>Ordinals are saved in the metadata: only ever append, and never past four values.</p>
 *
 * @since 2026.10
 */
public final class MicafiLight {

  private MicafiLight() {
  }

  /** An access point's ring: blue in service, white, dark, and flashing while it is adopted. */
  public enum Ring implements IStringSerializable {
    BLUE, WHITE, OFF, ADOPTING;

    @Nonnull
    @Override
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** A radio's link light: green when linked, blue, white, or dark. */
  public enum Link implements IStringSerializable {
    GREEN, BLUE, WHITE, OFF;

    @Nonnull
    @Override
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** An access point's ring. */
  public static final PropertyEnum<Ring> RING = PropertyEnum.create("ring", Ring.class);

  /** A radio's link light. */
  public static final PropertyEnum<Link> LINK = PropertyEnum.create("link", Link.class);

  /**
   * @param value a light's value
   * @param <E>   the light
   *
   * @return the value a click changes it to
   */
  public static <E extends Enum<E>> E next(E value) {
    E[] values = value.getDeclaringClass().getEnumConstants();
    return values[(value.ordinal() + 1) % values.length];
  }

  /**
   * @param property a light property
   * @param bits     its two bits of metadata
   * @param <E>      the light
   *
   * @return the value those bits hold
   */
  public static <E extends Enum<E> & IStringSerializable> E fromBits(PropertyEnum<E> property,
      int bits) {
    E[] values = property.getValueClass().getEnumConstants();
    return values[(bits & 3) % values.length];
  }
}

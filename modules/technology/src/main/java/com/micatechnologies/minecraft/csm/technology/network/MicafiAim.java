package com.micatechnologies.minecraft.csm.technology.network;

import java.util.Locale;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.IStringSerializable;

/**
 * Where a wall-arm device's head points on its swivel: straight out from the wall, or turned 45
 * degrees to the left or right (as the installer, standing at the wall and looking out, sees it),
 * each level or tipped down 20 degrees. The arm and its wall plate never move; the blockstate turns
 * only the head, about the ball joint at the end of the arm.
 *
 * <p>The metadata of {@link BlockMicafiDevice} is full (two bits of facing, two of light), so the
 * aim lives on a {@link TileEntityMicafiAim} that does nothing but hold it, and reaches the model
 * as an actual-state property. Sneak-clicking with an empty hand cycles it in declaration order.
 *
 * <p>Ordinals are saved in the tile entity: only ever append.</p>
 *
 * @since 2026.10
 */
public enum MicafiAim implements IStringSerializable {
  CENTRE(0, false),
  LEFT(45, false),
  RIGHT(-45, false),
  CENTRE_DOWN(0, true),
  LEFT_DOWN(45, true),
  RIGHT_DOWN(-45, true);

  /** The actual-state property the blockstate reads. */
  public static final PropertyEnum<MicafiAim> PROPERTY = PropertyEnum.create("aim",
      MicafiAim.class);

  private final int yaw;
  private final boolean down;

  MicafiAim(int yaw, boolean down) {
    this.yaw = yaw;
    this.down = down;
  }

  /**
   * @return the head's turn about the vertical, in degrees anticlockwise seen from above
   */
  public int getYaw() {
    return yaw;
  }

  /**
   * @return whether the head is tipped down
   */
  public boolean isDown() {
    return down;
  }

  /**
   * @return the aim a sneak-click changes this one to
   */
  public MicafiAim next() {
    MicafiAim[] values = values();
    return values[(ordinal() + 1) % values.length];
  }

  /**
   * @param ordinal a saved ordinal
   *
   * @return its aim, or {@link #CENTRE} for one this version does not know
   */
  public static MicafiAim fromOrdinal(int ordinal) {
    MicafiAim[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CENTRE;
  }

  @Nonnull
  @Override
  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }
}

package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import javax.annotation.Nullable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client-side appearance clipboard shared by the signal head screen
 * ({@link SignalHeadConfigGui}) and the signal cluster hanger's mount screen. It holds one thing
 * at a time, whichever was copied last: a head's whole appearance, or a hanger's colour. Static,
 * so a copy survives closing the screen and lasts until the next copy or the game exits.
 *
 * <p>What a paste does depends on what is held and where it goes:</p>
 * <ul>
 *   <li>head onto head: the whole appearance, as it always has;</li>
 *   <li>hanger onto head: the head's mount colour only;</li>
 *   <li>hanger or head onto hanger: the hanger's colour, from the hanger's colour or the head's
 *       mount colour ({@link #colorForHanger()}).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class SignalAppearanceClipboard {

  /** Immutable snapshot of the copy/paste-able appearance properties of a signal head. */
  public static final class HeadAppearance {

    final TrafficSignalBodyColor bodyColor;
    final TrafficSignalBodyColor doorColor;
    final TrafficSignalBodyColor visorColor;
    final TrafficSignalVisorType visorType;
    final TrafficSignalBodyStyle bodyStyle;
    final TrafficSignalBulbStyle bulbStyle;
    final boolean agingEnabled;
    final TrafficSignalBodyColor mountColor;
    final boolean horizontalFlip;

    HeadAppearance(TrafficSignalBodyColor bodyColor, TrafficSignalBodyColor doorColor,
        TrafficSignalBodyColor visorColor, TrafficSignalVisorType visorType,
        TrafficSignalBodyStyle bodyStyle, TrafficSignalBulbStyle bulbStyle, boolean agingEnabled,
        TrafficSignalBodyColor mountColor, boolean horizontalFlip) {
      this.bodyColor = bodyColor;
      this.doorColor = doorColor;
      this.visorColor = visorColor;
      this.visorType = visorType;
      this.bodyStyle = bodyStyle;
      this.bulbStyle = bulbStyle;
      this.agingEnabled = agingEnabled;
      this.mountColor = mountColor;
      this.horizontalFlip = horizontalFlip;
    }
  }

  @Nullable
  private static HeadAppearance head = null;

  @Nullable
  private static TrafficSignalBodyColor hangerColor = null;

  private SignalAppearanceClipboard() {
  }

  /** Holds a head's appearance, in place of whatever was copied before. */
  static void copyHead(HeadAppearance appearance) {
    head = appearance;
    hangerColor = null;
  }

  /**
   * Holds a signal cluster hanger's colour, in place of whatever was copied before.
   *
   * @param color the hanger's colour
   */
  public static void copyHangerColor(TrafficSignalBodyColor color) {
    hangerColor = color;
    head = null;
  }

  /** The copied head appearance, or {@code null} if a hanger colour (or nothing) is held. */
  @Nullable
  static HeadAppearance head() {
    return head;
  }

  /** The copied hanger colour, or {@code null} if a head appearance (or nothing) is held. */
  @Nullable
  public static TrafficSignalBodyColor hangerColor() {
    return hangerColor;
  }

  /** Whether anything has been copied. */
  public static boolean isEmpty() {
    return head == null && hangerColor == null;
  }

  /**
   * The colour a paste onto a hanger gives it: a copied hanger's colour, or a copied head's mount
   * colour.
   *
   * @return the colour, or {@code null} if nothing has been copied
   */
  @Nullable
  public static TrafficSignalBodyColor colorForHanger() {
    if (hangerColor != null) {
      return hangerColor;
    }
    return head == null ? null : head.mountColor;
  }
}

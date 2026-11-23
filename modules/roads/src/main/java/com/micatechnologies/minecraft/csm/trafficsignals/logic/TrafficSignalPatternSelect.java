package com.micatechnologies.minecraft.csm.trafficsignals.logic;

/**
 * Which coordination pattern an ADVANCED controller should be running, and who says so.
 *
 * <p>Three things can choose a pattern, in this order of authority:</p>
 * <ol>
 *   <li>a <b>pattern select switch</b> linked to the controller, while it is on (an event mode
 *       switch in a cabinet, a lever, anything redstone can do);</li>
 *   <li>a <b>manual pattern</b> held from the panel, {@code /csm signal pattern} or NBT;</li>
 *   <li>the <b>clock</b>: the time-of-day table, or pattern 1 when that table is off.</li>
 * </ol>
 *
 * <p>The switch outranks the manual hold because it is the one a person standing at the junction
 * can see and reach: a controller that ignored its own cabinet switch because of a setting on a
 * screen would look broken.</p>
 *
 * <p>This only ever decides the <em>wanted</em> pattern. Taking it up is the ring engine's
 * business, at a cycle boundary ({@link RingBarrierState#nextCoordinationSlot}), exactly as for a
 * pattern the clock selects. Preemption is untouched by any of it: a preempt overrides whatever
 * pattern is running.</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public final class TrafficSignalPatternSelect {

  /** "No pattern": the setting is off and the next authority down decides. */
  public static final int NONE = -1;

  /** The pattern a linked switch selects until the plan says otherwise: the second one. */
  public static final int DEFAULT_SWITCH_SLOT = 1;

  /** Who chose the wanted pattern. */
  public enum Source {
    CLOCK("clock"),
    MANUAL("manual"),
    SWITCH("switch");

    private final String label;

    Source(String label) {
      this.label = label;
    }

    /** A lower-case word for status lines. */
    public String getLabel() {
      return label;
    }

    /** The source with this ordinal, or {@link #CLOCK} for anything else. */
    public static Source fromOrdinal(int ordinal) {
      Source[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CLOCK;
    }
  }

  private TrafficSignalPatternSelect() {
  }

  /**
   * A stored pattern setting made safe: a real slot, or {@link #NONE}.
   *
   * @param slot a slot index from anywhere (NBT, a packet, a command)
   *
   * @return {@code slot} if it names a pattern, otherwise {@link #NONE}
   */
  public static int normalize(int slot) {
    return slot >= 0 && slot < TrafficTimeOfDaySchedule.SLOT_COUNT ? slot : NONE;
  }

  /**
   * Who decides the pattern.
   *
   * @param manualSlot the manual pattern, or {@link #NONE}
   * @param switchSlot the pattern a linked switch selects, or {@link #NONE} if switches are ignored
   * @param switchOn   whether a linked switch is on
   *
   * @return the authority whose choice {@link #slot} returns
   */
  public static Source source(int manualSlot, int switchSlot, boolean switchOn) {
    if (switchOn && normalize(switchSlot) != NONE) {
      return Source.SWITCH;
    }
    return normalize(manualSlot) != NONE ? Source.MANUAL : Source.CLOCK;
  }

  /**
   * The wanted pattern.
   *
   * @param clockSlot  the pattern the clock selects
   * @param manualSlot the manual pattern, or {@link #NONE}
   * @param switchSlot the pattern a linked switch selects, or {@link #NONE} if switches are ignored
   * @param switchOn   whether a linked switch is on
   *
   * @return the slot index the controller should be running
   */
  public static int slot(int clockSlot, int manualSlot, int switchSlot, boolean switchOn) {
    switch (source(manualSlot, switchSlot, switchOn)) {
      case SWITCH:
        return switchSlot;
      case MANUAL:
        return manualSlot;
      default:
        return Math.max(0, normalize(clockSlot));
    }
  }
}

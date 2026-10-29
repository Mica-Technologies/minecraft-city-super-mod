package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * A per-frame time budget for compiling display lists again, shared by every renderer that keeps
 * a board's whole page of text in one flat list.
 *
 * <p><b>Why.</b> Such a list is replayed for next to nothing but costs a tessellation and a driver
 * copy to compile, on the order of a hundred microseconds for a large board. And the boards that
 * hold them all change together: every flight board and ED tracking board in the world moves to
 * the next game minute on the same tick, and every paged board turns its page on the same tick.
 * Compiling a few dozen of them in one frame is a hitch of several milliseconds.</p>
 *
 * <p><b>The rule.</b> A board whose list exists but is out of date asks {@link #mayRecompile()}
 * first; when the frame has spent its {@link #FRAME_NANOS}, the board draws its old list this
 * frame and asks again the next, so a terminal's boards catch up over a few frames rather than
 * stalling one. The first compile in a frame is always allowed, so every board gets its turn. A
 * board with no list at all compiles at once whatever the budget says: drawing nothing is never
 * the answer. Each compile is timed with {@link #begin()} and {@link #end(long)}.</p>
 *
 * <p>Render thread only. {@link #describe()} is read from the server thread for
 * {@code /csm displaylists}; it reads diagnostic counters and nothing else.</p>
 *
 * @since 2026.10
 */
public final class CsmCompileBudget {

  /** Compile time allowed a frame before out-of-date lists wait for the next frame: 1 ms. */
  public static final long FRAME_NANOS = 1_000_000L;

  /** The clock, swappable so the rule can be tested without a game. */
  private static LongSupplier clock = System::nanoTime;

  /** Compile time spent this frame. */
  private static long spentThisFrame;

  /** Diagnostics since the last disconnect: compiles, their total and longest time, deferrals. */
  private static volatile long compiles;
  private static volatile long totalNanos;
  private static volatile long maxNanos;
  private static volatile long deferrals;
  private static volatile long peakFrameNanos;

  private CsmCompileBudget() {
  }

  /**
   * Whether a list that exists but is out of date may be compiled again this frame. A {@code false}
   * is counted as a deferral; the caller draws the list it has and asks again next frame.
   *
   * @return whether to recompile now
   */
  public static boolean mayRecompile() {
    if (spentThisFrame < FRAME_NANOS) {
      return true;
    }
    deferrals++;
    return false;
  }

  /**
   * Marks the start of a compile.
   *
   * @return the start time, for {@link #end(long)}
   */
  public static long begin() {
    return clock.getAsLong();
  }

  /**
   * Charges a compile to this frame's budget.
   *
   * @param started what {@link #begin()} returned
   */
  public static void end(long started) {
    long nanos = Math.max(0L, clock.getAsLong() - started);
    spentThisFrame += nanos;
    compiles++;
    totalNanos += nanos;
    if (nanos > maxNanos) {
      maxNanos = nanos;
    }
    if (spentThisFrame > peakFrameNanos) {
      peakFrameNanos = spentThisFrame;
    }
  }

  /** Starts a new frame's budget. Called from {@link CsmRenderUtils.FrameClock}. */
  public static void onFrameStart() {
    spentThisFrame = 0L;
  }

  /** Clears the diagnostics, on disconnect. */
  public static void reset() {
    spentThisFrame = 0L;
    compiles = 0L;
    totalNanos = 0L;
    maxNanos = 0L;
    deferrals = 0L;
    peakFrameNanos = 0L;
  }

  /**
   * One line for {@code /csm displaylists}: how many board lists were compiled, how long one took
   * on average and at most, the most compile time one frame spent, and how many recompiles waited
   * for a later frame.
   *
   * @return the lines
   */
  public static List<String> describe() {
    List<String> out = new ArrayList<>();
    long n = compiles;
    out.add(String.format(Locale.ROOT,
        "%-24s %d compiles, mean %.1f us, max %.1f us, peak frame %.2f ms, %d deferred",
        "board list compiles", n, n == 0 ? 0.0 : totalNanos / 1000.0 / n,
        maxNanos / 1000.0, peakFrameNanos / 1_000_000.0, deferrals));
    return out;
  }

  /** Test hook: replaces the clock. */
  static void setClock(LongSupplier newClock) {
    clock = newClock;
  }

  /** Test hook: how much of this frame's budget is spent. */
  static long spentThisFrame() {
    return spentThisFrame;
  }
}

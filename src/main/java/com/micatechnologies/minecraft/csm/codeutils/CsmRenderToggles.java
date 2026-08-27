package com.micatechnologies.minecraft.csm.codeutils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime switches for individual render passes, used to measure where frame time goes and to A/B
 * a render change without restarting the game.
 *
 * <h2>Why this exists</h2>
 *
 * Comparing two builds means restarting the client, and a restart moves the frame rate by five to
 * seven percent on its own -- more than most changes worth making. A cross-restart A/B therefore
 * cannot resolve anything small, and reading one as if it could has already produced a "result"
 * that was mechanically impossible. Flipping a switch inside one session avoids that entirely: the
 * driver is warm, the JIT is warm, nothing else moved.
 *
 * <p>Two uses. Setting a {@code skip} flag turns a pass off, which attributes cost the same way
 * deleting blocks does but one level down, inside a single renderer -- the frame time that
 * disappears is what the pass was costing. Setting a behaviour flag switches between an old and a
 * new implementation of the same pass so both can be measured back to back.</p>
 *
 * <p>Skipping a pass makes the game render incorrectly on purpose. That is fine for a measurement
 * and useless for anything else, so nothing here persists: every flag resets to its shipping value
 * when the game restarts, and {@link #reset()} puts them all back mid-session.</p>
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public final class CsmRenderToggles {

  /** Skip the traffic signal head's bulb quad pass. Measurement only -- bulbs vanish. */
  public static boolean skipSignalBulbs = false;

  /** Skip the traffic signal head's lit visor interior overlay. Measurement only. */
  public static boolean skipSignalVisorInteriors = false;

  /** Skip the traffic signal head's mount/bracket pass. Measurement only -- mounts vanish. */
  public static boolean skipSignalMount = false;

  /** Skip the traffic signal head's cached body/door/visor display list. Measurement only. */
  public static boolean skipSignalBody = false;

  /**
   * Draw the lit visor interiors per frame with an unquantised tint, the way they were drawn
   * before they were baked into the display list. Kept so the two can be measured against each
   * other inside one session; the baked path is what ships.
   */
  public static boolean visorInteriorsPerFrame = false;

  /**
   * Draw the bulb lens quads per frame instead of replaying their display list, the way they were
   * drawn before they were baked. Kept so the two can be measured and pixel-compared inside one
   * session; the baked path is what ships.
   */
  public static boolean bulbsPerFrame = false;

  /** Skip the crosswalk display face entirely. Measurement only -- the face goes blank. */
  public static boolean skipCrosswalkFace = false;

  /**
   * Draw the crosswalk display face per frame instead of replaying its display list, the way it was
   * drawn before it was baked. Kept so the two can be measured and pixel-compared inside one
   * session; the baked path is what ships.
   */
  public static boolean crosswalkFacePerFrame = false;

  private CsmRenderToggles() {
  }

  /**
   * Returns every toggle by name, in a stable order, for listing and lookup.
   *
   * @return the toggle names mapped to their current values
   */
  public static Map<String, Boolean> snapshot() {
    Map<String, Boolean> values = new LinkedHashMap<>();
    values.put("signalBulbs", skipSignalBulbs);
    values.put("signalVisorInteriors", skipSignalVisorInteriors);
    values.put("signalMount", skipSignalMount);
    values.put("signalBody", skipSignalBody);
    values.put("visorInteriorsPerFrame", visorInteriorsPerFrame);
    values.put("bulbsPerFrame", bulbsPerFrame);
    values.put("crosswalkFace", skipCrosswalkFace);
    values.put("crosswalkFacePerFrame", crosswalkFacePerFrame);
    return values;
  }

  /**
   * Sets a toggle by name.
   *
   * @param name    the toggle name, as listed by {@link #snapshot()}
   * @param skipped true to skip the pass
   *
   * @return true if the name matched a toggle
   */
  public static boolean set(String name, boolean skipped) {
    if ("signalBulbs".equalsIgnoreCase(name)) {
      skipSignalBulbs = skipped;
    } else if ("signalVisorInteriors".equalsIgnoreCase(name)) {
      skipSignalVisorInteriors = skipped;
    } else if ("signalMount".equalsIgnoreCase(name)) {
      skipSignalMount = skipped;
    } else if ("signalBody".equalsIgnoreCase(name)) {
      skipSignalBody = skipped;
    } else if ("visorInteriorsPerFrame".equalsIgnoreCase(name)) {
      visorInteriorsPerFrame = skipped;
    } else if ("bulbsPerFrame".equalsIgnoreCase(name)) {
      bulbsPerFrame = skipped;
    } else if ("crosswalkFace".equalsIgnoreCase(name)) {
      skipCrosswalkFace = skipped;
    } else if ("crosswalkFacePerFrame".equalsIgnoreCase(name)) {
      crosswalkFacePerFrame = skipped;
    } else {
      return false;
    }
    return true;
  }

  /** Restores every toggle to its shipping value. */
  public static void reset() {
    skipSignalBulbs = false;
    skipSignalVisorInteriors = false;
    skipSignalMount = false;
    skipSignalBody = false;
    visorInteriorsPerFrame = false;
    bulbsPerFrame = false;
    skipCrosswalkFace = false;
    crosswalkFacePerFrame = false;
  }

  /**
   * Whether any pass is currently being skipped, so callers can warn that what is on screen is not
   * what the mod normally draws.
   *
   * @return true if at least one toggle is set
   */
  public static boolean anyActive() {
    for (Boolean value : snapshot().values()) {
      if (value) {
        return true;
      }
    }
    return false;
  }
}

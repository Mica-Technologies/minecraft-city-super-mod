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

  /** Skip the crosswalk's arm and stub boxes. Measurement only -- the bracket vanishes. */
  public static boolean skipCrosswalkArms = false;

  /** Skip the crosswalk's 7-segment countdown overlay. Measurement only. */
  public static boolean skipCrosswalkCountdown = false;

  /** Skip the crosswalk display face entirely. Measurement only -- the face goes blank. */
  public static boolean skipCrosswalkFace = false;

  /**
   * Draw the crosswalk display face per frame instead of replaying its display list, the way it was
   * drawn before it was baked. Kept so the two can be measured and pixel-compared inside one
   * session; the baked path is what ships.
   */
  public static boolean crosswalkFacePerFrame = false;

  /**
   * Draw the crosswalk countdown overlay per frame instead of replaying its display list, the way
   * it was drawn before it was baked. Kept so the two can be measured and pixel-compared inside one
   * session; the baked path is what ships.
   */
  public static boolean crosswalkCountdownPerFrame = false;

  /** Skip the dynamic guide sign renderer entirely. Measurement only. */
  public static boolean skipGuideSign = false;

  /** Skip the dynamic street sign renderer entirely. Measurement only. */
  public static boolean skipStreetSign = false;

  /**
   * Force the guide sign's far-distance level of detail at any range, dropping the legend, shields,
   * arrows and text and leaving the body, posts and lighting. Measurement only -- it isolates what
   * the legend detail costs, using the LOD path the renderer already has.
   */
  public static boolean guideSignForceFarLod = false;

  /** As {@link #guideSignForceFarLod}, for the street sign. Measurement only. */
  public static boolean streetSignForceFarLod = false;

  /** Skip the guide sign's post structure. Measurement only. */
  public static boolean skipGuideSignPost = false;

  /** Skip the guide sign's external lighting fixtures. Measurement only. */
  public static boolean skipGuideSignLighting = false;

  /**
   * Draw the street sign's structural geometry per frame instead of replaying its display lists.
   * Kept so the two can be measured and pixel-compared inside one session; the baked path ships.
   */
  public static boolean streetSignStructurePerFrame = false;

  /**
   * Draw the guide sign's background per frame instead of replaying its display list. Kept so the
   * two can be measured and pixel-compared inside one session; the baked path ships.
   */
  public static boolean guideSignBackgroundPerFrame = false;

  /** Skip the span wire messenger cable entirely. Measurement only. */
  public static boolean skipSpanWireCable = false;

  /**
   * Draw the span wire cable per frame instead of replaying its display list. Kept so the two can
   * be measured and pixel-compared inside one session; the baked path ships.
   */
  public static boolean spanWireCablePerFrame = false;

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
    values.put("crosswalkArms", skipCrosswalkArms);
    values.put("crosswalkCountdown", skipCrosswalkCountdown);
    values.put("crosswalkFace", skipCrosswalkFace);
    values.put("guideSign", skipGuideSign);
    values.put("streetSign", skipStreetSign);
    values.put("guideSignForceFarLod", guideSignForceFarLod);
    values.put("streetSignForceFarLod", streetSignForceFarLod);
    values.put("guideSignPost", skipGuideSignPost);
    values.put("guideSignLighting", skipGuideSignLighting);
    values.put("streetSignStructurePerFrame", streetSignStructurePerFrame);
    values.put("guideSignBackgroundPerFrame", guideSignBackgroundPerFrame);
    values.put("crosswalkFacePerFrame", crosswalkFacePerFrame);
    values.put("crosswalkCountdownPerFrame", crosswalkCountdownPerFrame);
    values.put("spanWireCable", skipSpanWireCable);
    values.put("spanWireCablePerFrame", spanWireCablePerFrame);
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
    } else if ("crosswalkArms".equalsIgnoreCase(name)) {
      skipCrosswalkArms = skipped;
    } else if ("crosswalkCountdown".equalsIgnoreCase(name)) {
      skipCrosswalkCountdown = skipped;
    } else if ("crosswalkFace".equalsIgnoreCase(name)) {
      skipCrosswalkFace = skipped;
    } else if ("guideSign".equalsIgnoreCase(name)) {
      skipGuideSign = skipped;
    } else if ("streetSign".equalsIgnoreCase(name)) {
      skipStreetSign = skipped;
    } else if ("guideSignForceFarLod".equalsIgnoreCase(name)) {
      guideSignForceFarLod = skipped;
    } else if ("streetSignForceFarLod".equalsIgnoreCase(name)) {
      streetSignForceFarLod = skipped;
    } else if ("guideSignPost".equalsIgnoreCase(name)) {
      skipGuideSignPost = skipped;
    } else if ("guideSignLighting".equalsIgnoreCase(name)) {
      skipGuideSignLighting = skipped;
    } else if ("streetSignStructurePerFrame".equalsIgnoreCase(name)) {
      streetSignStructurePerFrame = skipped;
    } else if ("guideSignBackgroundPerFrame".equalsIgnoreCase(name)) {
      guideSignBackgroundPerFrame = skipped;
    } else if ("crosswalkFacePerFrame".equalsIgnoreCase(name)) {
      crosswalkFacePerFrame = skipped;
    } else if ("crosswalkCountdownPerFrame".equalsIgnoreCase(name)) {
      crosswalkCountdownPerFrame = skipped;
    } else if ("spanWireCable".equalsIgnoreCase(name)) {
      skipSpanWireCable = skipped;
    } else if ("spanWireCablePerFrame".equalsIgnoreCase(name)) {
      spanWireCablePerFrame = skipped;
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
    skipCrosswalkArms = false;
    skipCrosswalkCountdown = false;
    skipCrosswalkFace = false;
    skipGuideSign = false;
    skipStreetSign = false;
    guideSignForceFarLod = false;
    streetSignForceFarLod = false;
    skipGuideSignPost = false;
    skipGuideSignLighting = false;
    streetSignStructurePerFrame = false;
    guideSignBackgroundPerFrame = false;
    crosswalkFacePerFrame = false;
    crosswalkCountdownPerFrame = false;
    skipSpanWireCable = false;
    spanWireCablePerFrame = false;
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

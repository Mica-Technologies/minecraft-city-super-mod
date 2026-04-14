package com.micatechnologies.minecraft.csm.codeutils;

import java.time.Duration;

/**
 * Utility class for converting real-world time durations into Minecraft game ticks. Assumes the
 * standard rate of 20 ticks per second.
 *
 * @author Mica Technologies
 */
public class TickTimeConverter {

  private static final int TICKS_PER_SECOND = 20;

  public static int getTicksFromInterval(Duration duration) {
    return getTicksFromSeconds((int) duration.getSeconds());
  }

  public static int getTicksFromSeconds(int seconds) {
    return seconds * TICKS_PER_SECOND;
  }
}

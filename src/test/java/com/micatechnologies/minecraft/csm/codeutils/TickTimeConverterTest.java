package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TickTimeConverterTest {

  @Test
  void getTicksFromSecondsZero() {
    assertEquals(0, TickTimeConverter.getTicksFromSeconds(0));
  }

  @Test
  void getTicksFromSecondsOne() {
    assertEquals(20, TickTimeConverter.getTicksFromSeconds(1));
  }

  @Test
  void getTicksFromSecondsFive() {
    assertEquals(100, TickTimeConverter.getTicksFromSeconds(5));
  }

  @Test
  void getTicksFromSecondsLargeValue() {
    assertEquals(1200, TickTimeConverter.getTicksFromSeconds(60));
  }

  @Test
  void getTicksFromIntervalWithDuration() {
    assertEquals(0, TickTimeConverter.getTicksFromInterval(Duration.ZERO));
    assertEquals(20, TickTimeConverter.getTicksFromInterval(Duration.ofSeconds(1)));
    assertEquals(100, TickTimeConverter.getTicksFromInterval(Duration.ofSeconds(5)));
    assertEquals(1200, TickTimeConverter.getTicksFromInterval(Duration.ofMinutes(1)));
  }

  @Test
  void getTicksFromIntervalSubSecondTruncatesToZero() {
    // Duration.ofMillis(500) has 0 seconds when cast to int
    assertEquals(0, TickTimeConverter.getTicksFromInterval(Duration.ofMillis(500)));
  }
}

package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The boards' recompile budget: a frame's compiles stop once it has spent its budget, the first
 * compile of a frame is always allowed (so a board's turn always comes), and a new frame starts
 * the budget again. Driven by a fake clock, each compile costing what the test says.
 */
class CsmCompileBudgetTest {

  private final AtomicLong now = new AtomicLong();

  @BeforeEach
  void setUp() {
    CsmCompileBudget.setClock(now::get);
    CsmCompileBudget.reset();
  }

  @AfterEach
  void tearDown() {
    CsmCompileBudget.setClock(System::nanoTime);
    CsmCompileBudget.reset();
  }

  /** One compile of the given length, if the budget allows it. */
  private boolean compile(long nanos) {
    if (!CsmCompileBudget.mayRecompile()) {
      return false;
    }
    long started = CsmCompileBudget.begin();
    now.addAndGet(nanos);
    CsmCompileBudget.end(started);
    return true;
  }

  @Test
  void spreadsBoardsOverFrames() {
    // forty boards changing on the same game minute, 150 us each: 6 ms if compiled at once
    int boards = 40;
    long each = 150_000L;
    int compiled = 0;
    int frames = 0;
    boolean[] done = new boolean[boards];
    while (compiled < boards) {
      CsmCompileBudget.onFrameStart();
      frames++;
      for (int i = 0; i < boards; i++) {
        if (!done[i] && compile(each)) {
          done[i] = true;
          compiled++;
        }
      }
      // no frame spends more than the budget plus the one compile that crossed it
      assertTrue(CsmCompileBudget.spentThisFrame() < CsmCompileBudget.FRAME_NANOS + each);
    }
    // 1 ms a frame at 150 us a board is 7 boards a frame
    assertEquals((boards + 6) / 7, frames);
  }

  @Test
  void firstCompileOfAFrameIsAlwaysAllowed() {
    CsmCompileBudget.onFrameStart();
    // one board slower than the whole budget still compiles, and the next waits
    assertTrue(compile(5 * CsmCompileBudget.FRAME_NANOS));
    assertFalse(compile(1));
    CsmCompileBudget.onFrameStart();
    assertTrue(compile(1));
  }

  @Test
  void countsDeferralsAndPeak() {
    CsmCompileBudget.onFrameStart();
    compile(CsmCompileBudget.FRAME_NANOS);
    compile(1);
    compile(1);
    String line = CsmCompileBudget.describe().get(0);
    assertTrue(line.contains("1 compiles"), line);
    assertTrue(line.contains("2 deferred"), line);
    assertTrue(line.contains("peak frame 1.00 ms"), line);
  }
}

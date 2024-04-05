package com.micatechnologies.minecraft.csm.codeutils;

import com.mojang.text2speech.Narrator;
import java.util.concurrent.atomic.AtomicBoolean;

public class CsmNarrator {

  private static Narrator NARRATOR = null;
  private static final AtomicBoolean IS_NARRATOR_PLAYING = new AtomicBoolean(false);

  public static synchronized void init() {
    NARRATOR = Narrator.getNarrator();
  }

  public static synchronized void say(String message) {
    if (NARRATOR == null) {
      init();
    }
    new Thread(() -> {
      if (IS_NARRATOR_PLAYING.get()) {
        return;
      }
      IS_NARRATOR_PLAYING.set(true);
      NARRATOR.say(message);
      IS_NARRATOR_PLAYING.set(false);
    }).start();
  }
}

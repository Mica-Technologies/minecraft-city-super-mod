package com.micatechnologies.minecraft.csm.codeutils;

import java.util.List;

/**
 * A speech engine that {@link CsmTts} can speak through. The engine is optional: it lives in the
 * Text to Speech module, which carries the shaded synthesizer and its voices, and it registers
 * itself with {@link CsmTts#setEngine(ICsmTtsEngine)} during that module's pre-initialization.
 *
 * <p>Core never depends on an engine being present. With no engine registered — or with one that
 * has not finished loading, or that failed to load — {@link CsmTts} speaks through the system
 * narrator instead, which is exactly what it did when the synthesizer failed to initialize.</p>
 *
 * @author ah@micatechnologies.com
 * @version 1.0
 * @since 2026.9
 */
public interface ICsmTtsEngine {

  /**
   * Starts loading the engine if it has not been started already. Loading is asynchronous, so
   * this returns immediately and {@link #isReady()} stays false until it finishes. Calling this
   * more than once is harmless.
   *
   * @since 1.0
   */
  void startInit();

  /**
   * Gets whether the engine has finished loading and can synthesize speech.
   *
   * @return {@code true} if the engine is ready to speak
   *
   * @since 1.0
   */
  boolean isReady();

  /**
   * Gets the ids of the voices the engine offers.
   *
   * @return the available voice ids, or {@code null} if the engine cannot answer yet — the
   *     caller then falls back to the known-voice list
   *
   * @since 1.0
   */
  List<String> getAvailableVoiceIds();

  /**
   * Speaks the given message in the given voice. Only called when {@link #isReady()} is true.
   *
   * @param message the text to speak
   * @param voice   the id of the voice to speak it in, or empty for the current voice
   *
   * @since 1.0
   */
  void say(String message, String voice);
}

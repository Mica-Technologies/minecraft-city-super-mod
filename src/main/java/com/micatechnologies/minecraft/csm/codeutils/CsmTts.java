package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The mod's speech facade. Everything that wants to say something calls this class; it speaks
 * through the {@link ICsmTtsEngine} the Text to Speech module registers, and through the system
 * narrator when there is no engine or the engine is not ready.
 *
 * <p>Keeping the fallback here rather than in the engine means one code path decides how a
 * message is spoken: an install without the Text to Speech module behaves exactly like an
 * install whose synthesizer failed to load, which is a case the mod already handled.</p>
 *
 * @author Mica Technologies
 * @version 2.0
 */
@SideOnly(Side.CLIENT)
public class CsmTts {

  private static final String DEFAULT_VOICE = "cmu-slt-hsmm";
  private static final String[][] KNOWN_VOICES = {
      {"cmu-slt-hsmm", "CMU SLT (Female, US)"},
      {"cmu-rms-hsmm", "CMU RMS (Male, US)"},
      {"dfki-spike-hsmm", "DFKI Spike (Male, GB)"},
      {"dfki-prudence-hsmm", "DFKI Prudence (Female, GB)"}
  };

  /**
   * The registered speech engine, or {@code null} when the Text to Speech module is not
   * installed. Volatile because the module registers it on the main thread and speech runs on
   * its own threads.
   */
  private static volatile ICsmTtsEngine engine;

  /**
   * Registers the speech engine to speak through. Called from the Text to Speech module's
   * pre-initialization; the last engine registered wins.
   *
   * @param ttsEngine the engine to speak through
   *
   * @since 2.0
   */
  public static void setEngine(ICsmTtsEngine ttsEngine) {
    engine = ttsEngine;
  }

  /**
   * How loud speech should be, from the player's sound settings: the Master slider times the
   * Voice/Speech slider, 0 to 1.
   *
   * <p>Speech is not played through Minecraft's sound system -- the synthesizer writes its own
   * audio line -- so no slider applied to it until an engine asked for this. Voice/Speech is the
   * category vanilla added for narration, and the one a player expects to turn this down.
   * Cheap enough to read per audio buffer, so a slider moved mid-sentence takes effect at once.</p>
   *
   * @return the playback gain, 0 to 1
   *
   * @since 2026.9
   */
  public static float getVolume() {
    GameSettings settings = Minecraft.getMinecraft().gameSettings;
    if (settings == null) {
      return 1.0F;
    }
    return settings.getSoundLevel(SoundCategory.MASTER)
        * settings.getSoundLevel(SoundCategory.VOICE);
  }

  /**
   * Starts loading the speech engine, if one is registered. Loading is asynchronous, so this
   * returns immediately.
   */
  public static void startInit() {
    ICsmTtsEngine ttsEngine = engine;
    if (ttsEngine != null) {
      ttsEngine.startInit();
    }
  }

  /**
   * Gets whether a speech engine is registered and has finished loading.
   *
   * @return {@code true} if speech will be synthesized rather than narrated
   */
  public static boolean isReady() {
    ICsmTtsEngine ttsEngine = engine;
    return ttsEngine != null && ttsEngine.isReady();
  }

  public static String getDefaultVoice() {
    return DEFAULT_VOICE;
  }

  public static String[][] getKnownVoices() {
    return KNOWN_VOICES;
  }

  /**
   * Gets the ids of the voices that can be selected. These are the engine's voices once it has
   * loaded, and the known-voice list before that (or with no engine at all), so the selector in
   * the block's GUI always has something to offer.
   *
   * @return the selectable voice ids
   */
  public static List<String> getAvailableVoiceIds() {
    ICsmTtsEngine ttsEngine = engine;
    if (ttsEngine != null) {
      List<String> ids = ttsEngine.getAvailableVoiceIds();
      if (ids != null) {
        return ids;
      }
    }
    List<String> fallback = new ArrayList<>();
    for (String[] v : KNOWN_VOICES) {
      fallback.add(v[0]);
    }
    return fallback;
  }

  /**
   * Gets the human-readable name of a voice, or the id itself when the voice is not one of the
   * known ones.
   *
   * @param voiceId the id of the voice
   *
   * @return the display name of the voice
   */
  public static String getDisplayName(String voiceId) {
    for (String[] entry : KNOWN_VOICES) {
      if (entry[0].equals(voiceId)) {
        return entry[1];
      }
    }
    return voiceId;
  }

  /**
   * Says the given message in the given voice. Speech is asynchronous either way; a message
   * spoken while the engine is still loading, has failed to load, or is absent altogether goes
   * to the system narrator instead.
   *
   * @param message the text to speak
   * @param voice   the id of the voice to speak it in
   */
  public static void say(String message, String voice) {
    // Silenced by the player's sound settings: say nothing. This is the only volume the system
    // narrator can honour -- Mojang's text2speech has no volume of its own -- so without it a
    // muted game would still talk. The engine applies the level itself, sample by sample.
    if (getVolume() <= 0.0F) {
      return;
    }
    ICsmTtsEngine ttsEngine = engine;
    if (ttsEngine == null) {
      CsmNarrator.say(message);
      return;
    }

    // Idempotent: the engine ignores this once loading has started. Kept so that the first
    // thing to speak also starts the engine, as it always has.
    ttsEngine.startInit();

    if (!ttsEngine.isReady()) {
      // Still loading or failed — fall back immediately, no blocking
      CsmNarrator.say(message);
      return;
    }

    ttsEngine.say(message, voice);
  }
}

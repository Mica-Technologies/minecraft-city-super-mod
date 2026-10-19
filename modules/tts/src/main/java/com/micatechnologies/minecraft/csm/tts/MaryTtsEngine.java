package com.micatechnologies.minecraft.csm.tts;

import com.micatechnologies.minecraft.csm.codeutils.CsmNarrator;
import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTtsEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import marytts.LocalMaryInterface;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The MaryTTS speech engine. This is the only class in the mod that touches MaryTTS; everything
 * else speaks through {@link CsmTts}, which falls back to the system narrator when this engine
 * is absent or has not loaded.
 *
 * <p>Loading MaryTTS takes seconds and scans the shaded jar for voices, so it happens on its own
 * thread, on first use rather than at launch, and the engine reports itself not ready until it
 * finishes. A message that arrives while it loads is held (the latest one) and spoken as soon
 * as it has loaded, so the first announcement is late rather than lost or in another voice.</p>
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class MaryTtsEngine implements ICsmTtsEngine {

  private static final Logger LOGGER = LogManager.getLogger("CSM-TTS");
  private static final int AUDIO_BUFFER_SIZE = 4096;

  private static volatile LocalMaryInterface mary;
  private static volatile boolean initStarted = false;
  private static volatile boolean initialized = false;
  private static volatile boolean initFailed = false;
  private static volatile String currentVoice = "";
  private static final AtomicBoolean IS_PLAYING = new AtomicBoolean(false);
  /** The message given while loading, as {message, voice}; guarded by the class. */
  private static String[] pending;
  /** What the playback thread is saying, for the trace; null when it is idle. */
  private static volatile String nowPlaying;

  /**
   * Logs one stage of one message, stamped with the wall clock in milliseconds so the lines line
   * up with RCMC's announcement trace and with each other across threads. DEBUG goes to
   * {@code logs/debug.log}; a message that is lost is also logged at WARN, in {@code latest.log}.
   */
  private static void trace(String stage, String detail, String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("[tts] t={} {}{} \"{}\"", System.currentTimeMillis(), stage,
          detail == null || detail.isEmpty() ? "" : " " + detail, message);
    }
  }

  @Override
  public void startInit() {
    if (initStarted) {
      return;
    }
    synchronized (MaryTtsEngine.class) {
      if (initStarted) {
        return;
      }
      initStarted = true;
    }
    // Capture the calling thread's classloader (Forge's LaunchClassLoader).
    // New threads default to the system classloader, which can't see the
    // MaryTTS service files inside the shadow JAR.
    ClassLoader forgeClassLoader = Thread.currentThread().getContextClassLoader();
    new Thread(() -> {
      Thread.currentThread().setContextClassLoader(forgeClassLoader);
      try {
        LOGGER.info("Initializing MaryTTS...");
        long start = System.currentTimeMillis();
        LocalMaryInterface m = new LocalMaryInterface();
        LOGGER.info("MaryTTS created, available voices: {}", m.getAvailableVoices());
        String defaultVoice = CsmTts.getDefaultVoice();
        if (m.getAvailableVoices().contains(defaultVoice)) {
          m.setVoice(defaultVoice);
          currentVoice = defaultVoice;
        } else if (!m.getAvailableVoices().isEmpty()) {
          String first = m.getAvailableVoices().iterator().next();
          m.setVoice(first);
          currentVoice = first;
        }
        String[] held;
        synchronized (MaryTtsEngine.class) {
          mary = m;
          initialized = true;
          held = pending;
          pending = null;
        }
        LOGGER.info("MaryTTS initialized in {}ms — voice: {}",
            System.currentTimeMillis() - start, currentVoice);
        if (held != null) {
          trace("UNHOLD", "", held[0]);
          say(held[0], held[1]);
        }
      } catch (Throwable e) {
        LOGGER.error("Failed to initialize MaryTTS — TTS will fall back to system narrator", e);
        String[] held;
        synchronized (MaryTtsEngine.class) {
          initFailed = true;
          held = pending;
          pending = null;
        }
        if (held != null) {
          trace("NARRATOR", "engine-failed", held[0]);
          CsmNarrator.say(held[0]);
        }
      }
    }, "CSM-TTS-Init").start();
  }

  @Override
  public boolean isReady() {
    return initialized;
  }

  @Override
  public boolean isLoading() {
    return initStarted && !initialized && !initFailed;
  }

  @Override
  public List<String> getAvailableVoiceIds() {
    if (initialized && mary != null) {
      List<String> ids = new ArrayList<>(mary.getAvailableVoices());
      Collections.sort(ids);
      return ids;
    }
    return null;
  }

  @Override
  public void say(String message, String voice) {
    trace("SAY", "voice=" + voice, message);
    synchronized (MaryTtsEngine.class) {
      if (!initialized) {
        if (initFailed) {
          trace("NARRATOR", "engine-failed", message);
          CsmNarrator.say(message);
        } else {
          // Still loading: hold it; the loading thread speaks it when done.
          if (pending != null) {
            LOGGER.warn("[tts] t={} REPLACED-WHILE-LOADING \"{}\" by \"{}\"",
                System.currentTimeMillis(), pending[0], message);
          }
          trace("HOLD", "loading", message);
          pending = new String[]{message, voice};
        }
        return;
      }
    }
    if (!IS_PLAYING.compareAndSet(false, true)) {
      LOGGER.warn("[tts] t={} DROPPED-BUSY \"{}\" while playing \"{}\"",
          System.currentTimeMillis(), message, nowPlaying);
      return;
    }
    nowPlaying = message;

    // Capture classloader from calling thread (main client thread)
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    new Thread(() -> {
      Thread.currentThread().setContextClassLoader(cl);
      try {
        AudioInputStream audio;
        long synthStart = System.currentTimeMillis();
        trace("SYNTH-START", "", message);
        synchronized (MaryTtsEngine.class) {
          if (voice != null && !voice.isEmpty() && !voice.equals(currentVoice)
              && mary.getAvailableVoices().contains(voice)) {
            mary.setVoice(voice);
            currentVoice = voice;
          }
          audio = mary.generateAudio(message);
        }
        long audioMs = audio.getFrameLength() <= 0 ? -1L
            : Math.round(audio.getFrameLength() * 1000.0D / audio.getFormat().getFrameRate());
        trace("SYNTH-END", "took=" + (System.currentTimeMillis() - synthStart) + "ms audio="
            + audioMs + "ms", message);

        long playStart = System.currentTimeMillis();
        trace("PLAY-START", "", message);
        playAudio(audio);
        audio.close();
        trace("PLAY-END", "took=" + (System.currentTimeMillis() - playStart) + "ms", message);
      } catch (Exception e) {
        LOGGER.error("TTS playback failed for message: {}", message, e);
      } finally {
        nowPlaying = null;
        IS_PLAYING.set(false);
      }
    }, "CSM-TTS-Playback").start();
  }

  private static void playAudio(AudioInputStream audioStream) throws Exception {
    AudioFormat format = audioStream.getFormat();
    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
    if (!AudioSystem.isLineSupported(info)) {
      LOGGER.error("Audio line not supported for format: {}", format);
      return;
    }
    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
    line.open(format);
    line.start();
    try {
      byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = audioStream.read(buffer)) != -1) {
        // The line is Java Sound's, not Minecraft's, so the game's volume sliders reach it only
        // through this. Read per buffer, so a slider moved mid-sentence is heard at once.
        PcmVolume.apply(buffer, bytesRead, format, CsmTts.getVolume());
        line.write(buffer, 0, bytesRead);
      }
      line.drain();
    } finally {
      line.close();
    }
  }
}

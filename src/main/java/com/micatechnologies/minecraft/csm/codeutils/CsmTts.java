package com.micatechnologies.minecraft.csm.codeutils;

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

@SideOnly(Side.CLIENT)
public class CsmTts {

  private static final Logger LOGGER = LogManager.getLogger("CSM-TTS");
  private static final String DEFAULT_VOICE = "cmu-slt-hsmm";
  private static final String[][] KNOWN_VOICES = {
      {"cmu-slt-hsmm", "CMU SLT (Female, US)"}
  };
  private static final int AUDIO_BUFFER_SIZE = 4096;

  private static volatile LocalMaryInterface mary;
  private static volatile boolean initStarted = false;
  private static volatile boolean initialized = false;
  private static volatile boolean initFailed = false;
  private static volatile String currentVoice = "";
  private static final AtomicBoolean IS_PLAYING = new AtomicBoolean(false);

  public static void startInit() {
    if (initStarted) {
      return;
    }
    synchronized (CsmTts.class) {
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
        if (m.getAvailableVoices().contains(DEFAULT_VOICE)) {
          m.setVoice(DEFAULT_VOICE);
          currentVoice = DEFAULT_VOICE;
        } else if (!m.getAvailableVoices().isEmpty()) {
          String first = m.getAvailableVoices().iterator().next();
          m.setVoice(first);
          currentVoice = first;
        }
        mary = m;
        initialized = true;
        LOGGER.info("MaryTTS initialized in {}ms — voice: {}",
            System.currentTimeMillis() - start, currentVoice);
      } catch (Throwable e) {
        LOGGER.error("Failed to initialize MaryTTS — TTS will fall back to system narrator", e);
        initFailed = true;
      }
    }, "CSM-TTS-Init").start();
  }

  public static boolean isReady() {
    return initialized;
  }

  public static String getDefaultVoice() {
    return DEFAULT_VOICE;
  }

  public static String[][] getKnownVoices() {
    return KNOWN_VOICES;
  }

  public static List<String> getAvailableVoiceIds() {
    if (initialized && mary != null) {
      List<String> ids = new ArrayList<>(mary.getAvailableVoices());
      Collections.sort(ids);
      return ids;
    }
    List<String> fallback = new ArrayList<>();
    for (String[] v : KNOWN_VOICES) {
      fallback.add(v[0]);
    }
    return fallback;
  }

  public static String getDisplayName(String voiceId) {
    for (String[] entry : KNOWN_VOICES) {
      if (entry[0].equals(voiceId)) {
        return entry[1];
      }
    }
    return voiceId;
  }

  public static void say(String message, String voice) {
    if (!initStarted) {
      startInit();
    }

    if (!initialized) {
      // MaryTTS still loading or failed — fall back immediately, no blocking
      CsmNarrator.say(message);
      return;
    }

    if (!IS_PLAYING.compareAndSet(false, true)) {
      return;
    }

    // Capture classloader from calling thread (main client thread)
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    new Thread(() -> {
      Thread.currentThread().setContextClassLoader(cl);
      try {
        AudioInputStream audio;
        synchronized (CsmTts.class) {
          if (voice != null && !voice.isEmpty() && !voice.equals(currentVoice)
              && mary.getAvailableVoices().contains(voice)) {
            mary.setVoice(voice);
            currentVoice = voice;
          }
          audio = mary.generateAudio(message);
        }

        playAudio(audio);
        audio.close();
      } catch (Exception e) {
        LOGGER.error("TTS playback failed for message: {}", message, e);
      } finally {
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
        line.write(buffer, 0, bytesRead);
      }
      line.drain();
    } finally {
      line.close();
    }
  }
}

package com.micatechnologies.minecraft.csm.tts;

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
 * thread and the engine reports itself not ready until it finishes.</p>
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

  @Override
  public boolean isReady() {
    return initialized;
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
    if (!IS_PLAYING.compareAndSet(false, true)) {
      return;
    }

    // Capture classloader from calling thread (main client thread)
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    new Thread(() -> {
      Thread.currentThread().setContextClassLoader(cl);
      try {
        AudioInputStream audio;
        synchronized (MaryTtsEngine.class) {
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

package com.micatechnologies.minecraft.csm.tts;

import com.micatechnologies.minecraft.csm.codeutils.CsmNarrator;
import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTtsEngine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
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
 * finishes. Messages that arrive while it loads wait in the same queue as everything else (see
 * {@link #say}) and are spoken as soon as it has loaded, so the first announcement is late rather
 * than lost or in another voice.</p>
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
  /**
   * Most messages waiting at once. Past this the oldest goes: whatever has been waiting longest
   * is the most out of date.
   */
  private static final int MAX_QUEUED = 4;

  /**
   * A message that waited longer than this is skipped rather than spoken. A station call heard
   * ten seconds late describes a train that has already left.
   */
  private static final long MAX_WAIT_MS = 10_000L;

  /** Messages waiting to be spoken, oldest first; guarded by the class. */
  private static final Deque<Queued> QUEUE = new ArrayDeque<>();
  /** Whether a playback thread is working through {@link #QUEUE}; guarded by the class. */
  private static boolean playing;
  /** What the playback thread is saying; null when it is idle. Guarded by the class. */
  private static String nowPlaying;

  /** One message waiting to be spoken. */
  private static final class Queued {

    final String message;
    final String voice;
    final long queuedAt;

    Queued(String message, String voice) {
      this.message = message;
      this.voice = voice;
      this.queuedAt = System.currentTimeMillis();
    }
  }

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
        boolean startPlaying;
        synchronized (MaryTtsEngine.class) {
          mary = m;
          initialized = true;
          startPlaying = !QUEUE.isEmpty() && !playing;
          playing |= startPlaying;
        }
        LOGGER.info("MaryTTS initialized in {}ms — voice: {}",
            System.currentTimeMillis() - start, currentVoice);
        if (startPlaying) {
          // What was said while loading is spoken now, in order.
          startPlayback();
        }
      } catch (Throwable e) {
        LOGGER.error("Failed to initialize MaryTTS — TTS will fall back to system narrator", e);
        Queued latest;
        synchronized (MaryTtsEngine.class) {
          initFailed = true;
          latest = QUEUE.peekLast();
          QUEUE.clear();
        }
        // The narrator says one thing at a time, so only the newest of what waited.
        if (latest != null) {
          trace("NARRATOR", "engine-failed", latest.message);
          CsmNarrator.say(latest.message);
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

  /**
   * Queues {@code message} to be spoken after anything already waiting.
   *
   * <p>Messages are spoken one after another, never dropped for arriving while another plays.
   * The engine used to do exactly that: two announcements a fraction of a second apart (a
   * train's "This is ..." and the platform's "now arriving ...") rang two chimes and spoke one
   * sentence, with nothing to say the other had been lost. What it still declines, and logs:
   * a message identical to one already waiting or playing (two speakers in earshot saying the
   * same thing), and, past {@link #MAX_QUEUED} or {@link #MAX_WAIT_MS}, the oldest ones.</p>
   */
  @Override
  public void say(String message, String voice) {
    trace("SAY", "voice=" + voice, message);
    boolean start;
    synchronized (MaryTtsEngine.class) {
      if (initFailed) {
        trace("NARRATOR", "engine-failed", message);
        CsmNarrator.say(message);
        return;
      }
      if (message.equals(nowPlaying) || QUEUE.stream().anyMatch(q -> q.message.equals(message))) {
        trace("SKIP-DUPLICATE", "", message);
        return;
      }
      if (QUEUE.size() >= MAX_QUEUED) {
        Queued dropped = QUEUE.pollFirst();
        LOGGER.warn("[tts] t={} DROPPED-QUEUE-FULL \"{}\"", System.currentTimeMillis(),
            dropped.message);
      }
      QUEUE.addLast(new Queued(message, voice));
      // Still loading: it waits with the rest, and the loading thread starts playback when done.
      trace(initialized ? "QUEUE" : "HOLD", "depth=" + QUEUE.size()
          + (nowPlaying == null ? "" : " behind=\"" + nowPlaying + "\""), message);
      start = initialized && !playing;
      playing |= start;
    }
    if (start) {
      startPlayback();
    }
  }

  /** Speaks queued messages in order on one thread until the queue is empty. */
  private static void startPlayback() {
    // The caller's classloader (the client thread's, or the loading thread's, which was given
    // Forge's): a new thread would default to the system one, which cannot see MaryTTS.
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    new Thread(() -> {
      Thread.currentThread().setContextClassLoader(cl);
      for (;;) {
        Queued next;
        synchronized (MaryTtsEngine.class) {
          next = QUEUE.pollFirst();
          nowPlaying = next == null ? null : next.message;
          if (next == null) {
            playing = false;
            return;
          }
        }
        long waited = System.currentTimeMillis() - next.queuedAt;
        if (waited > MAX_WAIT_MS) {
          LOGGER.warn("[tts] t={} DROPPED-STALE after {}ms \"{}\"", System.currentTimeMillis(),
              waited, next.message);
          continue;
        }
        speak(next.message, next.voice, waited);
      }
    }, "CSM-TTS-Playback").start();
  }

  /** Synthesises and plays one message on the calling (playback) thread. */
  private static void speak(String message, String voice, long waited) {
    try {
      AudioInputStream audio;
      long synthStart = System.currentTimeMillis();
      trace("SYNTH-START", "waited=" + waited + "ms", message);
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
    }
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

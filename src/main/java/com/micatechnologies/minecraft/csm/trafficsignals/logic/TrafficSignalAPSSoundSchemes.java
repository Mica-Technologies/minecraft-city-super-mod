package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.CsmSounds;

/**
 * The sound schemes for traffic signal APS devices.
 *
 * @author Mica Technologies
 * @version 1.0
 * @see AbstractBlockTrafficSignalAPS
 * @since 2025.1.0
 */
public class TrafficSignalAPSSoundSchemes {

  /**
   * The Campbell/PedSafety sound schemes.
   *
   * <ul>
   *   <li>Campbell Standard Voice - Walk Sign is On</li>
   *   <li>Campbell Standard Voice - Warning Lights are Flashing</li>
   *   <li>Campbell Standard Voice - Yellow Lights are Flashing</li>
   *   <li>Campbell Standard Voice - Walk Sign is On for All Crossings</li>
   *   <li>Campbell Standard Percussive (East-West)</li>
   *   <li>Campbell Standard Percussive (North-South)</li>
   *   <li>Campbell Phil Voice - Walk Sign is On</li>
   *   <li>Campbell Phil Voice - Warning Lights are Flashing</li>
   *   <li>Campbell Phil Voice - Yellow Lights are Flashing</li>
   *   <li>Campbell Phil Voice - Walk Sign is On for All Crossings</li>
   * </ul>
   *
   * @since 1.0
   */
  public static final TrafficSignalAPSSoundScheme[] CAMPBELL =
      {new TrafficSignalAPSSoundScheme("Campbell Standard Voice - Walk Sign is On",
          CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_WAIT, 30,
          CsmSounds.SOUND.CAMPBELL_WAIT, 15, CsmSounds.SOUND.CAMPBELL_WALK_SIGN_ON, 140),
          new TrafficSignalAPSSoundScheme("Campbell Standard Voice - Warning Lights are Flashing",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_WAIT_LOOK_BOTH_WAYS, 30,
              CsmSounds.SOUND.CAMPBELL_WAIT_LOOK_BOTH_WAYS, 15,
              CsmSounds.SOUND.CAMPBELL_WARNING_LIGHTS_ARE_FLASHING, 140),
          new TrafficSignalAPSSoundScheme("Campbell Standard Voice - Yellow Lights are Flashing",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_WAIT_LOOK_BOTH_WAYS, 30,
              CsmSounds.SOUND.CAMPBELL_WAIT_LOOK_BOTH_WAYS, 15,
              CsmSounds.SOUND.CAMPBELL_YELLOW_LIGHTS_ARE_FLASHING, 140),
          new TrafficSignalAPSSoundScheme(
              "Campbell Standard Voice - Walk Sign is On for All Crossings",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_WAIT, 30,
              CsmSounds.SOUND.CAMPBELL_WAIT, 80, CsmSounds.SOUND.CAMPBELL_WALK_EXCLUSIVE, 140),
          new TrafficSignalAPSSoundScheme("Campbell Standard Percussive (East-West)",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_PERC_EW, 30,
              CsmSounds.SOUND.CAMPBELL_PERC_EW, 15, CsmSounds.SOUND.CAMPBELL_WALK_SIGN_ON, 140),
          new TrafficSignalAPSSoundScheme("Campbell Standard Percussive (North-South)",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_PERC_NS, 30,
              CsmSounds.SOUND.CAMPBELL_PERC_NS, 15, CsmSounds.SOUND.CAMPBELL_WALK_SIGN_ON, 140),
          new TrafficSignalAPSSoundScheme("Campbell Phil Voice - Walk Sign is On",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_PHIL_WAIT, 30,
              CsmSounds.SOUND.CAMPBELL_PHIL_WAIT, 15, CsmSounds.SOUND.CAMPBELL_PHIL_WALK_ON, 140),
          new TrafficSignalAPSSoundScheme("Campbell Phil Voice - Warning Lights are Flashing",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_PHIL_WAIT_LOOK_BOTH_WAYS, 30,
              CsmSounds.SOUND.CAMPBELL_PHIL_WAIT_LOOK_BOTH_WAYS, 15,
              CsmSounds.SOUND.CAMPBELL_WARNING_LIGHTS_ARE_FLASHING, 140),
          new TrafficSignalAPSSoundScheme("Campbell Phil Voice - Yellow Lights are Flashing",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_PHIL_WAIT_LOOK_BOTH_WAYS, 30,
              CsmSounds.SOUND.CAMPBELL_PHIL_WAIT_LOOK_BOTH_WAYS, 15,
              CsmSounds.SOUND.CAMPBELL_YELLOW_LIGHTS_ARE_FLASHING, 140),
          new TrafficSignalAPSSoundScheme("Campbell Phil Voice - Walk Sign is On for All Crossings",
              CsmSounds.SOUND.CAMPBELL_TONE1, CsmSounds.SOUND.CAMPBELL_PHIL_WAIT, 30,
              CsmSounds.SOUND.CAMPBELL_PHIL_WAIT, 80, CsmSounds.SOUND.CAMPBELL_PHIL_WALK_EXCLUSIVE,
              140)};
  /**
   * The Polara sound schemes.
   *
   * <ul>
   *   <li>Polara Standard Rapid Tick</li>
   *   <li>Polara Voice - Walk Sign is On</li>
   *   <li>Polara Voice - Walk Sign is on for All Crossings</li>
   *   <li>Polara Spanish Standard Rapid Tick</li>
   *   <li>Polara Spanish Voice - Walk Sign is On</li>
   *   <li>Polara Spanish Voice - Walk Sign is on for All Crossings</li>
   * </ul>
   *
   * @since 1.0
   */
  public static final TrafficSignalAPSSoundScheme[] POLARA =
      {new TrafficSignalAPSSoundScheme("Polara Standard Rapid Tick", CsmSounds.SOUND.POLARA_TONE1,
          CsmSounds.SOUND.POLARA_WAIT, 30, CsmSounds.SOUND.POLARA_WAIT, 15,
          CsmSounds.SOUND.POLARA_RAPID_TICK1, 140),
          new TrafficSignalAPSSoundScheme("Polara Voice - Walk Sign is On",
              CsmSounds.SOUND.POLARA_TONE1, CsmSounds.SOUND.POLARA_WAIT, 30,
              CsmSounds.SOUND.POLARA_WAIT, 15, CsmSounds.SOUND.POLARA_WALK, 140),
          new TrafficSignalAPSSoundScheme("Polara Voice - Walk Sign is on for All Crossings",
              CsmSounds.SOUND.POLARA_TONE1, CsmSounds.SOUND.POLARA_WAIT, 30,
              CsmSounds.SOUND.POLARA_WAIT, 80, CsmSounds.SOUND.POLARA_WALK_ALL_CROSSINGS, 140),
          new TrafficSignalAPSSoundScheme("Polara Spanish Standard Rapid Tick",
              CsmSounds.SOUND.POLARA_TONE1, CsmSounds.SOUND.POLARA_LANG2_WAIT, 30,
              CsmSounds.SOUND.POLARA_LANG2_WAIT, 15, CsmSounds.SOUND.POLARA_RAPID_TICK1, 140),
          new TrafficSignalAPSSoundScheme("Polara Spanish Voice - Walk Sign is On",
              CsmSounds.SOUND.POLARA_TONE1, CsmSounds.SOUND.POLARA_LANG2_WAIT, 30,
              CsmSounds.SOUND.POLARA_LANG2_WAIT, 15, CsmSounds.SOUND.POLARA_LANG2_WALK, 140),
          new TrafficSignalAPSSoundScheme(
              "Polara Spanish Voice - Walk Sign is on for All Crossings",
              CsmSounds.SOUND.POLARA_TONE1, CsmSounds.SOUND.POLARA_LANG2_WAIT, 30,
              CsmSounds.SOUND.POLARA_LANG2_WAIT, 80,
              CsmSounds.SOUND.POLARA_LANG2_WALK_ALL_CROSSINGS, 140)};
}

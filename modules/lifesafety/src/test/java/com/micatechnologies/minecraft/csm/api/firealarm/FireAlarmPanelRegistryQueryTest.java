package com.micatechnologies.minecraft.csm.api.firealarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The membership query a panel follower asks every few ticks during an alarm answers exactly what
 * the copying snapshot's {@code contains} did, per dimension and per kind of alarm.
 */
class FireAlarmPanelRegistryQueryTest {

  private static final int OVERWORLD = 0;
  private static final int NETHER = -1;
  private static final BlockPos PANEL = new BlockPos(120, 64, -340);
  private static final BlockPos OTHER = new BlockPos(121, 64, -340);

  @AfterEach
  void clear() {
    FireAlarmPanelRegistry.unregisterAll(OVERWORLD, PANEL);
    FireAlarmPanelRegistry.unregisterAll(OVERWORLD, OTHER);
    FireAlarmPanelRegistry.unregisterAll(NETHER, PANEL);
  }

  @Test
  void answersAsTheSnapshotDid() {
    BlockPos[] asked = {PANEL, OTHER, new BlockPos(PANEL), null};
    for (int step = 0; step < 4; step++) {
      switch (step) {
        case 1:
          FireAlarmPanelRegistry.registerFireAlarm(OVERWORLD, PANEL);
          break;
        case 2:
          FireAlarmPanelRegistry.registerStormAlarm(OVERWORLD, OTHER);
          FireAlarmPanelRegistry.registerFireAlarm(NETHER, OTHER);
          break;
        case 3:
          FireAlarmPanelRegistry.unregisterFireAlarm(OVERWORLD, PANEL);
          break;
        default:
          break;
      }
      for (int dim : new int[]{OVERWORLD, NETHER}) {
        for (BlockPos pos : asked) {
          assertEquals(FireAlarmPanelRegistry.getActiveFireAlarmPanels(dim).contains(pos),
              FireAlarmPanelRegistry.isFireAlarmActive(dim, pos), "fire " + step + " " + pos);
          assertEquals(FireAlarmPanelRegistry.getActiveStormAlarmPanels(dim).contains(pos),
              FireAlarmPanelRegistry.isStormAlarmActive(dim, pos), "storm " + step + " " + pos);
        }
      }
    }
  }

  @Test
  void aFireAlarmIsNotAStormAlarm() {
    FireAlarmPanelRegistry.registerFireAlarm(OVERWORLD, PANEL);
    assertTrue(FireAlarmPanelRegistry.isFireAlarmActive(OVERWORLD, PANEL));
    assertFalse(FireAlarmPanelRegistry.isStormAlarmActive(OVERWORLD, PANEL));
    assertFalse(FireAlarmPanelRegistry.isFireAlarmActive(NETHER, PANEL));
  }
}

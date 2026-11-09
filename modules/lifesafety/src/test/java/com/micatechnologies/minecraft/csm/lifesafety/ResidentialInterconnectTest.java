package com.micatechnologies.minecraft.csm.lifesafety;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.lifesafety.ResidentialInterconnect.Pattern;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/** The residential interconnect (#335): which alarms sound when one trips, and for how long. */
class ResidentialInterconnectTest {

  private static final BlockPos HOME_A = new BlockPos(0, 64, 0);
  private static final BlockPos A2 = new BlockPos(4, 64, 0);
  private static final BlockPos A3 = new BlockPos(8, 68, 0);
  private static final BlockPos HOME_B = new BlockPos(100, 64, 0);
  private static final BlockPos B2 = new BlockPos(104, 64, 0);
  private static final BlockPos LONE = new BlockPos(200, 64, 0);
  private static final BlockPos PANEL = new BlockPos(300, 64, 0);

  private static ResidentialInterconnect twoHomes() {
    ResidentialInterconnect r = new ResidentialInterconnect();
    for (BlockPos p : Arrays.asList(HOME_A, A2, A3)) {
      r.joinGroup(HOME_A, p);
    }
    for (BlockPos p : Arrays.asList(HOME_B, B2)) {
      r.joinGroup(HOME_B, p);
    }
    return r;
  }

  private static Map<BlockPos, Pattern> played(ResidentialInterconnect r, long tick) {
    Map<BlockPos, Pattern> out = new HashMap<>();
    r.tick(tick, out::put);
    return out;
  }

  @Test
  void oneTripSoundsEveryAlarmInItsHomeAndNoOther() {
    ResidentialInterconnect r = twoHomes();
    r.tripGroup(HOME_A, Pattern.SMOKE, 1000);
    Map<BlockPos, Pattern> due = played(r, 1000);
    assertEquals(3, due.size());
    assertEquals(Pattern.SMOKE, due.get(A3));
    assertFalse(due.containsKey(HOME_B));
    assertFalse(due.containsKey(B2));
  }

  @Test
  void everyAlarmBeatsTogetherOncePerCycle() {
    ResidentialInterconnect r = twoHomes();
    r.tripGroup(HOME_A, Pattern.SMOKE, 1000);
    assertEquals(3, played(r, 1000).size());
    for (long t = 1001; t < 1000 + Pattern.SMOKE.getPeriod(); t++) {
      assertTrue(played(r, t).isEmpty(), "nothing between cycles at " + t);
    }
    assertEquals(3, played(r, 1000 + Pattern.SMOKE.getPeriod()).size());
  }

  @Test
  void anUnlinkedKeySoundsAloneAndAnEmptiedGroupIsForgotten() {
    ResidentialInterconnect r = twoHomes();
    assertEquals(Arrays.asList(LONE), r.membersOf(LONE));
    r.leaveGroup(HOME_B, HOME_B);
    r.leaveGroup(HOME_B, B2);
    assertEquals(Arrays.asList(HOME_B), r.membersOf(HOME_B));
  }

  @Test
  void anAlarmThatLeftNoLongerSounds() {
    ResidentialInterconnect r = twoHomes();
    r.leaveGroup(HOME_A, A3);
    r.tripGroup(HOME_A, Pattern.SMOKE, 0);
    Map<BlockPos, Pattern> due = played(r, 0);
    assertEquals(2, due.size());
    assertFalse(due.containsKey(A3));
  }

  @Test
  void theHeadLeavingKeepsTheRestJoined() {
    ResidentialInterconnect r = twoHomes();
    r.leaveGroup(HOME_A, HOME_A);
    r.tripGroup(HOME_A, Pattern.SMOKE, 0);
    Map<BlockPos, Pattern> due = played(r, 0);
    assertEquals(2, due.size());
    assertTrue(due.containsKey(A2) && due.containsKey(A3));
  }

  @Test
  void quietsOnItsOwnOnceDetectionsStop() {
    ResidentialInterconnect r = twoHomes();
    r.tripGroup(HOME_A, Pattern.SMOKE, 0);
    r.tick(ResidentialInterconnect.CLEAR_TICKS - 1, (p, s) -> { });
    assertEquals(Pattern.SMOKE, r.soundingPattern(HOME_A));
    r.tick(ResidentialInterconnect.CLEAR_TICKS, (p, s) -> { });
    assertNull(r.soundingPattern(HOME_A));
    assertTrue(r.idle());
  }

  @Test
  void eachDetectionHoldsItUp() {
    ResidentialInterconnect r = twoHomes();
    r.tripGroup(HOME_A, Pattern.SMOKE, 0);
    r.tripGroup(HOME_A, Pattern.SMOKE, 500);
    r.tick(ResidentialInterconnect.CLEAR_TICKS + 10, (p, s) -> { });
    assertEquals(Pattern.SMOKE, r.soundingPattern(HOME_A));
  }

  @Test
  void smokeOutranksCoAndRestartsTheBeat() {
    ResidentialInterconnect r = twoHomes();
    r.tripGroup(HOME_A, Pattern.CO, 0);
    assertEquals(Pattern.CO, played(r, 0).get(A2));
    r.tripGroup(HOME_A, Pattern.SMOKE, 30);
    assertEquals(Pattern.SMOKE, r.soundingPattern(HOME_A));
    assertEquals(Pattern.SMOKE, played(r, 30).get(A2), "smoke starts its cycle at once");
    r.tripGroup(HOME_A, Pattern.CO, 40);
    assertEquals(Pattern.SMOKE, r.soundingPattern(HOME_A), "CO does not take smoke's place");
  }

  @Test
  void testSoundsTwoCyclesAndHushSilencesDetections() {
    ResidentialInterconnect r = twoHomes();
    r.testGroup(HOME_A, Pattern.SMOKE, 0);
    assertEquals(3, played(r, 0).size());
    assertEquals(3, played(r, Pattern.SMOKE.getPeriod()).size());
    r.tick(2L * Pattern.SMOKE.getPeriod(), (p, s) -> { });
    assertNull(r.soundingPattern(HOME_A), "a test ends after two cycles");

    r.tripGroup(HOME_A, Pattern.SMOKE, 1000);
    assertTrue(r.hushGroup(HOME_A, 1010));
    assertFalse(r.tripGroup(HOME_A, Pattern.SMOKE, 1500), "hushed");
    assertTrue(played(r, 1500).isEmpty());
    assertTrue(r.tripGroup(HOME_A, Pattern.SMOKE, 1010 + ResidentialInterconnect.HUSH_TICKS),
        "the hush runs out");
    assertFalse(r.hushGroup(HOME_B, 0), "nothing to hush in a quiet home");
  }

  @Test
  void panelSoundsItsMembersAndKeepsTheBeatWhenAZoneJoins() {
    ResidentialInterconnect r = new ResidentialInterconnect();
    r.setPanelGroup(PANEL, Pattern.SMOKE, Arrays.asList(A2), 100);
    assertEquals(1, played(r, 100).size());
    // a second zone's alarm joins: same start, so it beats with the first
    r.setPanelGroup(PANEL, Pattern.SMOKE, Arrays.asList(A2, A3), 120);
    assertTrue(played(r, 120).isEmpty());
    assertEquals(2, played(r, 100 + Pattern.SMOKE.getPeriod()).size());
    r.setPanelGroup(PANEL, null, null, 300);
    assertFalse(r.panelSounding(PANEL));
    assertTrue(r.idle());
  }

  @Test
  void anAlarmOnTwoSystemsPlaysOnceWithTheStrongerSignal() {
    ResidentialInterconnect r = new ResidentialInterconnect();
    r.setPanelGroup(PANEL, Pattern.CO, Arrays.asList(LONE), 0);
    r.tripGroup(LONE, Pattern.SMOKE, 0);
    Map<BlockPos, Pattern> due = played(r, 0);
    assertEquals(1, due.size());
    assertEquals(Pattern.SMOKE, due.get(LONE));
  }
}

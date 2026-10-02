package com.micatechnologies.minecraft.csm.technology.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TileEntityBellControllerNbtTest {

  @Test
  @DisplayName("the schedule and links survive a save and load")
  void roundTrip() {
    TileEntityBellController a = new TileEntityBellController();
    a.setSchedule(new BellSchedule(Arrays.asList(
        new BellSchedule.Entry(530, BellTone.BELL, "", true),
        new BellSchedule.Entry(480, BellTone.ANNOUNCE, "Good morning", true),
        new BellSchedule.Entry(900, BellTone.CHIME, "", false))));
    a.link(new BlockPos(1, 2, 3));
    a.link(new BlockPos(-40, 70, 12));
    NBTTagCompound tag = a.writeNBT(new NBTTagCompound());

    TileEntityBellController b = new TileEntityBellController();
    b.readNBT(tag);
    assertEquals(3, b.getSchedule().getEntries().size());
    BellSchedule.Entry first = b.getSchedule().getEntries().get(0);
    assertEquals(480, first.getMinute());
    assertEquals(BellTone.ANNOUNCE, first.getTone());
    assertEquals("Good morning", first.getText());
    assertFalse(b.getSchedule().getEntries().get(2).isEnabled());
    assertEquals(Arrays.asList(new BlockPos(1, 2, 3), new BlockPos(-40, 70, 12)), b.getLinks());
  }

  @Test
  @DisplayName("an empty controller writes nothing and reads as empty")
  void emptyWritesNothing() {
    NBTTagCompound tag = new TileEntityBellController().writeNBT(new NBTTagCompound());
    assertTrue(tag.getKeySet().isEmpty());
    TileEntityBellController b = new TileEntityBellController();
    b.readNBT(tag);
    assertTrue(b.getSchedule().isEmpty());
    assertTrue(b.getLinks().isEmpty());
  }

  @Test
  @DisplayName("links are not doubled and stop at the limit")
  void linkLimit() {
    TileEntityBellController c = new TileEntityBellController();
    assertTrue(c.link(BlockPos.ORIGIN));
    assertFalse(c.link(BlockPos.ORIGIN));
    for (int i = 1; i < TileEntityBellController.MAX_LINKS; i++) {
      assertTrue(c.link(new BlockPos(i, 0, 0)));
    }
    assertFalse(c.link(new BlockPos(-1, 0, 0)));
    assertTrue(c.unlink(BlockPos.ORIGIN));
    assertEquals(TileEntityBellController.MAX_LINKS - 1, c.getLinks().size());
  }
}

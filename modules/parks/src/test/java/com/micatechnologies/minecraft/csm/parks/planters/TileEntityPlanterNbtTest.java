package com.micatechnologies.minecraft.csm.parks.planters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityPlanterNbtTest {

  private static TileEntityPlanter read(NBTTagCompound tag) {
    TileEntityPlanter te = new TileEntityPlanter();
    te.readNBT(tag);
    return te;
  }

  @Test
  void roundTrip() {
    NBTTagCompound in = new NBTTagCompound();
    in.setString("plant", "minecraft:red_flower");
    in.setInteger("variant", 3);
    NBTTagCompound out = read(in).writeNBT(new NBTTagCompound());
    assertEquals("minecraft:red_flower", out.getString("plant"));
    assertEquals(3, out.getInteger("variant"));
    TileEntityPlanter again = read(out);
    assertEquals("minecraft:red_flower", again.getPlant());
    assertEquals(3, again.getVariant());
  }

  @Test
  void anEmptyPlanterWritesNothing() {
    NBTTagCompound out = read(new NBTTagCompound()).writeNBT(new NBTTagCompound());
    assertFalse(out.hasKey("plant"));
    assertFalse(out.hasKey("variant"));
  }

  @Test
  void anEmptyNameEmptiesThePlanter() {
    NBTTagCompound in = new NBTTagCompound();
    in.setString("plant", "");
    in.setInteger("variant", 2);
    TileEntityPlanter te = read(in);
    assertNull(te.getPlant());
    assertFalse(te.hasPlant());
    assertEquals(0, te.getVariant());
  }

  @Test
  void aBareNameIsCsmsAndASuffixIsTheVariant() {
    NBTTagCompound in = new NBTTagCompound();
    in.setString("plant", "shrub_lavender");
    assertEquals("csm:shrub_lavender", read(in).getPlant());
    in.setString("plant", "minecraft:sapling:2");
    assertEquals(2, read(in).getVariant());
    in.setInteger("variant", 4);
    assertEquals(4, read(in).getVariant(), "an explicit variant wins");
  }

  @Test
  void theVariantIsClamped() {
    NBTTagCompound in = new NBTTagCompound();
    in.setString("plant", "minecraft:red_flower");
    in.setInteger("variant", 40);
    assertEquals(15, read(in).getVariant());
    in.setInteger("variant", -1);
    assertEquals(0, read(in).getVariant());
  }

  @Test
  void anUnreadableNameIsEmpty() {
    NBTTagCompound in = new NBTTagCompound();
    in.setString("plant", "not a plant!");
    assertFalse(read(in).hasPlant());
  }

  @Test
  void theChunkRebuildsOnlyWhenThePlantChanges() {
    NBTTagCompound in = new NBTTagCompound();
    in.setString("plant", "csm:shrub_lavender");
    long lavender = read(in).getBakedModelKey();
    assertEquals(lavender, read(in).getBakedModelKey());
    in.setInteger("variant", 1);
    assertNotEquals(lavender, read(in).getBakedModelKey());
    in.setString("plant", "csm:shrub_rosemary");
    assertNotEquals(lavender, read(in).getBakedModelKey());
    assertTrue(read(new NBTTagCompound()).getBakedModelKey() != lavender);
  }
}

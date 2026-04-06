package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SerializationUtils}, covering BlockPos-to-NBT round-trip serialization,
 * list serialization, and null/edge-case handling.
 */
class SerializationUtilsTest {

  // region: BlockPos single value round-trip

  @Test
  void blockPosRoundTripPreservesCoordinates() {
    BlockPos pos = new BlockPos(100, 64, -200);
    NBTTagCompound compound = new NBTTagCompound();
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, "pos", pos);

    BlockPos result = SerializationUtils.getBlockPosFromNBTOrNull(compound, "pos");
    assertNotNull(result);
    assertEquals(pos, result);
  }

  @Test
  void blockPosRoundTripWithNegativeCoordinates() {
    BlockPos pos = new BlockPos(-30000000, 0, 30000000);
    NBTTagCompound compound = new NBTTagCompound();
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, "test", pos);

    BlockPos result = SerializationUtils.getBlockPosFromNBTOrNull(compound, "test");
    assertNotNull(result);
    assertEquals(pos.getX(), result.getX());
    assertEquals(pos.getY(), result.getY());
    assertEquals(pos.getZ(), result.getZ());
  }

  @Test
  void blockPosOriginRoundTrip() {
    BlockPos pos = BlockPos.ORIGIN;
    NBTTagCompound compound = new NBTTagCompound();
    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, "origin", pos);

    BlockPos result = SerializationUtils.getBlockPosFromNBTOrNull(compound, "origin");
    assertNotNull(result);
    assertEquals(0, result.getX());
    assertEquals(0, result.getY());
    assertEquals(0, result.getZ());
  }

  // endregion

  // region: Null handling for single BlockPos

  @Test
  void getBlockPosFromNullCompoundReturnsNull() {
    BlockPos result = SerializationUtils.getBlockPosFromNBTOrNull(null, "key");
    assertNull(result);
  }

  @Test
  void getBlockPosFromMissingKeyReturnsNull() {
    NBTTagCompound compound = new NBTTagCompound();
    BlockPos result = SerializationUtils.getBlockPosFromNBTOrNull(compound, "nonexistent");
    assertNull(result);
  }

  @Test
  void setNullBlockPosRemovesKeyFromCompound() {
    NBTTagCompound compound = new NBTTagCompound();
    compound.setLong("pos", 12345L);
    assertTrue(compound.hasKey("pos"));

    SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, "pos", null);
    assertFalse(compound.hasKey("pos"));
  }

  @Test
  void setNullBlockPosOnEmptyCompoundDoesNotThrow() {
    NBTTagCompound compound = new NBTTagCompound();
    assertDoesNotThrow(
        () -> SerializationUtils.setBlockPosInNBTOrRemoveIfNull(compound, "key", null));
  }

  // endregion

  // region: BlockPos list round-trip

  @Test
  void blockPosListRoundTripEmpty() {
    List<BlockPos> original = Collections.emptyList();
    NBTTagLongArray nbtArray = SerializationUtils.getBlockPosNBTArrayFromBlockPosList(original);
    List<BlockPos> result = SerializationUtils.getBlockPosListFromBlockPosNBTArray(nbtArray);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void blockPosListRoundTripSingleItem() {
    BlockPos pos = new BlockPos(10, 20, 30);
    List<BlockPos> original = Collections.singletonList(pos);
    NBTTagLongArray nbtArray = SerializationUtils.getBlockPosNBTArrayFromBlockPosList(original);
    List<BlockPos> result = SerializationUtils.getBlockPosListFromBlockPosNBTArray(nbtArray);

    assertEquals(1, result.size());
    assertEquals(pos, result.get(0));
  }

  @Test
  void blockPosListRoundTripMultipleItems() {
    List<BlockPos> original = Arrays.asList(
        new BlockPos(1, 2, 3),
        new BlockPos(-100, 255, -100),
        new BlockPos(0, 0, 0),
        new BlockPos(30000000, 128, -30000000));

    NBTTagLongArray nbtArray = SerializationUtils.getBlockPosNBTArrayFromBlockPosList(original);
    List<BlockPos> result = SerializationUtils.getBlockPosListFromBlockPosNBTArray(nbtArray);

    assertEquals(original.size(), result.size());
    for (int i = 0; i < original.size(); i++) {
      assertEquals(original.get(i), result.get(i),
          "Mismatch at index " + i);
    }
  }

  @Test
  void blockPosListFromNonArrayNBTReturnsEmptyList() {
    // Pass a non-NBTTagLongArray NBTBase (e.g. NBTTagCompound)
    NBTBase notAnArray = new NBTTagCompound();
    List<BlockPos> result = SerializationUtils.getBlockPosListFromBlockPosNBTArray(notAnArray);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void blockPosListFromNullReturnsEmptyList() {
    List<BlockPos> result = SerializationUtils.getBlockPosListFromBlockPosNBTArray(null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // endregion
}

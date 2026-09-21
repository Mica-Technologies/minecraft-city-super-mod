package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Arrow;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Legend;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class TileEntityExitSignNbtTest {

  private static final ExitSignConfig SAMPLE = new ExitSignConfig(Arrow.BOTH, Letters.GREEN,
      Housing.BLACK, Mount.END_RIGHT, Heads.ROUND, Legend.SALIDA);

  @Test
  void shortKeyRoundTrip() {
    TileEntityExitSign te = new TileEntityExitSign();
    te.readNBT(SAMPLE.write(new NBTTagCompound()));

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());
    assertEquals(3, output.getByte("ar"));
    assertEquals(1, output.getByte("lc"));
    assertEquals(1, output.getByte("hs"));
    assertEquals(3, output.getByte("mt"));
    assertEquals(2, output.getByte("hd"));
    assertEquals(1, output.getByte("lg"));

    TileEntityExitSign again = new TileEntityExitSign();
    again.readNBT(output);
    assertEquals(SAMPLE, again.getConfig());
  }

  @Test
  void emptyCompoundReadsAsUnset() {
    TileEntityExitSign te = new TileEntityExitSign();
    te.readNBT(new NBTTagCompound());
    assertEquals(TileEntityExitSign.UNSET, te.getConfig());
  }

  @Test
  void outOfRangeOrdinalKeepsTheFallback() {
    NBTTagCompound input = SAMPLE.write(new NBTTagCompound());
    input.setByte("ar", (byte) 99);
    input.setByte("hd", (byte) -1);

    TileEntityExitSign te = new TileEntityExitSign();
    te.readNBT(input);
    assertEquals(TileEntityExitSign.UNSET.getArrow(), te.getConfig().getArrow());
    assertEquals(TileEntityExitSign.UNSET.getHeads(), te.getConfig().getHeads());
    assertEquals(Legend.SALIDA, te.getConfig().getLegend());
  }

  @Test
  void packIsDistinctPerOption() {
    assertNotEquals(SAMPLE.pack(), SAMPLE.withArrow(Arrow.LEFT).pack());
    assertNotEquals(SAMPLE.pack(), SAMPLE.withLetters(Letters.RED).pack());
    assertNotEquals(SAMPLE.pack(), SAMPLE.withHousing(Housing.WHITE).pack());
    assertNotEquals(SAMPLE.pack(), SAMPLE.withMount(Mount.WALL).pack());
    assertNotEquals(SAMPLE.pack(), SAMPLE.withHeads(Heads.NONE).pack());
    assertNotEquals(SAMPLE.pack(), SAMPLE.withLegend(Legend.EXIT).pack());
  }
}

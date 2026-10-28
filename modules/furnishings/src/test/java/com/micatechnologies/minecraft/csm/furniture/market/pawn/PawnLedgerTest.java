package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * The pawn ledger: emerald remainders add up to whole emeralds, the daily allowance resets with
 * the day, and a record survives a save.
 */
class PawnLedgerTest {

  private static final double E = 1e-9;

  @Test
  void remaindersAddUpToWholeEmeralds() {
    double[] a = PawnEconomy.splitEmeralds(0, 4.0, 10.0);
    assertEquals(0, a[0], E);
    assertEquals(4.0, a[1], E);
    double[] b = PawnEconomy.splitEmeralds(a[1], 7.5, 10.0);
    assertEquals(1, b[0], E);
    assertEquals(1.5, b[1], E);
    double[] c = PawnEconomy.splitEmeralds(0, 30.0, 10.0);
    assertEquals(3, c[0], E);
    assertEquals(0, c[1], E);
    // Ten payouts of a tenth make one emerald exactly, despite binary fractions.
    double r = 0;
    long whole = 0;
    for (int i = 0; i < 10; i++) {
      double[] s = PawnEconomy.splitEmeralds(r, 0.1, 1.0);
      whole += (long) s[0];
      r = s[1];
    }
    assertEquals(1, whole);
  }

  @Test
  void allowanceResetsWithTheDay() {
    PawnLedger ledger = new PawnLedger("t");
    UUID p = UUID.randomUUID();
    assertEquals(Double.POSITIVE_INFINITY, ledger.allowanceToday(p, 100, 0), E);
    assertEquals(50, ledger.allowanceToday(p, 100, 50), E);
    ledger.recordPaid(p, 100, 30);
    assertEquals(20, ledger.allowanceToday(p, 100, 50), E);
    ledger.recordPaid(p, 100, 25);
    assertEquals(0, ledger.allowanceToday(p, 100, 50), E);
    assertEquals(50, ledger.allowanceToday(p, 101, 50), E);
    ledger.recordPaid(p, 101, 5);
    assertEquals(45, ledger.allowanceToday(p, 101, 50), E);
  }

  @Test
  void survivesASave() {
    PawnLedger ledger = new PawnLedger("t");
    UUID p = UUID.randomUUID();
    ledger.entry(p).emeraldRemainder = 3.25;
    ledger.entry(p).owed = 12.5;
    ledger.recordPaid(p, 7, 9);
    UUID empty = UUID.randomUUID();
    ledger.entry(empty);
    NBTTagCompound nbt = ledger.writeToNBT(new NBTTagCompound());
    assertEquals(1, nbt.getTagList("p", 10).tagCount());
    PawnLedger back = new PawnLedger("t");
    back.readFromNBT(nbt);
    assertEquals(3.25, back.entry(p).emeraldRemainder, E);
    assertEquals(12.5, back.entry(p).owed, E);
    assertEquals(41, back.allowanceToday(p, 7, 50), E);
  }
}

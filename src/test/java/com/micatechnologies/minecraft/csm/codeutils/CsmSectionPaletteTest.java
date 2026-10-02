package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.chunk.BlockStateContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link CsmSectionPalette#mayContain} may only say "no" when no cell of the section holds a
 * matching state: the tile entity backfill skips a section on that answer, and a wrong "no" would
 * leave a block without its tile entity. These build real chunk sections, through every palette a
 * section can have, and check the answer against reading every cell.
 */
class CsmSectionPaletteTest {

  private static final Predicate<IBlockState> CHEST = s -> s.getBlock() == Blocks.CHEST;

  @BeforeAll
  static void bootstrap() {
    Bootstrap.register();
  }

  @Test
  void thePaletteCanBeRead() {
    assertTrue(CsmSectionPalette.available(), "the palette field resolves in a dev workspace");
  }

  @Test
  void aSectionWithoutTheStateIsSkipped() {
    BlockStateContainer data = new BlockStateContainer();
    data.set(1, 2, 3, Blocks.STONE.getDefaultState());
    data.set(4, 5, 6, Blocks.GLASS.getDefaultState());
    assertFalse(CsmSectionPalette.mayContain(data, CHEST));
    assertTrue(CsmSectionPalette.mayContain(data, s -> s.getBlock() == Blocks.GLASS));
  }

  @Test
  void aSectionWithTheStateIsWalked() {
    BlockStateContainer data = new BlockStateContainer();
    data.set(1, 2, 3, Blocks.STONE.getDefaultState());
    data.set(15, 15, 15, Blocks.CHEST.getDefaultState());
    assertTrue(CsmSectionPalette.mayContain(data, CHEST));
  }

  @Test
  void aStateNoLongerUsedStillCountsWhichIsSafe() {
    BlockStateContainer data = new BlockStateContainer();
    data.set(0, 0, 0, Blocks.CHEST.getDefaultState());
    data.set(0, 0, 0, Blocks.STONE.getDefaultState());
    assertTrue(CsmSectionPalette.mayContain(data, CHEST));
  }

  @Test
  void theHashPaletteIsReadToo() {
    // More than 16 states moves the section to the hash map palette.
    List<IBlockState> states = distinctStates(40);
    BlockStateContainer data = fill(states);
    assertFalse(CsmSectionPalette.mayContain(data, CHEST));
    data.set(7, 7, 7, Blocks.CHEST.getDefaultState());
    assertTrue(CsmSectionPalette.mayContain(data, CHEST));
  }

  @Test
  void theGlobalPaletteAlwaysSaysMaybe() {
    // More than 256 states moves the section to the global registry palette.
    BlockStateContainer data = fill(distinctStates(300));
    assertTrue(CsmSectionPalette.mayContain(data, CHEST));
  }

  /** Random sections of 1 to 400 states: "no" must always mean no cell matches. */
  @Test
  void neverSaysNoWhenACellMatches() {
    Random rnd = new Random(20261002L);
    List<IBlockState> pool = distinctStates(600);
    int checked = 0;
    for (int trial = 0; trial < 200; trial++) {
      BlockStateContainer data = new BlockStateContainer();
      int kinds = 1 + rnd.nextInt(rnd.nextBoolean() ? 20 : 400);
      int cells = 1 + rnd.nextInt(4096);
      int chestAt = rnd.nextBoolean() ? rnd.nextInt(cells) : -1;
      for (int i = 0; i < cells; i++) {
        IBlockState state = i == chestAt ? Blocks.CHEST.getDefaultState()
            : pool.get(rnd.nextInt(kinds));
        data.set(rnd.nextInt(16), rnd.nextInt(16), rnd.nextInt(16), state);
      }
      boolean any = false;
      for (int x = 0; x < 16 && !any; x++) {
        for (int y = 0; y < 16 && !any; y++) {
          for (int z = 0; z < 16 && !any; z++) {
            any = CHEST.test(data.get(x, y, z));
          }
        }
      }
      if (any) {
        assertTrue(CsmSectionPalette.mayContain(data, CHEST), "trial " + trial);
        checked++;
      }
    }
    assertTrue(checked > 50, "enough sections held a chest: " + checked);
  }

  private static BlockStateContainer fill(List<IBlockState> states) {
    BlockStateContainer data = new BlockStateContainer();
    for (int i = 0; i < states.size(); i++) {
      data.set(i & 15, (i >> 8) & 15, (i >> 4) & 15, states.get(i));
    }
    return data;
  }

  /** Distinct registered states other than chests. */
  private static List<IBlockState> distinctStates(int n) {
    List<IBlockState> out = new ArrayList<>();
    for (IBlockState s : Block.BLOCK_STATE_IDS) {
      if (s.getBlock() == Blocks.CHEST) {
        continue;
      }
      out.add(s);
      if (out.size() == n) {
        break;
      }
    }
    return out;
  }
}

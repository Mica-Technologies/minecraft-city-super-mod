package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import net.minecraft.world.chunk.BlockStatePaletteLinear;
import net.minecraft.world.chunk.IBlockStatePalette;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * Answers "could this chunk section hold such a block?" from the section's palette, without
 * reading its 4,096 cells.
 *
 * <p>A section stores a palette of the states it uses and an index into it per cell. While a
 * section uses at most 256 different states the palette is its own small list
 * ({@link BlockStatePaletteLinear} up to 16, {@link BlockStatePaletteHashMap} up to 256), and every
 * state in the section is in it. States are never removed from it until the section is rewritten,
 * so it can hold a state no cell uses any more, never the other way round: if no palette state
 * matches, no cell does. Past 256 states the section indexes the global state registry instead,
 * which says nothing about the section, and the answer is "maybe".</p>
 *
 * <p>The palette is a protected field of {@link BlockStateContainer} with no accessor. It is
 * found once by its SRG name (the release jar runs against obfuscated Minecraft), then by its MCP
 * name (a dev workspace and unit tests). If neither resolves, every answer is "maybe", which makes
 * callers read every cell as they always did, and that is logged once.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public final class CsmSectionPalette {

  /** Most ids a {@link BlockStatePaletteHashMap} hands out (8 bits); a linear one holds 16. */
  static final int MAX_LOCAL_PALETTE_IDS = 256;

  /** Ids a {@link BlockStatePaletteLinear} holds (4 bits). */
  static final int LINEAR_PALETTE_IDS = 16;

  private static final String PALETTE_SRG = "field_186022_c";
  private static final String PALETTE_MCP = "palette";

  @Nullable
  private static final Field PALETTE = findPalette();

  private CsmSectionPalette() {
  }

  @Nullable
  private static Field findPalette() {
    try {
      return ObfuscationReflectionHelper.findField(BlockStateContainer.class, PALETTE_SRG);
    } catch (Throwable srgFailed) {
      try {
        Field f = BlockStateContainer.class.getDeclaredField(PALETTE_MCP);
        f.setAccessible(true);
        return f;
      } catch (Throwable mcpFailed) {
        Csm.getLogger().warn("Chunk section palettes cannot be read ({}); the tile entity "
            + "backfill will read every block of every section it checks.", srgFailed.toString());
        return null;
      }
    }
  }

  /** Whether section palettes can be read at all in this game. */
  public static boolean available() {
    return PALETTE != null;
  }

  /**
   * Whether the section might hold a state the test accepts.
   *
   * @param data the section's block state container
   * @param test the states looked for
   *
   * @return false only when the section's own palette has been read and no state in it passes;
   *     true when one does, or when the palette cannot be read or is the global one
   */
  public static boolean mayContain(BlockStateContainer data, Predicate<IBlockState> test) {
    if (PALETTE == null) {
      return true;
    }
    IBlockStatePalette palette;
    try {
      palette = (IBlockStatePalette) PALETTE.get(data);
    } catch (Throwable t) {
      return true;
    }
    if (!(palette instanceof BlockStatePaletteLinear)
        && !(palette instanceof BlockStatePaletteHashMap)) {
      return true; // the global registry palette, or a mod's: no list of the section's states
    }
    // Both hand out ids from 0 and answer null for an id they have not given out; a linear
    // palette holds 16 at most.
    int ids = palette instanceof BlockStatePaletteLinear ? LINEAR_PALETTE_IDS
        : MAX_LOCAL_PALETTE_IDS;
    for (int id = 0; id < ids; id++) {
      IBlockState state = palette.getBlockState(id);
      if (state != null && test.test(state)) {
        return true;
      }
    }
    return false;
  }
}

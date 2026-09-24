package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A Residential block that stores things in a {@link TileEntityResidentialStorage}: the TV stand
 * and sideboard, the kitchen cabinets, the refrigerator and the chest freezer. The storage screen
 * ({@code NoveltiesGuiProvider}) opens for any block that is one, at the position it names.
 *
 * @since 2026.9
 */
public interface IResidentialStorage {

  /**
   * How many slots a block holds, a multiple of nine (zero: it holds nothing).
   *
   * @return the slot count
   */
  int getSlots();

  /**
   * Where the block's slots are: its own position, or for a piece two blocks tall its lower
   * half's.
   *
   * @param world the world
   * @param pos   the block
   *
   * @return the position of the tile entity that holds the slots
   */
  default BlockPos getStoragePos(IBlockAccess world, BlockPos pos) {
    return pos;
  }

  /**
   * The sound played when the first player opens it, or null for none.
   *
   * @return the sound
   */
  @Nullable
  default ICsmSound getOpenSound() {
    return null;
  }

  /**
   * The sound played when the last player closes it, or null for none.
   *
   * @return the sound
   */
  @Nullable
  default ICsmSound getCloseSound() {
    return null;
  }
}

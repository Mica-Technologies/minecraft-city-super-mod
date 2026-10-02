package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCloset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.util.BlockRenderLayer;

/**
 * A school's trophy case: two blocks tall, set side by side into a row along a wall as the
 * lockers are ({@link BlockCloset}: end panels, here glass sides, only where the row stops), a
 * cupboard below and the trophies on glass shelves behind a glass front above. The trophies are
 * part of the model; the game picks one of two arrangements for each block by its position, so a
 * row does not repeat. The cupboard holds what is put in it.
 *
 * <p>Its glass is drawn in the translucent layer and the rest, the trophies included, in the
 * cutout layer ({@link ICsmGlassFronted}), as the store's display coolers are.</p>
 *
 * @since 2026.10
 */
public class BlockTrophyCase extends BlockCloset implements ICsmGlassFronted {

  /**
   * Constructs a trophy case.
   *
   * @param registryName its registry name, ending in its wood
   * @param box          its box facing north, in sixteenths from the floor (up to 32)
   * @param slots        how many slots its cupboard holds
   * @param openSound    the sound of its doors opening, or null
   * @param closeSound   the sound of them closing, or null
   */
  public BlockTrophyCase(String registryName, int[] box, int slots, @Nullable ICsmSound openSound,
      @Nullable ICsmSound closeSound) {
    super(registryName, box, slots, openSound, closeSound);
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}

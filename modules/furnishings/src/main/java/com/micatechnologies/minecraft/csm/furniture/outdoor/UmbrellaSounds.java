package com.micatechnologies.minecraft.csm.furniture.outdoor;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The sound of an umbrella's canopy opening or furling: the soft flap of cloth (vanilla's wool),
 * a little higher opening than closing.
 *
 * @since 2026.9
 */
final class UmbrellaSounds {

  private UmbrellaSounds() {
  }

  /**
   * Plays the canopy's sound at {@code pos} (server side, heard by everyone near).
   *
   * @param world the world
   * @param pos   the umbrella
   * @param open  whether it has just opened
   */
  static void play(World world, BlockPos pos, boolean open) {
    world.playSound(null, pos, SoundEvents.BLOCK_CLOTH_PLACE, SoundCategory.BLOCKS, 0.8F,
        open ? 0.9F : 0.7F);
    world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.BLOCKS,
        0.25F, open ? 0.7F : 0.55F);
  }
}

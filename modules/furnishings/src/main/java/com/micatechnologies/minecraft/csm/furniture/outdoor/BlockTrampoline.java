package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockDiningTable;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A backyard trampoline: a block of mat 0.75 m up that joins on all four sides, as the dining
 * table does, into a trampoline of any rectangle, its padded frame only round the outside and a
 * leg at each outer corner ({@code gen_furniture_outdoor.py}). Landing on it bounces
 * ({@link Bounce}): no fall damage, back up with most of the speed, higher with each jump made
 * as it lands, and a quiet boing; sneak to stop.
 *
 * @since 2026.9
 */
public class BlockTrampoline extends BlockDiningTable implements IBouncy {

  /**
   * Constructs a trampoline.
   *
   * @param registryName its registry name, ending in its pad's colour
   */
  public BlockTrampoline(String registryName) {
    super(registryName);
    setSoundType(SoundType.CLOTH);
  }

  @Override
  public void onFallenUpon(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Entity entity,
      float fallDistance) {
    Bounce.fallenUpon(world, pos, entity, fallDistance);
  }

  @Override
  public void onLanded(@Nonnull World world, @Nonnull Entity entity) {
    Bounce.landed(this, world, entity);
  }
}

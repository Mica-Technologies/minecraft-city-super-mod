package com.micatechnologies.minecraft.csm;

import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

/**
 * The world generator for the City Super Mod.
 *
 * @version 1.0
 * @since 2023.2.1
 */
public class CsmWorldGenerator implements IWorldGenerator {

  /**
   * The weight to assign to {@link CsmWorldGenerator}. Heavy weights tend to sink to the bottom of
   * list of world generators (i.e. they run later).
   *
   * @since 1.0
   */
  public static final int WORLD_GENERATION_WEIGHT = 5;

  /**
   * Generate some world. This method is called by Minecraft Forge during world generation.
   *
   * @param random         the chunk specific {@link Random}.
   * @param chunkX         the chunk X coordinate of this chunk.
   * @param chunkZ         the chunk Z coordinate of this chunk.
   * @param world          : additionalData[0] The minecraft {@link World} we're generating for.
   * @param chunkGenerator : additionalData[1] The {@link IChunkProvider} that is generating.
   * @param chunkProvider  : additionalData[2] {@link IChunkProvider} that is requesting the world
   *                       generation.
   *
   * @since 1.0
   */
  @Override
  public void generate(Random random,
      int chunkX,
      int chunkZ,
      World world,
      IChunkGenerator chunkGenerator,
      IChunkProvider chunkProvider) {
    // Not implemented (yet)
    // There are no blocks to generate automatically in a new world.
  }
}

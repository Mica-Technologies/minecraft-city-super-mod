package com.micatechnologies.minecraft.csm.buildingmaterials.stairs;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockStairs;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/**
 * A stair of the Stairs &amp; Railings tab: steel pan, concrete with a safety nosing, open treads
 * on a stringer, and residential treads and risers. A vanilla stair in everything but its models,
 * which are this tab's own (the nosing drawn at each tread's front, so the blockstate turns the
 * model without locking its uvs), and so what a {@link BlockStairRailing} follows.
 *
 * @since 2026.10
 */
public class BlockStairFlight extends AbstractBlockStairs {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * @param registryName its registry name
   * @param wood         timber treads rather than steel or concrete (its sound, tool and the
   *                     block it behaves as)
   */
  public BlockStairFlight(String registryName, boolean wood) {
    super(stash(registryName, wood), wood ? SoundType.WOOD : SoundType.STONE,
        wood ? "axe" : "pickaxe", 0, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Block stash(String registryName, boolean wood) {
    PENDING.set(registryName);
    return wood ? Blocks.PLANKS : Blocks.STONE;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }
}

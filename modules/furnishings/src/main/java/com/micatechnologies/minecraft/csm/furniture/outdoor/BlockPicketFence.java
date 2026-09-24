package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockFence;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

/**
 * A white picket fence: a vanilla fence in every way that matters -- it joins other fences of
 * wood, a fence gate (its own {@link BlockPicketGate} included) and the solid side of a block,
 * stands a fence's height to jump over, and takes a lead -- drawn as a square post with a capped
 * top and, toward each neighbour it joins, two rails with pointed pickets on them
 * ({@code gen_furniture_outdoor.py}).
 *
 * @since 2026.9
 */
public class BlockPicketFence extends AbstractBlockFence {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a picket fence.
   *
   * @param registryName its registry name, ending in its paint
   */
  public BlockPicketFence(String registryName) {
    super(stash(registryName), SoundType.WOOD, "axe", 0, 2.0F, 5.0F, 0.0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.WOOD;
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

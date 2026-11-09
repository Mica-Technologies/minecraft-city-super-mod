package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The outside end of a PTAC's wall sleeve: the architectural louvre grille on a building's face
 * under each hotel room window, its back against the wall the placer faces, at the height of a
 * {@link BlockHvacPtac} on the floor inside. Decorative. Model from
 * {@code dev-env-utils/scripts/gen_hvac_units.py}.
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class BlockHvacPtacWallGrille extends AbstractBlockRotatableNSEW {

  /** The grille, 15.5 px wide and 7 tall, a pixel proud of the wall, facing north. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.015625, 0.0625, 0.9375, 0.984375,
      0.5, 1.0);

  public BlockHvacPtacWallGrille() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "hvac_ptac_wall_grille";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}

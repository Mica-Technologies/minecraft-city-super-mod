package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSetBasic;
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
 * Ground-face block, sometimes called burnished: the face is ground back after curing
 * until the aggregate shows through and polishes.
 *
 * <p>What a lobby or a school corridor is faced in when plain block is too plain.</p>
 *
 * <p>A block, stairs, slab and fence set, like the colour metal sets beside it. Laid in running
 * bond, and its faces differ: the sides show the coursing and the top shows the units end on with
 * their two hollow cores.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetCmuGroundface extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetCmuGroundface}.
   *
   * @since 1.0
   */
  public BlockSetCmuGroundface() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "cmu_groundface";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return true;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}

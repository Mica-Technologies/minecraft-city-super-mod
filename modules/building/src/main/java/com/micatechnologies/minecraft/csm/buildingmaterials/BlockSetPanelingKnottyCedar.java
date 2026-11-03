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
 * Knotty cedar V-groove paneling: tongue-and-groove cedar boards run plumb, a V at every joint, the
 * wall and ceiling of a lodge or cabin interior. Unlike the accent wall panels, which hang on a
 * wall, this is the wall: a block, stairs, slab and fence set. The textures come from
 * {@code dev-env-utils/scripts/gen_siding.py}, which draws it as it draws the board and batten.
 *
 * @version 1.0
 * @since 2026.11
 */
public class BlockSetPanelingKnottyCedar extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetPanelingKnottyCedar}.
   *
   * @since 1.0
   */
  public BlockSetPanelingKnottyCedar() {
    super(Material.WOOD, SoundType.WOOD, "axe", 0, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "paneling_knotty_cedar";
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

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
 * Split-face block: the unit is cut after curing so the face fractures, leaving a rough
 * exposed-aggregate texture.
 *
 * <p>Common on plinths, retaining walls and the lower storey of anything that wants to look
 * heavier at the bottom.</p>
 *
 * <p>A block, stairs, slab and fence set, like the colour metal sets beside it. Laid in running
 * bond, and its faces differ: the sides show the coursing and the top shows the units end on with
 * their two hollow cores.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSetCmuSplitface extends AbstractBlockSetBasic {

  /**
   * Constructs a {@link BlockSetCmuSplitface}.
   *
   * @since 1.0
   */
  public BlockSetCmuSplitface() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 255);
  }

  @Override
  public String getBlockRegistryName() {
    return "cmu_splitface";
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

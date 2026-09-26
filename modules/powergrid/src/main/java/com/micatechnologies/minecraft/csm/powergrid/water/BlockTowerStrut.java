package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A water tower's strut: the level member between two legs at a panel point, a block of it at
 * a time along {@link #AXIS} (stored, square to the placing player's view). Where the block at
 * either end is a leg ({@link #LEGL}, {@link #LEGR}, actual state) the strut reaches into it
 * with its gusset plate.
 *
 * @since 2026.9
 */
public class BlockTowerStrut extends AbstractBlock {

  public static final PropertyEnum<EnumFacing.Axis> AXIS = BlockTowerBrace.AXIS;
  public static final PropertyBool LEGL = BlockTowerBrace.LEGL;
  public static final PropertyBool LEGR = BlockTowerBrace.LEGR;

  private static final AxisAlignedBB BOX_X = new AxisAlignedBB(0, 0.34, 0.34, 1, 0.66, 0.66);
  private static final AxisAlignedBB BOX_Z = new AxisAlignedBB(0.34, 0.34, 0, 0.66, 0.66, 1);
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockTowerStrut(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 0, 2F, 6F, 0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, LEGL, LEGR);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing r = BlockTowerBrace.right(state.getValue(AXIS));
    return state.withProperty(LEGL, BlockTowerBrace.leg(world, pos.offset(r.getOpposite())))
        .withProperty(LEGR, BlockTowerBrace.leg(world, pos.offset(r)));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS, (meta & 1) == 0 ? EnumFacing.Axis.X
        : EnumFacing.Axis.Z);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.X ? 0 : 1;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().rotateY().getAxis());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(AXIS) == EnumFacing.Axis.X ? BOX_X : BOX_Z;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
    return BlockRenderLayer.SOLID;
  }
}

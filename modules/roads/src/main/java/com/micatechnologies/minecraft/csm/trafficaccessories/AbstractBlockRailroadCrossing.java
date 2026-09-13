package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The redstone-driven hardware of a railroad crossing: the flasher mast and the gate. Both face
 * one of four ways and carry a {@link #POWERED} state read off adjacent redstone, which is what
 * a real crossing's track circuit delivers -- so a detector rail, or any train mod's detector,
 * runs the crossing with no controller in the loop. Powered, the flashers wig-wag and ring and
 * the gate comes down; unpowered, everything is dark and the gate is up.
 *
 * <p>Facing takes the low two bits of the metadata and the powered flag the third, the way
 * {@code AbstractBlockPoweredSign} packs its own beside eight facings.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public abstract class AbstractBlockRailroadCrossing extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /** Whether the crossing is active: redstone-powered from any side. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  protected AbstractBlockRailroadCrossing() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
    // The superclass set a default state that predates POWERED; make it fully specified.
    setDefaultState(blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState()
        .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(POWERED, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(POWERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState placed =
        super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
    return placed.withProperty(POWERED, worldIn.getRedstonePowerFromNeighbors(pos) > 0);
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    boolean powered = world.getRedstonePowerFromNeighbors(pos) > 0;
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 3);
    }
  }

  /**
   * Whether the crossing at {@code pos} is active. Safe to call for any block.
   *
   * @param world the world
   * @param pos   the position
   *
   * @return {@code true} if a crossing block there is powered
   */
  public static boolean isActive(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() instanceof AbstractBlockRailroadCrossing
        && state.getValue(POWERED);
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      EnumFacing facing) {
    return true;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * No block light: the lamps are lit by the OptiFine emissive companions of their lens
   * strips (the flasher) and by full-bright quads in the renderer (the gate), the RRFB rule.
   *
   * @return always 0
   */
  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return 0;
  }
}

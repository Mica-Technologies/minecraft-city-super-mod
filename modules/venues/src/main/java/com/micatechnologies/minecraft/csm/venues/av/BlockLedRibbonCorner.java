package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A 45 degree piece of LED ribbon board ({@code led_ribbon_board_corner}): the band across its
 * cell on the diagonal, for a bowl's chamfered corner or a run of corners along a 45 degree
 * line. OBJ, drawn as at a bowl's north-west corner, the band facing south-east over the floor.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), as the dasher corner's: the band
 * faces between that facing and the one a quarter turn clockwise from it, so east faces
 * south-east (a bowl's north-west corner), south faces south-west (north-east corner), west
 * faces north-west (south-east corner) and north faces north-east (south-west corner). Bits 2-3
 * the pattern, as the straight's. The band runs from the far side of the cell beside it on the
 * right to the far side of the cell beside it below: a north-west corner at (x, z) meets the
 * straight running along x at (x + 1, z), the straight running along z at (x, z + 1), and the
 * next corner at (x - 1, z + 1).</p>
 *
 * @since 2026.10
 */
public class BlockLedRibbonCorner extends BlockVenue {

  private static final AxisAlignedBB CELL = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  /**
   * Constructs the corner.
   *
   * @param registryName its registry name
   */
  public BlockLedRibbonCorner(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.EAST)
        .withProperty(BlockLedRibbon.PATTERN, RibbonPattern.TEAM_NAVY));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, BlockLedRibbon.PATTERN);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(BlockLedRibbon.PATTERN, RibbonPattern.of(meta >> 2));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex()
        | state.getValue(BlockLedRibbon.PATTERN).ordinal() << 2;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state;
  }

  /** The band faces back at the placer, between two cardinal facings. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    float yaw = MathHelper.wrapDegrees(placer.rotationYaw + 180.0F);
    int quarter = MathHelper.floor((yaw < 0 ? yaw + 360.0F : yaw) / 90.0F) & 3;
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(quarter));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      RibbonPattern next = RibbonPattern.of(state.getValue(BlockLedRibbon.PATTERN).ordinal()
          + 1);
      world.setBlockState(pos, state.withProperty(BlockLedRibbon.PATTERN, next), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.ribbon.pattern",
          new TextComponentTranslation("csm.venues.ribbon.pattern." + next.ordinal())), true);
    }
    return true;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return CELL;
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }
}

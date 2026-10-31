package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import com.micatechnologies.minecraft.csm.venues.arena.TileEntityArenaFloorAnchor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The pitcher's mound's middle ({@code pitchers_mound}): the cell holding the pitcher's rubber
 * on the mound's 5 ft x 34 in table, 10 in (4 px) up. Round it, {@link BlockMoundSlope} cells
 * up to three blocks away show the rest of the mound: the table's front sloping an inch a foot
 * towards home, the sides and back rounding down to the 18 ft circle. The rubber's front is 60 ft
 * 6 in from home plate's point, the mound's middle 59 ft, both in the same cell column as the
 * plate.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the field's, as every ballpark anchor's
 * is: the way its centre line runs from home plate towards centre field (the pitcher throws the
 * other way). Placed by hand it faces the way the placer looks.</p>
 *
 * @since 2026.10
 */
public class BlockPitchersMound extends BlockArenaFloorBase
    implements ICsmTileEntityProvider, IBallparkAnchor {

  /** The way the field's centre line runs from home. Stored, bits 0-1. */
  public static final PropertyDirection FACING = PropertyDirection.create("facing",
      EnumFacing.Plane.HORIZONTAL);

  /**
   * Constructs the mound's middle.
   *
   * @param registryName its registry name
   */
  public BlockPitchersMound(String registryName) {
    super(registryName, Surface.CLAY, null);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING);
  }

  @Override
  public BallparkFamily family() {
    return BallparkLayouts.MOUND;
  }

  @Override
  public int kindIndex(IBlockState state) {
    return 0;
  }

  @Override
  public boolean third() {
    return false;
  }

  @Override
  public EnumFacing pictureFacing(IBlockState state) {
    return state.getValue(FACING);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    int c = BallparkLayouts.MOUND.cellAt(0, 0, 0, false);
    int h = c >= 0 ? BallparkLayouts.MOUND_HEIGHT[c] : 0;
    return new AxisAlignedBB(0, 0, 0, 1, 1 + h / 16.0, 1);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityArenaFloorAnchor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityarenaflooranchor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityArenaFloorAnchor();
  }
}

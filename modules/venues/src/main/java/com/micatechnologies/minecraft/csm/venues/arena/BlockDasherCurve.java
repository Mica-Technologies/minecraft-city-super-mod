package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A cell of a rink's curved corner: dasher board ({@code dasher_board_curve_<corner>}) or its
 * glass ({@code dasher_glass_curve_<corner>}) on a true quarter circle of radius 8 blocks, the
 * corner radius of a 61 x 26 rink. The circle's ice face crosses fifteen cells; each cell draws
 * its stretch (OBJ, cut at the cell faces, reaching a quarter block past them where the board's
 * back does), and the stretches and the straights either side meet without a joint.
 *
 * <p>One block per rink corner ({@code nw}, {@code ne}, {@code se}, {@code sw}); <b>metadata</b>
 * is the cell, 0 to 14, from the end beside the corner's side run to the end beside its end run.
 * {@link DasherShapes#CURVE_CELLS} lists where each cell sits, drawn as the north-west corner:
 * offsets {dx, dz} from the ice's corner cell (the first ice cell in from both the west and the
 * north boards), every other corner turned from it. A sneak-click steps the cell, for a builder
 * placing by hand.</p>
 *
 * @since 2026.10
 */
public class BlockDasherCurve extends BlockDasher {

  /** The cell along the curve. Stored, the whole metadata. */
  public static final PropertyInteger CELL = PropertyInteger.create("cell", 0, 14);

  private static final AxisAlignedBB WHOLE = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  private final EnumFacing corner;

  /**
   * Constructs a curve block.
   *
   * @param registryName its registry name
   * @param glass        glass rather than a board
   * @param corner       which corner: north for the north-west as drawn, east for the north-east,
   *                     south for the south-east, west for the south-west
   */
  public BlockDasherCurve(String registryName, boolean glass, EnumFacing corner) {
    super(registryName, glass);
    this.corner = corner;
    setDefaultState(blockState.getBaseState().withProperty(CELL, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, CELL);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(CELL, Math.min(meta, 14));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(CELL);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState withRotation(@Nonnull IBlockState state, Rotation rot) {
    return state;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState withMirror(@Nonnull IBlockState state, Mirror mirror) {
    return state;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return WHOLE;
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    double[][] all = isGlass() ? DasherShapes.CURVE_GLASS : DasherShapes.CURVE_BOARD;
    BlockDasherCorner.addBoxes(pos, entityBox, boxes, all[state.getValue(CELL)], corner,
        collisionHeight());
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      world.setBlockState(pos, state.withProperty(CELL, (state.getValue(CELL) + 1) % 15), 3);
    }
    return true;
  }
}

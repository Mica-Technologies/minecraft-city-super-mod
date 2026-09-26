package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Residential furniture placed side by side into a run: a sofa, a bookcase, a TV stand, a
 * sideboard. Its end panels (a sofa's arms) are drawn only where the run stops -- {@link #LEFT}
 * and {@link #RIGHT} say whether the same block, facing the same way, continues on that side --
 * so three placed in a row read as one long piece, not three pushed together.
 *
 * <p>Left and right are the sitter's, or the viewer's standing in front, facing
 * {@link #FACING}. Both are actual state, never stored; the multipart blockstate picks the
 * parts from them ({@code gen_furniture_residential.py}).</p>
 *
 * @since 2026.9
 */
public class BlockResidentialRun extends BlockResidentialFurniture {

  /** The run continues to the left. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The run continues to the right. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  /**
   * Constructs a run piece that is not sat on.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param upholstered  whether it is upholstered
   */
  public BlockResidentialRun(String registryName, int[] box, boolean upholstered) {
    super(registryName, box, upholstered);
  }

  /**
   * Constructs a run piece of another material that is not sat on: a restroom's trough sink.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param material     what it is made of
   */
  protected BlockResidentialRun(String registryName, int[] box, FixtureMaterial material) {
    super(registryName, box, material.getMaterial(), material.getSound(),
        material.getHardness());
  }

  /**
   * Constructs a run piece with a seat in every block.
   *
   * @param registryName  its registry name
   * @param box           its box facing north, in sixteenths
   * @param upholstered   whether it is upholstered
   * @param seatTopPx     the top of the seat, in sixteenths
   * @param seatForwardPx how far forward of the middle the seat is, in sixteenths
   */
  public BlockResidentialRun(String registryName, int[] box, boolean upholstered,
      double seatTopPx, double seatForwardPx) {
    super(registryName, box, upholstered, seatTopPx, seatForwardPx, 0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(LEFT, continues(world, pos, facing, facing.rotateYCCW()))
        .withProperty(RIGHT, continues(world, pos, facing, facing.rotateY()));
  }

  /**
   * Whether the run goes on past {@code side}: the same block there, facing the same way.
   *
   * @param world  the world
   * @param pos    this block
   * @param facing the way this block faces
   * @param side   the side looked past
   *
   * @return whether it continues
   */
  protected boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }
}

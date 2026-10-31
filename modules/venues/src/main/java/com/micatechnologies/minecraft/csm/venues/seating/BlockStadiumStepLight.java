package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An aisle step light ({@code stadium_aisle_step_light}): a small LED strip in an aluminium
 * housing on the riser of an aisle step, lighting the tread in front of it. It stands in the air
 * cell over a tread, against the riser of the step behind, and is always lit (its lens carries
 * an OptiFine {@code _e} companion). No collision, so it never trips anyone.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the light shines, down
 * the aisle; bit 2 high, for a full-block riser (the step behind a whole block higher) rather
 * than a stair's half step. Placed by hand it faces whoever placed it and goes high when the
 * block behind is a full block; a sneak-click switches the height.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumStepLight extends BlockVenue {

  /** Mounted high, on a full-block riser. Stored, bit 2. */
  public static final PropertyBool HIGH = PropertyBool.create("high");

  private static final AxisAlignedBB LOW_BOX = BlockStadiumSeat.box(4.5, 2.5, 14.5, 11.5, 6.5,
      16);
  private static final AxisAlignedBB HIGH_BOX = BlockStadiumSeat.box(4.5, 10.5, 14.5, 11.5,
      14.5, 16);

  /**
   * Constructs the step light.
   *
   * @param registryName its registry name
   */
  public BlockStadiumStepLight(String registryName) {
    super(registryName, Material.CIRCUITS, SoundType.METAL, 0.5F);
    setLightLevel(6 / 15.0F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(HIGH, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, HIGH);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(HIGH, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(HIGH) ? 4 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing front = placer.getHorizontalFacing().getOpposite();
    BlockPos behind = pos.offset(front.getOpposite());
    boolean high = world.getBlockState(behind).isFullCube();
    return getDefaultState().withProperty(FACING, front).withProperty(HIGH, high);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(HIGH) ? HIGH_BOX : LOW_BOX;
  }

  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      world.setBlockState(pos, state.withProperty(HIGH, !state.getValue(HIGH)), 3);
    }
    return true;
  }
}

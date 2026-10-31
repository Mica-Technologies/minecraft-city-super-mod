package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A door in the dasher boards: the player bench gate ({@code dasher_bench_gate}), a length of
 * board on hinges, and the penalty box door ({@code dasher_penalty_door}), the same with a glass
 * leaf on it a block higher (the whole door draws in the translucent layer). A click opens or
 * shuts it; it swings a quarter turn away from the ice about its hinge. A board straight beside
 * an open door ends with an end post, so the opening shows its frame.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the ice; bit 2 open;
 * bit 3 the hinge on the right (looking at the ice from behind the board), clear for the
 * left. Two doors side by side with opposite hinges are a double door.</p>
 *
 * @since 2026.10
 */
public class BlockDasherDoor extends BlockDasher {

  /** Open. Stored, bit 2. */
  public static final PropertyBool OPEN = PropertyBool.create("open");
  /** Hinged on the right. Stored, bit 3. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  private static final AxisAlignedBB CLOSED = px(0, 0, 16, 4.25, 0, BOARD_HEIGHT);
  private static final AxisAlignedBB OPEN_LEFT = px(0, 4, 4, 16, 0, BOARD_HEIGHT);
  private static final AxisAlignedBB OPEN_RIGHT = px(12, 4, 16, 16, 0, BOARD_HEIGHT);
  private static final AxisAlignedBB SELECT_CLOSED = px(0, -0.25, 16, 4.25, 0, 1.0);
  private static final AxisAlignedBB SELECT_OPEN_LEFT = px(0, 4, 4.25, 16, 0, 1.0);
  private static final AxisAlignedBB SELECT_OPEN_RIGHT = px(11.75, 4, 16, 16, 0, 1.0);

  private final boolean penalty;

  /**
   * Constructs a door.
   *
   * @param registryName its registry name
   * @param penalty      the penalty box door (with its glass leaf) rather than the bench gate
   */
  public BlockDasherDoor(String registryName, boolean penalty) {
    super(registryName, false);
    this.penalty = penalty;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(OPEN, false).withProperty(RIGHT, false));
  }

  /** Whether it is the penalty box door. */
  public boolean isPenalty() {
    return penalty;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN, RIGHT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(OPEN, (meta & 4) != 0).withProperty(RIGHT, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(OPEN) ? 4 : 0)
        | (state.getValue(RIGHT) ? 8 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state;
  }

  /** Facing the placer, who stands on the ice; hinged on the side of the cell clicked. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = placer.getHorizontalFacing().getOpposite();
    EnumFacing right = f.rotateY();
    double along = (hitX - 0.5) * right.getXOffset() + (hitZ - 0.5) * right.getZOffset();
    return getDefaultState().withProperty(FACING, f).withProperty(RIGHT, along > 0);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (!state.getValue(OPEN)) {
      return SELECT_CLOSED;
    }
    return state.getValue(RIGHT) ? SELECT_OPEN_RIGHT : SELECT_OPEN_LEFT;
  }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    AxisAlignedBB b = !state.getValue(OPEN) ? CLOSED
        : state.getValue(RIGHT) ? OPEN_RIGHT : OPEN_LEFT;
    return RotationUtils.rotateBoundingBoxByFacing(b, state.getValue(FACING));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      world.playSound(null, pos, open ? SoundEvents.BLOCK_FENCE_GATE_OPEN
              : SoundEvents.BLOCK_FENCE_GATE_CLOSE, SoundCategory.BLOCKS, 0.8F,
          world.rand.nextFloat() * 0.1F + 0.9F);
    }
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return penalty ? BlockRenderLayer.TRANSLUCENT
        : BlockRenderLayer.CUTOUT;
  }
}

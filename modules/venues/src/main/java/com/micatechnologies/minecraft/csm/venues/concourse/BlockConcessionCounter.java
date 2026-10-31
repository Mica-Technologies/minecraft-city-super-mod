package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The concession stand's front counter ({@code concession_counter}): a block-high counter with a
 * solid-surface top overhanging a front panel, an LED strip under the lip and an open shelf on
 * the server's side. Side by side the counters join into one run; where it stops a counter draws
 * an end panel ({@code end_l}, {@code end_r}, actual state). An outside corner is
 * {@link BlockConcessionCounterCorner}, which the runs on both its sides join.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the customers' side; bits 2-3
 * the front panel's finish: 0 stainless, 1 navy, 2 crimson, 3 black. A sneak-click with an empty
 * hand steps the finish. The top is a block high, so a card terminal or a register set on it
 * stands in the cell above at its floor.</p>
 *
 * @since 2026.10
 */
public class BlockConcessionCounter extends BlockVenue {

  /** The front panel's finish. */
  public static final PropertyInteger FINISH = PropertyInteger.create("finish", 0, 3);
  /** The run stops on the left (looking the way the counter faces). Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** The run stops on the right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  /**
   * Constructs the counter.
   *
   * @param registryName its registry name
   */
  public BlockConcessionCounter(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(FINISH, 0).withProperty(END_L, true).withProperty(END_R, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, FINISH, END_L, END_R);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(FINISH, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(FINISH) << 2;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(FINISH, (meta >> 2) & 3);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(END_L, !joins(world, pos.offset(f.rotateYCCW()), f, true))
        .withProperty(END_R, !joins(world, pos.offset(f.rotateY()), f, false));
  }

  /**
   * Whether the block at {@code pos}, beside a counter facing {@code facing}, carries its run on:
   * a counter facing the same way, or an outside corner turning it (on the left a corner facing
   * the same way, on the right a corner facing one quarter clockwise).
   */
  static boolean joins(IBlockAccess world, BlockPos pos, EnumFacing facing, boolean left) {
    IBlockState s = world.getBlockState(pos);
    if (s.getBlock() instanceof BlockConcessionCounter) {
      return s.getValue(FACING) == facing;
    }
    if (s.getBlock() instanceof BlockConcessionCounterCorner) {
      return s.getValue(FACING) == (left ? facing : facing.rotateY());
    }
    return false;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    ItemStack held = player.getHeldItem(hand);
    if (!player.isSneaking() || !held.isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int finish = (state.getValue(FINISH) + 1) & 3;
      world.setBlockState(pos, state.withProperty(FINISH, finish), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.counter.finish",
          new TextComponentTranslation("csm.venues.counter.finish." + finish)), true);
    }
    return true;
  }
}

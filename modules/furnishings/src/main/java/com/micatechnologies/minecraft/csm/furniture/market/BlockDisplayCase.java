package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.ISwitchable;
import com.micatechnologies.minecraft.csm.furniture.residential.LampSwitching;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A refrigerated display a block high: the island freezer, the ice cream dipping cabinet, the
 * deli and bakery service cases, drawn by {@code gen_furniture_market.py}. Cases of the same
 * block set side by side join into one long case, the end glass and panels only where it stops
 * (a {@link BlockResidentialStorage} run). Each block holds 27 slots behind the refrigerator's
 * door sounds; the stock seen through the glass is part of the model, not what it holds.
 *
 * <p>Its lights are on when placed ({@link #LIT}), and it gives light while they are; a
 * sneak-free click on the case opens it, so the lights are switched by redstone
 * ({@link LampSwitching}: a change of power switches them) or by clicking it with an empty hand
 * while sneaking. Either switches the case and the cases joined to it in its line, up to
 * {@link DisplayLine#REACH}, so one light switch, linked or beside it, works a row. {@link #LIT} is
 * stored in the bit above the facing, {@link LampSwitching#POWERED} in the top bit. The glass is
 * a translucent texture, so the case draws in the translucent layer.</p>
 *
 * @since 2026.9
 */
public class BlockDisplayCase extends BlockResidentialStorage implements ISwitchable {

  /** Whether its lights are on. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  /** The light the case gives while lit. */
  public static final int LIGHT = 10;

  /**
   * Constructs a display case.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockDisplayCase(String registryName, int[] box) {
    super(registryName, box, 27, FurnishingsSounds.FRIDGE_OPEN, FurnishingsSounds.FRIDGE_CLOSE);
    setDefaultState(getDefaultState().withProperty(LIT, true)
        .withProperty(LampSwitching.POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT, LIT, LampSwitching.POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(LIT, (meta & 4) != 0)
        .withProperty(LampSwitching.POWERED, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(LIT) ? 4 : 0)
        | (state.getValue(LampSwitching.POWERED) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIT, true).withProperty(LampSwitching.POWERED, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? LIGHT : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  /** A click opens the case; a sneaking click with an empty hand switches its lights. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      if (!player.getHeldItem(hand).isEmpty()) {
        return false;
      }
      if (!world.isRemote) {
        boolean lit = !state.getValue(LIT);
        setLineLit(world, pos, state, lit);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
            0.3F, lit ? 0.6F : 0.5F);
      }
      return true;
    }
    return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
  }

  /**
   * A change of redstone power switches the lights of its line: on when power comes, off when it
   * goes ({@link LampSwitching}).
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(LampSwitching.POWERED)) {
      IBlockState remembered = state.withProperty(LampSwitching.POWERED, powered);
      world.setBlockState(pos, remembered, 3);
      setLineLit(world, pos, remembered, powered);
    }
  }

  /**
   * Switches the lights of the case at {@code pos} and of the cases it reaches in its line
   * ({@link DisplayLine}). The others keep their own record of power, so switching them sets off
   * nothing further.
   */
  private void setLineLit(World world, BlockPos pos, IBlockState state, boolean lit) {
    EnumFacing facing = state.getValue(FACING);
    for (BlockPos p : DisplayLine.reach(pos, facing, q -> {
      IBlockState s = world.getBlockState(q);
      return s.getBlock() == this && s.getValue(FACING) == facing;
    })) {
      IBlockState s = p.equals(pos) ? state : world.getBlockState(p);
      if (s.getValue(LIT) != lit) {
        world.setBlockState(p, s.withProperty(LIT, lit), 3);
      }
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}

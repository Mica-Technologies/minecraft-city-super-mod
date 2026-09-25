package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCloset;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialRun;
import com.micatechnologies.minecraft.csm.furniture.residential.ISwitchable;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A refrigerated display two blocks tall -- the glass-door reach-in cooler and freezer, the open
 * multideck dairy case -- drawn by {@code gen_furniture_market.py}. Placed and broken as one
 * piece ({@code BlockResidentialTall}), and set side by side it joins into one line of doors
 * with the end panels only where it stops ({@link BlockCloset}). The lower half holds 27 slots
 * behind the refrigerator's door sounds; the drinks, ice cream or milk seen on its shelves are
 * part of the model, not what it holds.
 *
 * <p>Its lights ({@link BlockDisplayCase#LIT}) are on when placed and both halves give light
 * while they are. A click opens it; a sneaking click with an empty hand switches the lights, and
 * so does a change of redstone power at either half, as a lamp's does. Either switches the
 * cooler and the coolers joined to it in its line, up to {@link DisplayLine#REACH} doors, so one
 * light switch works a row and a long row takes more than one. {@code LIT} is stored in both halves, in the bit above
 * {@code UPPER}; whether it was powered is kept by the lower half's
 * {@link TileEntityDisplayCase}, the metadata being full. The glass is translucent, so it draws
 * in the translucent layer.</p>
 *
 * @since 2026.9
 */
public class BlockDisplayCooler extends BlockCloset implements ISwitchable {

  /**
   * Constructs a two-block display.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths from the floor (up to 32)
   */
  public BlockDisplayCooler(String registryName, int[] box) {
    super(registryName, box, 27, FurnishingsSounds.FRIDGE_OPEN, FurnishingsSounds.FRIDGE_CLOSE);
    setDefaultState(getDefaultState().withProperty(BlockDisplayCase.LIT, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UPPER, BlockResidentialRun.LEFT,
        BlockResidentialRun.RIGHT, BlockDisplayCase.LIT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 7).withProperty(BlockDisplayCase.LIT, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(BlockDisplayCase.LIT) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(BlockDisplayCase.LIT, true);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityDisplayCase) {
      ((TileEntityDisplayCase) te).setPowered(isPowered(world, pos));
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(BlockDisplayCase.LIT) ? BlockDisplayCase.LIGHT : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  /** A click opens it; a sneaking click with an empty hand switches its lights. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      if (!player.getHeldItem(hand).isEmpty()) {
        return false;
      }
      if (!world.isRemote) {
        BlockPos lower = state.getValue(UPPER) ? pos.down() : pos;
        boolean lit = !state.getValue(BlockDisplayCase.LIT);
        setLineLit(world, lower, lit);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
            0.3F, lit ? 0.6F : 0.5F);
      }
      return true;
    }
    return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
  }

  /**
   * A change of redstone power at either half switches the lights of its line: on when power
   * comes, off when it goes.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    BlockPos lower = state.getValue(UPPER) ? pos.down() : pos;
    TileEntity te = world.getTileEntity(lower);
    if (!(te instanceof TileEntityDisplayCase)) {
      return;
    }
    TileEntityDisplayCase memory = (TileEntityDisplayCase) te;
    boolean powered = isPowered(world, lower);
    if (powered != memory.wasPowered()) {
      memory.setPowered(powered);
      setLineLit(world, lower, powered);
    }
  }

  private boolean isPowered(World world, BlockPos lower) {
    return world.isBlockPowered(lower) || world.isBlockPowered(lower.up());
  }

  /**
   * Switches the lights of the cooler at {@code lower} and of the coolers it reaches in its line
   * ({@link DisplayLine}). The others keep their own record of power, so switching them sets off
   * nothing further.
   */
  private void setLineLit(World world, BlockPos lower, boolean lit) {
    IBlockState base = world.getBlockState(lower);
    if (base.getBlock() != this) {
      return;
    }
    EnumFacing facing = base.getValue(FACING);
    for (BlockPos door : DisplayLine.reach(lower, facing, p -> {
      IBlockState s = world.getBlockState(p);
      return s.getBlock() == this && s.getValue(FACING) == facing && !s.getValue(UPPER);
    })) {
      setLit(world, door, lit);
    }
  }

  /** Switches both halves' lights. */
  private void setLit(World world, BlockPos lower, boolean lit) {
    for (BlockPos half : new BlockPos[]{lower, lower.up()}) {
      IBlockState s = world.getBlockState(half);
      if (s.getBlock() == this && s.getValue(BlockDisplayCase.LIT) != lit) {
        world.setBlockState(half, s.withProperty(BlockDisplayCase.LIT, lit), 3);
      }
    }
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDisplayCase.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydisplaycase";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return (meta & 4) != 0 ? null : new TileEntityDisplayCase(27);
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}

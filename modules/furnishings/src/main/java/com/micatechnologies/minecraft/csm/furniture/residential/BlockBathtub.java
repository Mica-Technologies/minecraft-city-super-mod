package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A built-in bathtub, two blocks long, its back and one end against the walls: placed as one
 * piece by its own item (which refuses unless both blocks are free) and broken as one from
 * either. The block it is placed in is the tap end ({@link #HEAD} false); the other,
 * {@code facing.rotateY()} of it, is the head end, where the bather leans back.
 *
 * <p>It holds water, drawn as a surface in the tub ({@link #WATER}, stored in both blocks):</p>
 * <ul>
 *   <li>a water bucket pours in and fills it; an empty bucket scoops a full tub's water out;</li>
 *   <li>an empty hand on the tap end runs the tap to fill it (with the spray's sound), or pulls
 *   the plug on a full one;</li>
 *   <li>a glass bottle fills at the tap, as at a sink;</li>
 *   <li>an empty hand on the head end sits in the bath, on Core's seat.</li>
 * </ul>
 *
 * @since 2026.9
 */
public class BlockBathtub extends BlockResidentialFurniture implements IWaterTap {

  /** Whether this is the head end, away from the tap. */
  public static final PropertyBool HEAD = PropertyBool.create("head");
  /** Whether the tub is full. */
  public static final PropertyBool WATER = PropertyBool.create("water");

  private static final int[] BOX = {0, 0, 3, 16, 9, 16};
  /** Where the bather sits: on the tub's floor, leaning back on the head end. */
  private static final double SEAT_TOP = 3.0;
  /** How far from the head end's middle towards the taps the bather sits, in blocks. */
  private static final double SEAT_IN = 0.05;

  /**
   * Constructs a bathtub.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockBathtub(String registryName) {
    super(registryName, BOX, FixtureMaterial.PORCELAIN.getMaterial(),
        FixtureMaterial.PORCELAIN.getSound(), FixtureMaterial.PORCELAIN.getHardness(), SEAT_TOP,
        0, 0);
    setDefaultState(getDefaultState().withProperty(HEAD, false).withProperty(WATER, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, HEAD, WATER);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(HEAD, (meta & 4) != 0)
        .withProperty(WATER, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(HEAD) ? 4 : 0)
        | (state.getValue(WATER) ? 8 : 0);
  }

  /** The other end of the tub whose end {@code state} is at {@code pos}. */
  private static BlockPos otherEnd(BlockPos pos, IBlockState state) {
    EnumFacing along = state.getValue(FACING).rotateY();
    return state.getValue(HEAD) ? pos.offset(along.getOpposite()) : pos.offset(along);
  }

  /** Whether {@code pos} is the matching other end of the tub at {@code state}. */
  private boolean isOtherEnd(World world, BlockPos pos, IBlockState state) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == this && other.getValue(FACING) == state.getValue(FACING)
        && other.getValue(HEAD) != state.getValue(HEAD);
  }

  // --- placing and breaking as one piece ---------------------------------------------------

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemBathtub(this);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(HEAD, false).withProperty(WATER, false);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    world.setBlockState(otherEnd(pos, state), state.withProperty(HEAD, true), 3);
  }

  /** A creative player breaking the head end takes the tap end with it, dropping nothing. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(HEAD) && player.capabilities.isCreativeMode) {
      BlockPos tap = otherEnd(pos, state);
      if (isOtherEnd(world, tap, state)) {
        world.setBlockToAir(tap);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /** The other end goes with this one; only the tap end drops the tub. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    BlockPos other = otherEnd(pos, state);
    if (isOtherEnd(world, other, state)) {
      if (state.getValue(HEAD)) {
        world.destroyBlock(other, true);
      } else {
        world.setBlockToAir(other);
      }
    }
    super.breakBlock(world, pos, state);
  }

  /** A head end whose tap end has gone (a command) goes too. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (state.getValue(HEAD) && !isOtherEnd(world, otherEnd(pos, state), state)) {
      world.setBlockToAir(pos);
    }
    super.neighborChanged(state, world, pos, block, fromPos);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(HEAD) ? Items.AIR : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  // --- water and bathing -----------------------------------------------------------------

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    ItemStack held = player.getHeldItem(hand);
    boolean full = state.getValue(WATER);
    if (held.getItem() == Items.WATER_BUCKET) {
      if (full) {
        return false;
      }
      if (!world.isRemote) {
        IWaterTap.give(player, hand, held, new ItemStack(Items.BUCKET));
        setWater(world, pos, state, true, SoundEvents.ITEM_BUCKET_EMPTY);
      }
      return true;
    }
    if (held.getItem() == Items.BUCKET && full) {
      if (!world.isRemote) {
        IWaterTap.give(player, hand, held, new ItemStack(Items.WATER_BUCKET));
        setWater(world, pos, state, false, SoundEvents.ITEM_BUCKET_FILL);
      }
      return true;
    }
    if (IWaterTap.fillAtTap(world, pos, player, hand)) {
      return true;
    }
    if (!held.isEmpty()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    if (state.getValue(HEAD)) {
      return sit(world, pos, state, player);
    }
    SoundEvent run = FurnishingsSounds.SHOWER_SPRAY.getSoundEvent();
    setWater(world, pos, state, !full, full ? SoundEvents.ITEM_BUCKET_EMPTY : run);
    return true;
  }

  /** Fills or empties both ends, with a sound. */
  private void setWater(World world, BlockPos pos, IBlockState state, boolean water,
      SoundEvent sound) {
    world.setBlockState(pos, state.withProperty(WATER, water), 3);
    BlockPos other = otherEnd(pos, state);
    if (isOtherEnd(world, other, state)) {
      IBlockState o = world.getBlockState(other);
      world.setBlockState(other, o.withProperty(WATER, water), 3);
    }
    if (sound != null) {
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.8F, water ? 0.8F : 1.0F);
    }
  }

  /**
   * Seats the bather in the head end, leaning back on it and looking along the tub to the
   * taps, and lets them out into the room in front of the tub.
   */
  @Override
  protected boolean sit(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
    EnumFacing front = state.getValue(FACING);
    EnumFacing toTap = front.rotateYCCW();
    double sx = pos.getX() + 0.5 + toTap.getXOffset() * SEAT_IN + front.getXOffset() * 0.1;
    double sz = pos.getZ() + 0.5 + toTap.getZOffset() * SEAT_IN + front.getZOffset() * 0.1;
    BlockPos out = pos.offset(front);
    EntityCsmSeat.sit(world, pos, new AxisAlignedBB(pos), "csm.furnishings.seat.taken", sx,
        pos.getY() + 0.1, sz, SEAT_TOP / 16.0 - 0.22, toTap, out.getX() + 0.5, out.getY(),
        out.getZ() + 0.5, player);
    return true;
  }

  /**
   * The bathtub's item: places the whole tub, and only where both of its blocks are free.
   */
  public static class ItemBathtub extends ItemBlock {

    /**
     * Constructs the item.
     *
     * @param block the bathtub
     */
    public ItemBathtub(BlockBathtub block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY,
        float hitZ, @Nonnull IBlockState newState) {
      BlockPos other = pos.offset(newState.getValue(FACING).rotateY());
      if (!world.getBlockState(other).getBlock().isReplaceable(world, other)
          || !player.canPlayerEdit(other, side, stack)) {
        return false;
      }
      return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
  }
}

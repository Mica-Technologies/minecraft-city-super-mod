package com.micatechnologies.minecraft.csm.healthcare;

import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A piece two blocks big, both cells of one block: the patient headwall (two wide) and the
 * wardrobe (two tall). {@link #SECOND} marks the second cell; it is the first flag, so bit 2 of a
 * floor piece's metadata and bit 3 of a wall piece's.
 *
 * <p>A {@link Shape#WIDE wide} piece's second cell is to the right of the first as you face it
 * ({@code first.offset(facing.rotateYCCW())}); a {@link Shape#TALL tall} piece's is above it. The
 * item places both; a generator places each cell with {@code /setblock}, in either order, and
 * nothing removes a cell whose partner is missing. Breaking either cell breaks both, and only the
 * first drops the item.</p>
 *
 * @since 2026.10
 */
public class BlockHealthcarePair extends BlockHealthcare {

  /** Which way the second cell lies from the first. */
  public enum Shape {
    /** To the right, as you face the piece. */
    WIDE,
    /** Above. */
    TALL
  }

  /** The second cell. */
  public static final PropertyBool SECOND = PropertyBool.create("second");

  private static final PropertyBool[] FLAGS = {SECOND};

  private final Shape shape;

  /**
   * Constructs a two-cell piece.
   *
   * @param registryName its registry name
   * @param box          each cell's box facing north, in sixteenths
   * @param material     what it is made of
   * @param mount        floor or wall
   * @param shape        which way the second cell lies
   */
  public BlockHealthcarePair(String registryName, int[] box, HealthcareMaterial material,
      Mount mount, Shape shape) {
    super(registryName, box, material, mount, true);
    this.shape = shape;
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  /**
   * Where the other cell of the cell at {@code pos} is.
   *
   * @param pos   a cell
   * @param state its state
   *
   * @return the partner's position
   */
  public BlockPos partner(BlockPos pos, IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    boolean second = state.getValue(SECOND);
    if (shape == Shape.TALL) {
      return second ? pos.down() : pos.up();
    }
    EnumFacing right = facing.rotateYCCW();
    return second ? pos.offset(right.getOpposite()) : pos.offset(right);
  }

  private boolean isPartner(World world, BlockPos pos, IBlockState state) {
    BlockPos p = partner(pos, state);
    IBlockState o = world.getBlockState(p);
    return o.getBlock() == this && o.getValue(FACING) == state.getValue(FACING)
        && o.getValue(SECOND) != state.getValue(SECOND);
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemPair(this);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    IBlockState first = state.withProperty(SECOND, false);
    world.setBlockState(partner(pos, first), first.withProperty(SECOND, true), 3);
  }

  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(SECOND) && player.capabilities.isCreativeMode
        && isPartner(world, pos, state)) {
      world.setBlockToAir(partner(pos, state));
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (isPartner(world, pos, state)) {
      if (state.getValue(SECOND)) {
        world.destroyBlock(partner(pos, state), true);
      } else {
        world.setBlockToAir(partner(pos, state));
      }
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(SECOND) ? Items.AIR : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  /** The item: places both cells, and only where the second is free. */
  public static class ItemPair extends ItemBlock {

    public ItemPair(BlockHealthcarePair block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      BlockHealthcarePair pair = (BlockHealthcarePair) block;
      BlockPos other = pair.partner(pos, newState.withProperty(SECOND, false));
      if (other.getY() >= world.getHeight()
          || !world.getBlockState(other).getBlock().isReplaceable(world, other)
          || !player.canPlayerEdit(other, side, stack)) {
        return false;
      }
      return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ,
          newState.withProperty(SECOND, false));
    }
  }
}

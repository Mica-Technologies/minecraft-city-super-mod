package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBookcase;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
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
 * Gondola shelving: a block of store shelving, a pegboard back at +Z and two shelves of stock in
 * front of it, drawn by {@code gen_furniture_market.py}. It joins and stacks as the bookcase does
 * -- end uprights only where a run stops ({@link #LEFT}, {@link #RIGHT}), the base deck only at
 * the bottom of a stack and the top cap only at its head ({@link #UP}, {@link #DOWN}) -- but
 * with any gondola, whatever it is stocked with, so tins beside cereal over snacks read as one
 * run of shelving. Two runs set back to back make an island gondola, stocked on both sides. The
 * end caps are gondolas too, drawn deeper with a header over them, so an end cap joins and stacks
 * with the end caps beside it (and closes a run turned the other way).
 *
 * <p>A click with an empty hand hangs sale tags on its shelf edges, a second adds a shelf
 * talker, a third takes both off again ({@link #TAGS}, stored in the two bits above the facing);
 * a sneaking click steps back. A click holding anything is left alone, so blocks can still be
 * placed against the shelving.</p>
 *
 * <p>Some stock (bottles, spray triggers) is a cutout, so it draws in the cutout layer. The
 * stock is part of the model: the shelving holds nothing.</p>
 *
 * @since 2026.9
 */
public class BlockGondola extends BlockBookcase {

  /** What hangs on its shelf edges. */
  public static final PropertyEnum<ShelfTags> TAGS = PropertyEnum.create("tags", ShelfTags.class);

  /**
   * Constructs a gondola.
   *
   * @param registryName its registry name, ending in what it is stocked with
   * @param box          its box facing north, in sixteenths
   */
  public BlockGondola(String registryName, int[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(TAGS, ShelfTags.NONE));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT, UP, DOWN, TAGS);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(TAGS, ShelfTags.byOrdinal(meta >> 2));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(TAGS).ordinal() << 2);
  }

  @Override
  protected boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() instanceof BlockGondola && other.getValue(FACING) == facing;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      ShelfTags next = state.getValue(TAGS).step(player.isSneaking());
      world.setBlockState(pos, state.withProperty(TAGS, next), 3);
      world.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS,
          0.5F, next == ShelfTags.NONE ? 0.8F : 1.2F);
    }
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}

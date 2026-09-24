package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A wall fitting that folds down on a right-click and up on the next: the restroom's baby
 * changing station, folded flat against the wall until it is wanted. {@link #OPEN} is stored,
 * in the bit above the facing, and picks the model and the box.
 *
 * @since 2026.9
 */
public class BlockFoldingFixture extends BlockBathroomFixture {

  /** Whether it is folded down. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  private final AxisAlignedBB openBox;

  /**
   * Constructs a folding fitting.
   *
   * @param registryName its registry name, ending in its finish
   * @param closedBox    its box folded up, facing north, in sixteenths
   * @param openBox      its box folded down, facing north, in sixteenths
   * @param material     what it is made of
   */
  public BlockFoldingFixture(String registryName, int[] closedBox, int[] openBox,
      FixtureMaterial material) {
    super(registryName, closedBox, material);
    this.openBox = new AxisAlignedBB(openBox[0] / 16.0, openBox[1] / 16.0, openBox[2] / 16.0,
        openBox[3] / 16.0, openBox[4] / 16.0, openBox[5] / 16.0);
    setDefaultState(getDefaultState().withProperty(OPEN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OPEN, false);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(OPEN) ? openBox : super.getBlockBoundingBox(state, source, pos);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      world.playSound(null, pos, open ? SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN
              : SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.7F,
          open ? 1.1F : 1.2F);
    }
    return true;
  }
}

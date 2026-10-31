package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
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
 * A concession stand's countertop machine: the hot dog roller grill, the popcorn machine, the
 * pretzel warmer, the nacho station (chip warmer and cheese dispenser) and the soft-serve
 * machine. Each stands at the floor of its cell, so it sits on a block-high counter (the
 * concession counters, or anything full height).
 *
 * <p>They are scenery that runs: a click switches one on or off ({@code on}, metadata bit 2).
 * On, its moving parts are animated textures (the rollers turn, the kettle pops, the cheese
 * pump's light glows) and it gives a little light; nothing ticks. The machines that work in
 * Furnishings run on its appliance framework, which is not Core's, so these do not cook.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), its front (the side the
 * customers see); bit 2 on. Placed by hand it is on.</p>
 *
 * @since 2026.10
 */
public class BlockConcessionAppliance extends BlockVenue {

  /** Which machine. */
  public enum Kind {
    /** The hot dog roller grill: a sneeze guard over turning rollers. */
    HOT_DOG_ROLLER(BlockConcourseRun.box(1, 0, 2.5, 15, 8.5, 13.5), 6),
    /** The popcorn machine: a glass cabinet with a kettle under a lit roof. */
    POPCORN(BlockConcourseRun.box(1.5, 0, 2, 14.5, 15.5, 14), 10),
    /** The pretzel warmer: a lit glass case with pretzels on two shelves. */
    PRETZEL_WARMER(BlockConcourseRun.box(2, 0, 3, 14, 13, 13), 8),
    /** The nacho station: a chip warmer and a cheese pump dispenser. */
    NACHO(BlockConcourseRun.box(1, 0, 3, 15, 12.5, 13.5), 6),
    /** The soft-serve machine: a countertop twin-twist freezer with three handles. */
    SOFT_SERVE(BlockConcourseRun.box(2, 0, 2, 14, 15, 14), 4);

    final AxisAlignedBB box;
    final int light;

    Kind(AxisAlignedBB box, int light) {
      this.box = box;
      this.light = light;
    }
  }

  /** Switched on. Stored, bit 2. */
  public static final PropertyBool ON = PropertyBool.create("on");

  private final Kind kind;

  /**
   * Constructs a machine.
   *
   * @param registryName its registry name
   * @param kind         which machine
   */
  public BlockConcessionAppliance(String registryName, Kind kind) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    this.kind = kind;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(ON, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, ON);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(ON, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(ON) ? 4 : 0);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return kind.box;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(ON) ? kind.light : 0;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    // the glass of the cases and the sneeze guard
    return BlockRenderLayer.TRANSLUCENT;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (!world.isRemote) {
      boolean on = !state.getValue(ON);
      world.setBlockState(pos, state.withProperty(ON, on), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4F,
          on ? 1.2F : 0.9F);
    }
    return true;
  }
}

package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
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
 * The concession counter's outside corner ({@code concession_counter_corner}): a counter with a
 * front on two sides, the side it faces and the one a quarter turn to its left (looking the way
 * it faces), so at a stand's north-west corner it faces north. The straight counters on both its
 * sides carry their run into it and draw no end panel.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), bits 2-3 the finish as the
 * straight counter's. A sneak-click with an empty hand steps the finish.</p>
 *
 * @since 2026.10
 */
public class BlockConcessionCounterCorner extends BlockVenue {

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  /**
   * Constructs the corner.
   *
   * @param registryName its registry name
   */
  public BlockConcessionCounterCorner(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(BlockConcessionCounter.FINISH, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, BlockConcessionCounter.FINISH);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(BlockConcessionCounter.FINISH, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex()
        | state.getValue(BlockConcessionCounter.FINISH) << 2;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
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
      int finish = (state.getValue(BlockConcessionCounter.FINISH) + 1) & 3;
      world.setBlockState(pos, state.withProperty(BlockConcessionCounter.FINISH, finish), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.counter.finish",
          new TextComponentTranslation("csm.venues.counter.finish." + finish)), true);
    }
    return true;
  }
}

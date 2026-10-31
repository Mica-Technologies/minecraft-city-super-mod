package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A stadium light tower's head ({@code stadium_light_head}, five floodlights across and four
 * rows up; {@code stadium_light_head_small}, three across and two up): banks of LED floodlights
 * on a steel frame, aimed down at the field, on top of a {@link BlockLightPole} shaft. A
 * {@link BlockVenueUnit} drawn whole by its master, the bottom middle cell, which sits on the
 * shaft's top.
 *
 * <p><b>Light.</b> Lit, the master gives light level 15 and the lenses are drawn bright (with an
 * OptiFine {@code _e} glow), so the head reads lit at night. It does not light the field below:
 * Lighting's light-up air projection is Lighting's alone, and Sports &amp; Venues depends on Core
 * only, so a field under a 30-block tower needs its own light (Lighting's fixtures, or glowstone
 * under the turf) if it must be bright.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the floodlights aim;
 * bit 2 lit. A click switches it. Placed by hand it is lit and aims at the placer.</p>
 *
 * @since 2026.10
 */
public class BlockLightHead extends BlockVenueUnit {

  /** Switched on. Stored, bit 2. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  /**
   * Constructs a head.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockLightHead(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(LIT, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LIT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LIT, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIT, true);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? 15 : 0;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      boolean lit = !state.getValue(LIT);
      world.setBlockState(pos, state.withProperty(LIT, lit), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5F,
          lit ? 1.1F : 0.8F);
    }
    return true;
  }
}

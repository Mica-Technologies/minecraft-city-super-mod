package com.micatechnologies.minecraft.csm.venues.bowling;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The end of a pair of lanes: the pinsetter cabinet over the pit, the black curtain behind the
 * pins, the kickbacks beside the pin decks and, over the decks, the masking unit with its lit
 * graphic. A {@link BlockVenueUnit} four blocks wide (both lanes of a pair); its model faces the
 * bowlers. The master is the cabinet's bottom cell over the back pin deck block of the lane on
 * the bowlers' right; the cabinet reaches two blocks back and the masking panel hangs a block
 * over the front pin deck blocks, so the pins under it stay free.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the bowlers; bits 2-3 the
 * masking graphic (0 starburst, 1 cosmic, 2 retro, 3 neon waves). A sneak-click with an empty hand
 * steps the graphic. The master gives a little light, as the panel's lamps do.</p>
 *
 * @since 2026.10
 */
public class BlockBowlingPinsetter extends BlockVenueUnit {

  /** How many masking graphics. */
  public static final int DESIGNS = 4;
  /** The masking graphic. Stored. */
  public static final PropertyInteger DESIGN = PropertyInteger.create("design", 0, DESIGNS - 1);

  /**
   * Constructs the pinsetter.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockBowlingPinsetter(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(DESIGN, 0));
    setLightLevel(0.5F);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, DESIGN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(DESIGN, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(DESIGN) << 2;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking() || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int d = (state.getValue(DESIGN) + 1) % DESIGNS;
      world.setBlockState(pos, state.withProperty(DESIGN, d), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.bowling.masking",
          new TextComponentTranslation("csm.venues.bowling.masking." + d)), true);
    }
    return true;
  }
}

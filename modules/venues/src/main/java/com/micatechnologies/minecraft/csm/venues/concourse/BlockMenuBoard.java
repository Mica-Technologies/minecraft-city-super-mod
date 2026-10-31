package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The concession stand's digital menu board ({@code concession_menu_board}): a screen three
 * blocks wide and two tall flat on the wall behind the counter, its menu baked into the chunk
 * mesh (one 256 px picture per menu), so it costs what a painting does. A {@link BlockVenueUnit}
 * whose master is the bottom middle cell; the screen is on the back of its cells, against the
 * wall.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the reader; bits 2-3 the
 * menu: 0 hot dogs and the grill, 1 beer and drinks, 2 pizza and snacks, 3 sweets. A sneak-click
 * with an empty hand steps the menu. For a menu of your own words, Core's wayfinding panel is
 * the concession sign ({@link BlockConcessionSign}), four lines set in its editor.</p>
 *
 * @since 2026.10
 */
public class BlockMenuBoard extends BlockVenueUnit {

  /** The menu shown. Stored, bits 2-3. */
  public static final PropertyInteger LAYOUT = PropertyInteger.create("layout", 0, 3);

  /**
   * Constructs the board.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockMenuBoard(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(LAYOUT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LAYOUT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LAYOUT, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(LAYOUT) << 2;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    ItemStack held = player.getHeldItem(hand);
    if (!player.isSneaking() || !held.isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int layout = (state.getValue(LAYOUT) + 1) & 3;
      world.setBlockState(pos, state.withProperty(LAYOUT, layout), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.menu_board.layout",
          new TextComponentTranslation("csm.venues.menu_board.layout." + layout)), true);
    }
    return true;
  }
}

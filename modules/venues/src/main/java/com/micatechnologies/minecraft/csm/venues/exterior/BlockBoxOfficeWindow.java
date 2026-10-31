package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The box office window ({@code box_office_window}): one ticket window two blocks tall, set into
 * the wall of an arena's front. Its lower block is the panel under the window and the counter
 * ledge (with the deal tray); its upper block the glass with a stainless speaking grille, and
 * over it a lit header band whose legend is stored: BOX OFFICE, WILL CALL, TICKETS or blank. A
 * {@link BlockVenueUnit} whose master is the lower block, the upper one an invisible part.
 *
 * <p>Windows side by side facing the same way are a run: each draws its own bay and the run
 * draws its end frames only where it stops ({@code end_l}, {@code end_r}, actual state). For one
 * header across a whole run, put a concession sign ({@code concession_sign}, Core's wayfinding
 * panel, with BOX OFFICE, WILL CALL and TICKETS among its presets) over the windows instead and
 * set the windows blank.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the customer outside;
 * bits 2-3 the header: 0 BOX OFFICE, 1 WILL CALL, 2 TICKETS, 3 blank. A sneak-click with an empty
 * hand steps the header.</p>
 *
 * @since 2026.10
 */
public class BlockBoxOfficeWindow extends BlockVenueUnit {

  /** The header's legend. Stored, bits 2-3. */
  public static final PropertyInteger LEGEND = PropertyInteger.create("legend", 0, 3);
  /** The run stops on the left (looking the way the window faces). Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** The run stops on the right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  /**
   * Constructs the window.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockBoxOfficeWindow(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(LEGEND, 0).withProperty(END_L, true).withProperty(END_R, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEGEND, END_L, END_R);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LEGEND, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(LEGEND) << 2;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(END_L, !same(world, pos.offset(f.rotateYCCW()), f))
        .withProperty(END_R, !same(world, pos.offset(f.rotateY()), f));
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LEGEND) == 3 ? 0 : 7;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    ItemStack held = player.getHeldItem(hand);
    if (hand != EnumHand.MAIN_HAND || !player.isSneaking() || !held.isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int legend = (state.getValue(LEGEND) + 1) & 3;
      world.setBlockState(pos, state.withProperty(LEGEND, legend), 3);
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.box_office.legend",
          new TextComponentTranslation("csm.venues.box_office.legend." + legend)), true);
    }
    return true;
  }
}

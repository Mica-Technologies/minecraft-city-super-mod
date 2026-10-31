package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The exterior marquee board's controller ({@code marquee_board_controller}): a board cell like
 * any other ({@link BlockMarqueeBoard}) that also holds what the board shows, in its tile entity
 * ({@link TileEntityMarquee}), and draws it across the whole board
 * ({@link TileEntityMarqueeRenderer}). Put it at the board's <b>bottom-left</b> cell as the reader
 * sees it (on a double-sided board, as the front's reader sees it); the board is as wide as the
 * cells running to its right, as tall as the cells running up from it.
 *
 * <p><b>Metadata</b>: as a cell's, bits 0-1 the facing and bit 2 double-sided. A click opens the
 * text editor: three lines, the mode (text and video in turn, text only, video only), the text and
 * background colours. {@code /blockdata} sets the same ({@link TileEntityMarquee}).</p>
 *
 * @since 2026.10
 */
public class BlockMarqueeController extends BlockMarqueeBoard implements ICsmTileEntityProvider {

  /**
   * Constructs the controller.
   *
   * @param registryName its registry name
   */
  public BlockMarqueeController(String registryName) {
    super(registryName);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      VenuesGuiProvider.open(player, world, pos, VenuesGuiProvider.TEXT_GUI_ID);
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityMarquee.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityvenuemarquee";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityMarquee();
  }
}

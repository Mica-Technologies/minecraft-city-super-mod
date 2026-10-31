package com.micatechnologies.minecraft.csm.venues.courts;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import com.micatechnologies.minecraft.csm.venues.VenuesSounds;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A courtside scoreboard ({@code court_scoreboard}: a board three blocks wide and a block and a
 * half tall on two posts, the master the left post's foot, looking at the board) or a track's lap
 * counter ({@code track_lap_counter}: a two-digit flip counter and a bell on a stand, two blocks
 * tall). The casing is baked; the scores and the laps are {@link TileEntityCourtBoardRenderer}'s.
 *
 * <p>A click opens the venue text editor; on the lap counter a click turns it down a lap and a
 * sneak-click with an empty hand opens the editor. Metadata is the facing, towards the reader.</p>
 *
 * @since 2026.10
 */
public class BlockCourtBoard extends BlockVenueUnit implements ICsmTileEntityProvider {

  private final boolean lapCounter;

  /**
   * Constructs a board.
   *
   * @param registryName its registry name
   * @param lapCounter   the lap counter rather than the scoreboard
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockCourtBoard(String registryName, boolean lapCounter, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    this.lapCounter = lapCounter;
  }

  /** The lap counter rather than the scoreboard. */
  public boolean isLapCounter() {
    return lapCounter;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityCourtBoard)) {
      return false;
    }
    boolean editor = !lapCounter || (player.isSneaking() && player.getHeldItem(hand).isEmpty());
    if (editor) {
      if (world.isRemote) {
        VenuesGuiProvider.open(player, world, pos, VenuesGuiProvider.TEXT_GUI_ID);
      }
      return true;
    }
    if (!world.isRemote) {
      int left = ((TileEntityCourtBoard) te).lapDown();
      if (left == 1) {
        world.playSound(null, pos.up(), VenuesSounds.TRACK_LAP_BELL.getSoundEvent(),
            SoundCategory.BLOCKS, 2.0F, 1.0F);
      }
      player.sendStatusMessage(new TextComponentTranslation("csm.venues.track.laps", left), true);
    }
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityCourtBoard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitycourtboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityCourtBoard();
  }
}

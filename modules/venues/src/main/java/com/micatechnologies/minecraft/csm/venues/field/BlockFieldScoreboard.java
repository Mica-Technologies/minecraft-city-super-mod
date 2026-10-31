package com.micatechnologies.minecraft.csm.venues.field;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.IVenueUnitListener;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboard;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * A football or soccer scoreboard ({@code football_scoreboard}, {@code soccer_scoreboard}): a
 * board 12 blocks wide and 5 tall on two legs 3 tall, the ballpark line score's casing. A
 * {@link BlockVenueUnit}: the master is the left leg's foot, looking at the board (it reaches a
 * block to the master's left and ten to its right). The casing is baked; what the board shows is
 * {@link TileEntityFieldScoreboardRenderer}'s.
 *
 * <p>A click opens the venue text editor; a sneak-click with an empty hand starts or stops the
 * clock, and so does a rising redstone edge at any of its cells. Metadata is the facing, towards
 * the reader.</p>
 *
 * @since 2026.10
 */
public class BlockFieldScoreboard extends BlockVenueUnit implements ICsmTileEntityProvider,
    IVenueUnitListener {

  private final boolean soccer;

  /**
   * Constructs a board.
   *
   * @param registryName its registry name
   * @param soccer       the soccer board rather than the football one
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockFieldScoreboard(String registryName, boolean soccer, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    this.soccer = soccer;
  }

  /** The soccer board rather than the football one. */
  public boolean isSoccer() {
    return soccer;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityFieldScoreboard)) {
      return false;
    }
    if (player.isSneaking() && player.getHeldItem(hand).isEmpty()) {
      if (!world.isRemote) {
        TileEntityFieldScoreboard board = (TileEntityFieldScoreboard) te;
        boolean running = board.toggleClock();
        player.sendStatusMessage(running
            ? new TextComponentTranslation("csm.venues.scoreboard.running")
            : new TextComponentTranslation("csm.venues.scoreboard.stopped",
                board.clockText(world.getTotalWorldTime(), 0)), true);
      }
      return true;
    }
    if (world.isRemote) {
      VenuesGuiProvider.open(player, world, pos, VenuesGuiProvider.TEXT_GUI_ID);
    }
    return true;
  }

  @Override
  public void onUnitNeighborChanged(World world, BlockPos master, IBlockState state,
      BlockPos cell) {
    TileEntity te = world.getTileEntity(master);
    if (te instanceof TileEntityFieldScoreboard) {
      ((TileEntityFieldScoreboard) te).redstone(world.isBlockPowered(cell));
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    if (!world.isRemote) {
      onUnitNeighborChanged(world, pos, state, pos);
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFieldScoreboard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfieldscoreboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityFieldScoreboard();
  }

  /** The clock as the board shows it, for a message. */
  static String clock(int tenths, boolean soccer) {
    if (soccer) {
      int s = Math.max(0, tenths) / 10;
      return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
    return TileEntityScoreboard.clockText(tenths);
  }
}

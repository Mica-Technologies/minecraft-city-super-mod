package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A ballpark scoreboard: the line score board ({@code ballpark_scoreboard}, 12 blocks wide and 5
 * tall on two legs 3 tall: the teams' runs inning by inning, R H E, the count's ball, strike and
 * out lamps and the pitch speed) or the count board ({@code ballpark_count_board}, 3 x 2 on a
 * wall or fence: the count and the pitch speed). A {@link BlockVenueUnit}: the line score's
 * master is the left leg's foot (the board reaches a block to its left and ten to its right,
 * looking at the board), the count board's its bottom left cell. The casing is baked; what the
 * board shows is {@link TileEntityBallparkScoreboardRenderer}'s.
 *
 * <p>A click anywhere on the board opens its editor (the venue text editor: the teams, each
 * team's runs by inning as numbers between spaces, hits and errors, the count, the pitch speed,
 * the colours and who is batting); {@code /blockdata} on the master sets the same
 * ({@link TileEntityBallparkScoreboard}). Metadata is the facing, towards the reader.</p>
 *
 * @since 2026.10
 */
public class BlockBallparkScoreboard extends BlockVenueUnit implements ICsmTileEntityProvider {

  private final boolean small;

  /**
   * Constructs a board.
   *
   * @param registryName its registry name
   * @param small        the count board rather than the line score
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockBallparkScoreboard(String registryName, boolean small, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    this.small = small;
  }

  /** The count board rather than the line score. */
  public boolean isSmall() {
    return small;
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
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBallparkScoreboard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityballparkscoreboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBallparkScoreboard();
  }
}

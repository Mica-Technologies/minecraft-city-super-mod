package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.IVenueUnitListener;
import com.micatechnologies.minecraft.csm.venues.VenuesGuiProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The centre-hung scoreboard ({@code centre_hung_scoreboard}, {@code _small}): four screens, two
 * long and two short, between an LED ring above and one below, hung from the truss or ceiling
 * over the middle of the floor. A hung {@link BlockVenueUnit}: its master is the middle cell of
 * the bottom layer, its parts the shell's cells ({@link ScoreboardSize}), and its baked OBJ
 * draws the casing, the rings and the dark screens. What the screens show is
 * {@link TileEntityScoreboardRenderer}'s.
 *
 * <p>A click anywhere on the board opens its screen editor; a sneak-click with an empty hand
 * starts or stops the clock, as does a redstone pulse at any of its cells. Metadata is the
 * facing: the long faces look north and south at facing north or south.</p>
 *
 * @since 2026.10
 */
public class BlockScoreboard extends BlockVenueUnit implements ICsmTileEntityProvider,
    IVenueUnitListener {

  private final ScoreboardSize size;

  /**
   * Constructs a scoreboard.
   *
   * @param registryName its registry name
   * @param size         its size
   */
  public BlockScoreboard(String registryName, ScoreboardSize size) {
    super(registryName, true, BlockVenueUnit.cellsOf(size.boxes), size.boxes);
    this.size = size;
  }

  /** The board's size. */
  public ScoreboardSize getSize() {
    return size;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityScoreboard)) {
      return false;
    }
    if (player.isSneaking() && player.getHeldItem(hand).isEmpty()) {
      if (!world.isRemote) {
        TileEntityScoreboard board = (TileEntityScoreboard) te;
        boolean running = board.toggleClock();
        player.sendStatusMessage(running
            ? new TextComponentTranslation("csm.venues.scoreboard.running")
            : new TextComponentTranslation("csm.venues.scoreboard.stopped",
                TileEntityScoreboard.clockText(board.remaining(world.getTotalWorldTime(), 0))),
            true);
      }
      return true;
    }
    if (world.isRemote) {
      VenuesGuiProvider.open(player, world, pos, VenuesGuiProvider.SCOREBOARD_GUI_ID);
    }
    return true;
  }

  @Override
  public void onUnitNeighborChanged(World world, BlockPos master, IBlockState state,
      BlockPos cell) {
    TileEntity te = world.getTileEntity(master);
    if (te instanceof TileEntityScoreboard) {
      ((TileEntityScoreboard) te).redstone(world.isBlockPowered(cell));
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
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityScoreboard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityvenuescoreboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityScoreboard();
  }
}

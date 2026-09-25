package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

/**
 * A bus departure display: either the station's departure board, a slim landscape monitor that
 * lists every bus leaving the stops around it (route, destination, bay, minutes), or the bay
 * display, a small amber LED sign hung over one bay that lists that stop's next buses as the
 * arrival display on its post does. Both read the stops they can see ({@link BusStation}) and the
 * buses' shared timetable ({@code BusDepartures}), so they agree with the stops' route plates
 * and every arrival display's countdown.
 *
 * <p>The display sits against the back of its block (the model's z = 16): on a wall, or with
 * none behind it, on two rods to the block above ({@link #HUNG}, actual state), unless another
 * display of the same block is there. Departure boards facing the same way side by side and
 * stacked are a bank, as the flight information boards are, and each shows the page after the
 * one to its reader's left or above ({@link TileEntityBusDepartureBoard}).</p>
 *
 * <p><b>Setting a board up.</b> A click steps which agency the board lists: every agency, then
 * CITYLINE, RIVERWAY, VERDANT and EMBERLINE. A sneaking click turns spoken announcements on or
 * off. Either is set on the whole bank at once. The bay display has nothing to set.</p>
 *
 * @since 2026.9
 */
public class BlockBusBoard extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /** Nothing behind to hang on, and no display of this block above: drawn on rods. */
  public static final PropertyBool HUNG = PropertyBool.create("hung");

  /** The most boards a click sets at once. */
  private static final int MAX_BANK = 64;

  private final boolean bay;

  /**
   * Constructs a display.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param bay          true for the bay display, false for the departure board
   */
  public BlockBusBoard(String registryName, double[] box, boolean bay) {
    super(registryName, box, false, bay ? 2 : 3);
    this.bay = bay;
    setDefaultState(getDefaultState().withProperty(HUNG, false));
  }

  /**
   * Whether this is the bay display rather than the departure board.
   *
   * @return true for the bay display
   */
  public boolean isBay() {
    return bay;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, HUNG);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    BlockPos behind = pos.offset(facing.getOpposite());
    boolean wall = world.getBlockState(behind).getBlockFaceShape(world, behind, facing)
        == BlockFaceShape.SOLID;
    boolean above = world.getBlockState(pos.up()).getBlock() == this;
    return state.withProperty(HUNG, !wall && !above);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (bay) {
      return false;
    }
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      return true;
    }
    TileEntity here = world.getTileEntity(pos);
    if (!(here instanceof TileEntityBusDepartureBoard)) {
      return true;
    }
    TileEntityBusDepartureBoard board = (TileEntityBusDepartureBoard) here;
    List<TileEntityBusDepartureBoard> bank = bank(world, pos, state.getValue(FACING));
    if (player.isSneaking()) {
      boolean announce = !board.isAnnouncing();
      for (TileEntityBusDepartureBoard b : bank) {
        b.setAnnouncing(announce);
      }
      String key = !announce ? "csm.transit.board.announce_off"
          : Loader.isModLoaded("csm_tts") ? "csm.transit.board.announce_on"
              : "csm.transit.board.announce_no_tts";
      player.sendStatusMessage(new TextComponentTranslation(key), true);
    } else {
      int filter = (board.getFilter() + 1) % BusStation.FILTERS;
      for (TileEntityBusDepartureBoard b : bank) {
        b.setFilter(filter);
      }
      player.sendStatusMessage(filter == 0
          ? new TextComponentTranslation("csm.transit.board.filter_all")
          : new TextComponentTranslation("csm.transit.board.filter",
              TileEntityBusDepartureBoard.agencyTitle(filter)), true);
    }
    world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
        0.3F, 0.8F);
    return true;
  }

  /**
   * The boards of a bank: this block, facing the same way, joined side by side or stacked to
   * the one clicked, at most {@link #MAX_BANK}.
   */
  private List<TileEntityBusDepartureBoard> bank(World world, BlockPos start,
      EnumFacing facing) {
    EnumFacing left = facing.rotateY();
    EnumFacing[] steps = {left, left.getOpposite(), EnumFacing.UP, EnumFacing.DOWN};
    List<TileEntityBusDepartureBoard> out = new ArrayList<>();
    Set<BlockPos> seen = new HashSet<>();
    ArrayDeque<BlockPos> open = new ArrayDeque<>();
    open.add(start);
    seen.add(start);
    while (!open.isEmpty() && out.size() < MAX_BANK) {
      BlockPos p = open.poll();
      TileEntity te = world.getTileEntity(p);
      if (te instanceof TileEntityBusDepartureBoard) {
        out.add((TileEntityBusDepartureBoard) te);
      }
      for (EnumFacing step : steps) {
        BlockPos q = p.offset(step);
        if (seen.contains(q) || !world.isBlockLoaded(q)) {
          continue;
        }
        IBlockState other = world.getBlockState(q);
        if (other.getBlock() == this && other.getValue(FACING) == facing) {
          seen.add(q);
          open.add(q);
        }
      }
    }
    return out;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return bay ? TileEntityBusBayDisplay.class : TileEntityBusDepartureBoard.class;
  }

  @Override
  public String getTileEntityName() {
    return bay ? "tileentitybusbaydisplay" : "tileentitybusdepartureboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return bay ? new TileEntityBusBayDisplay() : new TileEntityBusDepartureBoard();
  }
}

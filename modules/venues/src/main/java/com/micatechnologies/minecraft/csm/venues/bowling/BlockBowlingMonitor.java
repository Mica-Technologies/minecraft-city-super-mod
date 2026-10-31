package com.micatechnologies.minecraft.csm.venues.bowling;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
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
 * The overhead scoring monitors a pair of lanes shares: two screens side by side on a bar, hung
 * over the approaches facing the settee, each showing its lane's bowlers frame by frame. A hung
 * {@link BlockVenueUnit} four blocks wide and two tall; the casing is baked, what the screens show
 * is {@link TileEntityBowlingMonitorRenderer}'s. The master is the bottom cell on the bowlers'
 * right (looking at the screens).
 *
 * <p>A click opens the venue text editor: a line a bowler, three a lane (see
 * {@link TileEntityBowlingMonitor}). Metadata is the facing (S 0, W 1, N 2, E 3), towards the
 * reader.</p>
 *
 * @since 2026.10
 */
public class BlockBowlingMonitor extends BlockVenueUnit implements ICsmTileEntityProvider {

  /**
   * Constructs the monitors.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockBowlingMonitor(String registryName, int[] cells, int[] boxes) {
    super(registryName, true, cells, boxes);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!(world.getTileEntity(pos) instanceof TileEntityBowlingMonitor)) {
      return false;
    }
    if (world.isRemote) {
      VenuesGuiProvider.open(player, world, pos, VenuesGuiProvider.TEXT_GUI_ID);
    }
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBowlingMonitor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybowlingmonitor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBowlingMonitor();
  }
}

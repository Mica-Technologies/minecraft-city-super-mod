package com.micatechnologies.minecraft.csm.healthcare.nursecall;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.healthcare.clinical.BlockNurseStation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The nurses' station counter with monitors ({@code nurse_station_counter_monitors}), which shows
 * the nurse calls of the master panel it is linked to: while any of the panel's rooms calls, its
 * two charting screens give way to a flashing CALL screen with the panel's six room numbers, rooms
 * 1-3 on the left screen and 4-6 on the right, each lit while that room calls.
 *
 * <p>It is a counter like the others (it joins them, and its ends are actual state) with the
 * master panel's room lamps added: its tile entity's {@code rooms} byte, one bit a room as the
 * panel's, read into the actual state ({@link BlockNurseCallMasterPanel#ROOM}) for the baked
 * multipart model. Nothing ticks and nothing is drawn by a renderer: the network sets the byte when
 * a call starts or ends, as it sets the panel's, and the CALL band flashes by an animated
 * texture. A monitor is linked to one panel ({@code /csmhealthcare nursecall link <panel>
 * <monitor>}, or by hand: sneak-click the panel, then the monitor) and any number of monitors to a
 * panel; it takes no room on the panel.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing, as every counter piece. A click lists the calling
 * rooms, as the panel's does.</p>
 *
 * @since 2026.10
 */
public class BlockNurseStationMonitors extends BlockNurseStation implements
    ICsmTileEntityProvider {

  private static final IProperty<?>[] ACTUAL = actual();

  private static IProperty<?>[] actual() {
    IProperty<?>[] out = new IProperty<?>[2 + BlockNurseCallMasterPanel.ROOMS];
    out[0] = EAST;
    out[1] = WEST;
    System.arraycopy(BlockNurseCallMasterPanel.ROOM, 0, out, 2, BlockNurseCallMasterPanel.ROOMS);
    return out;
  }

  /**
   * Constructs the counter with monitors.
   *
   * @param box its box facing north, in sixteenths
   */
  public BlockNurseStationMonitors(int[] box) {
    super("nurse_station_counter_monitors", box, false);
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    state = super.defaultExtras(state);
    for (PropertyBool p : BlockNurseCallMasterPanel.ROOM) {
      state = state.withProperty(p, false);
    }
    return state;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    TileEntity te = world.getTileEntity(pos);
    int rooms = te instanceof TileEntityNurseStationMonitor
        ? ((TileEntityNurseStationMonitor) te).getRooms() : 0;
    for (int i = 0; i < BlockNurseCallMasterPanel.ROOMS; i++) {
      s = s.withProperty(BlockNurseCallMasterPanel.ROOM[i], (rooms >> i & 1) != 0);
    }
    return s;
  }

  /**
   * A click lists the calling rooms of the linked panel; a sneak-click links the monitor to the
   * panel picked by sneak-clicking one.
   *
   * @since 2026.10
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    if (player.isSneaking()) {
      NurseCallLinking.sneakClick(world, pos, NurseCallNetwork.Kind.MONITOR, player);
      return true;
    }
    NurseCallNetwork net = NurseCallNetwork.get(world);
    BlockPos panel = net.panelOfMonitor(pos);
    if (panel == null) {
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.healthcare.nursecall.monitor_unlinked"), true);
      return true;
    }
    List<Long> rooms = net.childrenOf(panel.toLong());
    List<String> calling = new ArrayList<>();
    for (int i = 0; i < rooms.size() && i < BlockNurseCallMasterPanel.ROOMS; i++) {
      if (net.roomCalling(rooms.get(i))) {
        calling.add(String.valueOf(i + 1));
      }
    }
    if (calling.isEmpty()) {
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.healthcare.nursecall.panel_quiet", rooms.size()), true);
    } else {
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.healthcare.nursecall.panel_calling", String.join(", ", calling)), true);
    }
    return true;
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      NurseCallNetwork.get(world).refresh(world, pos);
    }
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityNurseStationMonitor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitynursestationmonitor";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityNurseStationMonitor();
  }
}

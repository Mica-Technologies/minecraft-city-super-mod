package com.micatechnologies.minecraft.csm.healthcare.nursecall;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
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
 * The nurses' station master panel ({@code nurse_call_master_panel}): six numbered room lamps,
 * lit while that room calls, and a chime when a new call comes in. Rooms are the dome lights
 * (or lone devices) linked to it, numbered in the order they were linked; more rooms take more
 * panels, side by side.
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5). The room
 * lamps are its tile entity's {@code rooms} byte (one bit a room, NBT), read into the actual
 * state for the baked model; nothing ticks. While any room calls it gives a redstone signal and
 * its alarm lamp is lit. A click lists the calling rooms; a sneak-click picks the panel to link dome
 * lights and devices to.</p>
 *
 * @since 2026.10
 */
public class BlockNurseCallMasterPanel extends BlockHealthcare implements ICsmTileEntityProvider {

  /** How many rooms one panel shows. */
  public static final int ROOMS = 6;

  /** The room lamps, actual state from the tile entity. */
  public static final PropertyBool[] ROOM = new PropertyBool[ROOMS];

  static {
    for (int i = 0; i < ROOMS; i++) {
      ROOM[i] = PropertyBool.create("room" + (i + 1));
    }
  }

  public BlockNurseCallMasterPanel() {
    super("nurse_call_master_panel", new int[]{1, 2, 14, 15, 14, 16}, HealthcareMaterial.PLASTIC,
        Mount.WALL, true);
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ROOM;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    for (PropertyBool p : ROOM) {
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
    int rooms = te instanceof TileEntityNurseCallPanel ? ((TileEntityNurseCallPanel) te)
        .getRooms() : 0;
    for (int i = 0; i < ROOMS; i++) {
      s = s.withProperty(ROOM[i], (rooms >> i & 1) != 0);
    }
    return s;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return true;
    }
    if (player.isSneaking()) {
      NurseCallLinking.sneakClick(world, pos, NurseCallNetwork.Kind.PANEL, player);
      return true;
    }
    NurseCallNetwork net = NurseCallNetwork.get(world);
    List<Long> rooms = net.childrenOf(pos.toLong());
    List<String> calling = new ArrayList<>();
    for (int i = 0; i < rooms.size(); i++) {
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
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityNurseCallPanel && ((TileEntityNurseCallPanel) te).getRooms()
        != 0 ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityNurseCallPanel.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitynursecallpanel";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityNurseCallPanel();
  }
}

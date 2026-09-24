package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Streetscape tab's screens: the utility box number editor, the parking meter screen and
 * the mailbox compartment screen, the one with a server-side container.
 *
 * @version 1.0
 */
public class StreetscapeGuiProvider implements ICsmGuiProvider {

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (BlockMailbox.isMailboxGui(id)) {
      ContainerMailbox container = mailboxContainer(id, player, world, pos, false);
      if (container == null) {
        return null;
      }
      BlockMailbox block = (BlockMailbox) world.getBlockState(pos).getBlock();
      String title = block.getLocalizedName();
      if (block.getCompartmentCount() > 1) {
        title += " -- " + block.getCompartmentName(container.getCompartment());
      }
      return new GuiMailbox(player.inventory, container, title);
    }
    if (id == BlockParkingMeter.GUI_ID) {
      TileEntity te = world.getTileEntity(pos);
      Block block = world.getBlockState(pos).getBlock();
      if (te instanceof TileEntityParkingMeter && block instanceof BlockParkingMeter) {
        return new ParkingMeterGui((TileEntityParkingMeter) te,
            ((BlockParkingMeter) block).getKind() == BlockParkingMeter.Kind.STATION);
      }
      return null;
    }
    if (id != BlockUtilityBoxLabelled.GUI_ID) {
      return null;
    }
    TileEntity te = world.getTileEntity(pos);
    Block block = world.getBlockState(pos).getBlock();
    if (!(te instanceof TileEntityUtilityBoxLabel) || !(block instanceof BlockUtilityBoxLabelled)) {
      return null;
    }
    UtilityBoxSpec.Label label = ((BlockUtilityBoxLabelled) block).getSpec().getLabel();
    int lines = label != null ? label.getLines() : 1;
    return new UtilityBoxLabelGui((TileEntityUtilityBoxLabel) te, lines);
  }

  @Nullable
  @Override
  public Object getServerGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    return BlockMailbox.isMailboxGui(id) ? mailboxContainer(id, player, world, pos, true) : null;
  }

  /** The container a mailbox GUI id names, or null if the box or compartment is not there. */
  @Nullable
  private static ContainerMailbox mailboxContainer(int id, EntityPlayer player, World world,
      BlockPos pos, boolean server) {
    TileEntity te = world.getTileEntity(pos);
    Block block = world.getBlockState(pos).getBlock();
    if (!(te instanceof TileEntityMailbox) || !(block instanceof BlockMailbox)) {
      return null;
    }
    BlockMailbox mailbox = (BlockMailbox) block;
    int i = BlockMailbox.guiCompartment(id);
    if (i >= mailbox.getCompartmentCount()) {
      return null;
    }
    return new ContainerMailbox(player.inventory, (TileEntityMailbox) te, i,
        BlockMailbox.guiMode(id), mailbox.getSlots(i), server);
  }
}

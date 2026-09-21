package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The signage module's screens, handed to Core's GUI handler through {@code CsmGuiRegistry}.
 */
public class SignageGuiProvider implements ICsmGuiProvider {

  /** An advertising board's screen. GUI ids are global across the mod; 0 to 29 were taken. */
  public static final int AD_BOARD_GUI_ID = 30;

  /** Opens the board screen for the board {@code pos} is a block of. Client side. */
  static void open(EntityPlayer player, World world, BlockPos pos) {
    player.openGui(Csm.instance, AD_BOARD_GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
  }

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id != AD_BOARD_GUI_ID) {
      return null;
    }
    BlockPos controller = AdBoards.findController(world, pos);
    TileEntity te = controller == null ? null : world.getTileEntity(controller);
    return te instanceof TileEntityAdBoard ? new GuiAdBoard((TileEntityAdBoard) te, pos) : null;
  }
}

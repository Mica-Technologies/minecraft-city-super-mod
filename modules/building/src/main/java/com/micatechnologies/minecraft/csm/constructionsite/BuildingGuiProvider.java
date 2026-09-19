package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.buildingmaterials.ContainerDoorWorkshop;
import com.micatechnologies.minecraft.csm.buildingmaterials.GuiDoorWorkshop;
import com.micatechnologies.minecraft.csm.buildingmaterials.GuiGarageKeypad;
import com.micatechnologies.minecraft.csm.buildingmaterials.TileEntityDoorWorkshop;
import com.micatechnologies.minecraft.csm.buildingmaterials.TileEntityGarageDoorControl;
import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The building module's GUIs, handed to Core's GUI handler through {@code CsmGuiRegistry}.
 *
 * @version 1.0
 * @since 2026.9
 */
public class BuildingGuiProvider implements ICsmGuiProvider {

  /**
   * The crane configuration screen. GUI ids are global across the mod; 0 to 24 were taken.
   *
   * @since 1.0
   */
  public static final int CRANE_GUI_ID = 25;

  /**
   * The garage door keypad's PIN screen.
   *
   * @since 1.0
   */
  public static final int KEYPAD_GUI_ID = 26;

  /**
   * The Door Workshop, the one screen here with a server-side container (its slots).
   *
   * @since 1.0
   */
  public static final int DOOR_WORKSHOP_GUI_ID = 27;

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id == DOOR_WORKSHOP_GUI_ID) {
      TileEntity te = world.getTileEntity(pos);
      return te instanceof TileEntityDoorWorkshop
          ? new GuiDoorWorkshop(player.inventory, (TileEntityDoorWorkshop) te) : null;
    }
    if (id == KEYPAD_GUI_ID) {
      TileEntity te = world.getTileEntity(pos);
      return te instanceof TileEntityGarageDoorControl
          ? new GuiGarageKeypad((TileEntityGarageDoorControl) te, pos, player) : null;
    }
    if (id != CRANE_GUI_ID) {
      return null;
    }
    TileEntityCraneHead head = CraneLocator.findHead(world, pos);
    return head == null ? null : new CraneHeadGui(head, pos);
  }

  @Nullable
  @Override
  public Object getServerGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id != DOOR_WORKSHOP_GUI_ID) {
      return null;
    }
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityDoorWorkshop
        ? new ContainerDoorWorkshop(player.inventory, (TileEntityDoorWorkshop) te) : null;
  }
}

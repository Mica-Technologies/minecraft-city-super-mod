package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
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

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id != CRANE_GUI_ID) {
      return null;
    }
    TileEntityCraneHead head = CraneLocator.findHead(world, pos);
    return head == null ? null : new CraneHeadGui(head, pos);
  }
}

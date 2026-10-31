package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import com.micatechnologies.minecraft.csm.venues.av.GuiScoreboard;
import com.micatechnologies.minecraft.csm.venues.av.GuiVenueText;
import com.micatechnologies.minecraft.csm.venues.av.IVenueTextHolder;
import com.micatechnologies.minecraft.csm.venues.av.TileEntityScoreboard;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The venues module's screens, handed to Core's GUI handler through {@code CsmGuiRegistry}: the
 * scoreboard's editor and the text editor the ribbon board and the banners share. GUI ids are
 * global across the mod; 0 to 61 were taken.
 *
 * @since 2026.10
 */
public class VenuesGuiProvider implements ICsmGuiProvider {

  /** The centre-hung scoreboard's editor. */
  public static final int SCOREBOARD_GUI_ID = 70;
  /** A ribbon board's or a banner's text. */
  public static final int TEXT_GUI_ID = 71;

  /**
   * Opens a screen for the block at {@code pos} (a unit's master). Client side.
   *
   * @param player the player
   * @param world  the world
   * @param pos    the block
   * @param id     the screen
   */
  public static void open(EntityPlayer player, World world, BlockPos pos, int id) {
    player.openGui(Csm.instance, id, world, pos.getX(), pos.getY(), pos.getZ());
  }

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (id == SCOREBOARD_GUI_ID && te instanceof TileEntityScoreboard) {
      return new GuiScoreboard((TileEntityScoreboard) te);
    }
    if (id == TEXT_GUI_ID && te instanceof IVenueTextHolder) {
      return new GuiVenueText((IVenueTextHolder) te, pos,
          world.getBlockState(pos).getBlock().getLocalizedName());
    }
    return null;
  }
}

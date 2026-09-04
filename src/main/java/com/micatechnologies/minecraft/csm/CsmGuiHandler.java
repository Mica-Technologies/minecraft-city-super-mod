package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.codeutils.gui.CsmGuiRegistry;
import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * The GUI handler for the City Super Mod.
 * <p>
 * This is the one handler Forge knows about, and every {@code openGui} call site keeps using it
 * with the same GUI id it always has. The screens themselves come from the
 * {@link ICsmGuiProvider}s registered with {@link CsmGuiRegistry} by the part of the mod that
 * owns them, so this class names no subsystem.
 *
 * @version 1.0
 * @since 2023.2.1
 */
public class CsmGuiHandler implements IGuiHandler {

  /**
   * Returns a server-side GUI container to be displayed to the user.
   *
   * @param id     The GUI ID Number
   * @param player The player viewing the GUI
   * @param world  The current world
   * @param x      X Position
   * @param y      Y Position
   * @param z      Z Position
   *
   * @return A server-side GUI container to be displayed to the user, null if none.
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
    return CsmGuiRegistry.findServerGuiElement(id, player, world, new BlockPos(x, y, z));
  }

  /**
   * Returns a client-side GUI container to be displayed to the user.
   *
   * @param id     The GUI ID Number
   * @param player The player viewing the GUI
   * @param world  The current world
   * @param x      X Position
   * @param y      Y Position
   * @param z      Z Position
   *
   * @return A client-side GUI container to be displayed to the user, null if none.
   *
   * @since 1.0
   */
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
    return CsmGuiRegistry.findClientGuiElement(id, player, world, new BlockPos(x, y, z));
  }
}

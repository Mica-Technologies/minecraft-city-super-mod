package com.micatechnologies.minecraft.csm.codeutils.gui;

import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A source of GUI screens for one part of the mod.
 * <p>
 * Core owns the single {@code IGuiHandler} that Forge knows about; each part of the mod
 * contributes its own branch of what used to be one long if/else chain by registering a provider
 * with {@link CsmGuiRegistry}. GUI ids are unique across the mod and are unchanged by this
 * split, so a provider simply declines (returns {@code null}) for any id that is not its own.
 * <p>
 * As with the handler this replaces, {@link #getClientGuiElement} returns client GUI objects and
 * is only ever invoked on the client; the server side has no containers and returns {@code null}
 * for everything.
 *
 * @version 1.0
 * @since 2026.9
 */
public interface ICsmGuiProvider {

  /**
   * Returns the client-side GUI to display for the given id, or {@code null} if this provider
   * does not handle the id (or its guard on the block/tile entity at the position fails).
   *
   * @param id     the GUI id number
   * @param player the player viewing the GUI
   * @param world  the current world
   * @param pos    the position the GUI was opened at
   *
   * @return the client-side GUI to display, or {@code null} if none
   *
   * @since 1.0
   */
  @Nullable
  Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos);

  /**
   * Returns the server-side GUI container for the given id, or {@code null} if this provider does
   * not handle the id. No part of the mod uses a server-side container today, so the default
   * declines everything.
   *
   * @param id     the GUI id number
   * @param player the player viewing the GUI
   * @param world  the current world
   * @param pos    the position the GUI was opened at
   *
   * @return the server-side GUI container to display, or {@code null} if none
   *
   * @since 1.0
   */
  @Nullable
  default Object getServerGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    return null;
  }
}

package com.micatechnologies.minecraft.csm.codeutils.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The registry of {@link ICsmGuiProvider}s that Core's GUI handler asks, in registration order,
 * for the screen belonging to a GUI id.
 * <p>
 * Providers register from a mod's pre-initialization. Forge runs every mod's pre-initialization
 * before any initialization, and Core registers the GUI handler in its initialization, so every
 * provider is present before a GUI can be opened.
 * <p>
 * Registration order does not matter: ids are unique across the mod, and the one id that two
 * providers share is disambiguated by their own block/tile entity guards, so at most one provider
 * ever returns a non-null result for a given position.
 *
 * @version 1.0
 * @since 2026.9
 */
public final class CsmGuiRegistry {

  /**
   * The registered providers, in registration order.
   *
   * @since 1.0
   */
  private static final List<ICsmGuiProvider> PROVIDERS = new ArrayList<>();

  /**
   * Private constructor: this class is a static registry and is never instantiated.
   *
   * @since 1.0
   */
  private CsmGuiRegistry() {
    throw new UnsupportedOperationException("CsmGuiRegistry must not be instantiated");
  }

  /**
   * Registers a GUI provider. Call this from a mod's pre-initialization.
   *
   * @param provider the provider to register
   *
   * @since 1.0
   */
  public static void register(ICsmGuiProvider provider) {
    if (provider != null && !PROVIDERS.contains(provider)) {
      PROVIDERS.add(provider);
    }
  }

  /**
   * Returns the registered providers, in registration order.
   *
   * @return the registered providers
   *
   * @since 1.0
   */
  public static List<ICsmGuiProvider> getProviders() {
    return Collections.unmodifiableList(PROVIDERS);
  }

  /**
   * Asks each registered provider, in registration order, for the client-side GUI belonging to
   * the given id, and returns the first non-null answer.
   *
   * @param id     the GUI id number
   * @param player the player viewing the GUI
   * @param world  the current world
   * @param pos    the position the GUI was opened at
   *
   * @return the client-side GUI to display, or {@code null} if no provider handles the id
   *
   * @since 1.0
   */
  @Nullable
  public static Object findClientGuiElement(int id, EntityPlayer player, World world,
      BlockPos pos) {
    for (ICsmGuiProvider provider : PROVIDERS) {
      Object gui = provider.getClientGuiElement(id, player, world, pos);
      if (gui != null) {
        return gui;
      }
    }
    return null;
  }

  /**
   * Asks each registered provider, in registration order, for the server-side GUI container
   * belonging to the given id, and returns the first non-null answer.
   *
   * @param id     the GUI id number
   * @param player the player viewing the GUI
   * @param world  the current world
   * @param pos    the position the GUI was opened at
   *
   * @return the server-side GUI container to display, or {@code null} if no provider handles the
   *     id
   *
   * @since 1.0
   */
  @Nullable
  public static Object findServerGuiElement(int id, EntityPlayer player, World world,
      BlockPos pos) {
    for (ICsmGuiProvider provider : PROVIDERS) {
      Object gui = provider.getServerGuiElement(id, player, world, pos);
      if (gui != null) {
        return gui;
      }
    }
    return null;
  }
}

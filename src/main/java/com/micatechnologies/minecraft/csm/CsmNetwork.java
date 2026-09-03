package com.micatechnologies.minecraft.csm;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * A network channel for the City Super Mod. Wraps one {@link SimpleNetworkWrapper}, assigns
 * message discriminators from its own counter, and provides the send helpers used across the
 * mod.
 * <p>
 * There is one instance per mod container: Core owns {@link #CORE} (channel {@code csm}) and
 * each module owns a channel named after its mod id. Discriminators are per channel, so a
 * module's packet ids depend only on the order that module registers them in its own
 * {@code preInit} — never on which other modules are installed or on the order Forge loads
 * them in. That order is deterministic per channel, so client and server always agree.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class CsmNetwork {

  /**
   * The maximum length of a network channel name. Vanilla's custom payload packet writes the
   * channel name as a string capped at this many characters, so a longer name would be rejected
   * on the wire.
   *
   * @since 2026.9
   */
  public static final int MAX_CHANNEL_NAME_LENGTH = 20;

  /**
   * The network channel owned by Core, carrying Core's own packets. Modules must not register
   * on it; they create their own channel instead.
   *
   * @since 2026.9
   */
  public static final CsmNetwork CORE = create(CsmConstants.MOD_NAMESPACE);

  /**
   * The underlying Forge network channel that this instance sends and registers on.
   *
   * @since 2026.9
   */
  private final SimpleNetworkWrapper packetHandler;

  /**
   * The next network message ID to use on this channel. This value is incremented each time a
   * new network message is registered.
   *
   * @since 1.0
   */
  private int nextNetworkMessageId = 0;

  /**
   * Constructs a network channel wrapper around the given Forge network channel.
   *
   * @param packetHandler the Forge network channel to wrap
   *
   * @since 2026.9
   */
  private CsmNetwork(SimpleNetworkWrapper packetHandler) {
    this.packetHandler = packetHandler;
  }

  /**
   * Creates a network channel with the given name. Each mod container in the mod family calls
   * this once, with its own mod id as the channel name.
   *
   * @param channelName the channel name; must be non-empty and no longer than
   *                    {@link #MAX_CHANNEL_NAME_LENGTH} characters
   *
   * @return the created network channel
   *
   * @throws IllegalArgumentException if the channel name is empty or too long
   * @since 2026.9
   */
  public static CsmNetwork create(String channelName) {
    if (channelName == null || channelName.isEmpty()) {
      throw new IllegalArgumentException("A network channel name must not be empty.");
    }
    if (channelName.length() > MAX_CHANNEL_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "The network channel name \"" + channelName + "\" is " + channelName.length()
              + " characters long; the maximum is " + MAX_CHANNEL_NAME_LENGTH + ".");
    }
    return new CsmNetwork(NetworkRegistry.INSTANCE.newSimpleChannel(channelName));
  }

  /**
   * Registers a network message on this channel.
   *
   * @param handler      The message handler.
   * @param messageClass The message class.
   * @param sides        The sides to register the message for.
   * @param <T>          The message type.
   * @param <V>          The reply type.
   *
   * @since 1.0
   */
  public <T extends IMessage, V extends IMessage> void registerMessage(
      Class<? extends IMessageHandler<T, V>> handler,
      Class<T> messageClass,
      Side... sides) {
    for (Side side : sides) {
      packetHandler.registerMessage(handler, messageClass, nextNetworkMessageId, side);
    }
    nextNetworkMessageId++;
  }

  public void sendTo(IMessage message, EntityPlayerMP side) {
    packetHandler.sendTo(message, side);
  }

  public void sendToAll(IMessage message) {
    packetHandler.sendToAll(message);
  }

  public void sendToServer(IMessage message) {
    packetHandler.sendToServer(message);
  }

  public void sendToDimension(IMessage message, int dimensionId) {
    packetHandler.sendToDimension(message, dimensionId);
  }

  public void sendToAllAround(IMessage message, NetworkRegistry.TargetPoint point) {
    packetHandler.sendToAllAround(message, point);
  }

  public void sendToAllTracking(IMessage message, NetworkRegistry.TargetPoint point) {
    packetHandler.sendToAllTracking(message, point);
  }
}

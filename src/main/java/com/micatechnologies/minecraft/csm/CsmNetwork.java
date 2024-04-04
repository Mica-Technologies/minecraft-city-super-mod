package com.micatechnologies.minecraft.csm;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class CsmNetwork {

  /**
   * The next network message ID to use. This value is incremented each time a new network message
   * is registered.
   *
   * @since 1.0
   */
  private static int nextNetworkMessageId = 0;

  /**
   * The network channel for the mod. Used for sending packets between client and server.
   *
   * @since 1.0.0
   */
  public static final SimpleNetworkWrapper PACKET_HANDLER =
      NetworkRegistry.INSTANCE.newSimpleChannel(
          CsmConstants.MOD_NAMESPACE);

  /**
   * Registers a network message with the mod.
   *
   * @param handler      The message handler.
   * @param messageClass The message class.
   * @param sides        The sides to register the message for.
   * @param <T>          The message type.
   * @param <V>          The reply type.
   *
   * @since 1.0
   */
  public static <T extends IMessage, V extends IMessage> void registerNetworkMessage(
      Class<? extends IMessageHandler<T, V>> handler,
      Class<T> messageClass,
      Side... sides) {
    for (Side side : sides) {
      PACKET_HANDLER.registerMessage(handler, messageClass, nextNetworkMessageId, side);
    }
    nextNetworkMessageId++;
  }

  public static void sendTo(IMessage message, EntityPlayerMP side) {
    PACKET_HANDLER.sendTo(message, side);
  }

  public static void sendToAll(IMessage message) {
    PACKET_HANDLER.sendToAll(message);
  }

  public static void sendToServer(IMessage message) {
    PACKET_HANDLER.sendToServer(message);
  }

  public static void sendToDimension(IMessage message, int dimensionId) {
    PACKET_HANDLER.sendToDimension(message, dimensionId);
  }

  public static void sendToAllAround(IMessage message, NetworkRegistry.TargetPoint point) {
    PACKET_HANDLER.sendToAllAround(message, point);
  }

  public static void sendToAllTracking(IMessage message, NetworkRegistry.TargetPoint point) {
    PACKET_HANDLER.sendToAllTracking(message, point);
  }
}

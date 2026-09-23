package com.micatechnologies.minecraft.csm.hvac;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Server to client, about once a second: the temperature where the player stands, straight out of
 * the simulation. The HUD shows exactly this number; the client computes nothing, which is what
 * keeps it in agreement with the thermostats (the old HUD recomputed a temperature from the
 * client's partial view of the equipment and smoothed it its own way, and drifted tens of
 * degrees from the thermostat on the wall beside it).
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class HvacHudPacket implements IMessage {

  /** Resend an unchanged reading this often, in milliseconds, in case one was lost. */
  private static final long RESEND_MS = 5000L;

  private static final Map<UUID, LastSent> LAST_SENT = new ConcurrentHashMap<>();

  private static final class LastSent {
    final boolean visible;
    final float temperature;
    final boolean indoors;
    final long millis;

    LastSent(boolean visible, float temperature, boolean indoors, long millis) {
      this.visible = visible;
      this.temperature = temperature;
      this.indoors = indoors;
      this.millis = millis;
    }
  }

  private boolean visible;
  private float temperature;
  private boolean indoors;

  public HvacHudPacket() {
  }

  HvacHudPacket(boolean visible, float temperature, boolean indoors) {
    this.visible = visible;
    this.temperature = temperature;
    this.indoors = indoors;
  }

  /**
   * Sends a player their reading when it differs from what they were last sent (to a tenth of a
   * degree), or when the last send is getting old.
   */
  static void send(EntityPlayer player, boolean visible, float temperature, boolean indoors) {
    if (!(player instanceof EntityPlayerMP)) {
      return;
    }
    float t = Math.round(temperature * 10.0f) / 10.0f;
    long now = System.currentTimeMillis();
    LastSent last = LAST_SENT.get(player.getUniqueID());
    if (last != null && !visible && !last.visible) {
      return; // already hidden
    }
    if (last != null && last.visible == visible && last.temperature == t
        && last.indoors == indoors && now - last.millis < RESEND_MS) {
      return;
    }
    LAST_SENT.put(player.getUniqueID(), new LastSent(visible, t, indoors, now));
    CsmHvac.NETWORK.sendTo(new HvacHudPacket(visible, t, indoors), (EntityPlayerMP) player);
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    visible = buf.readBoolean();
    temperature = buf.readFloat();
    indoors = buf.readBoolean();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeBoolean(visible);
    buf.writeFloat(temperature);
    buf.writeBoolean(indoors);
  }

  // region Client state

  /** Last reading received, and when (client clock). Read by the HUD overlay. */
  static volatile boolean clientVisible;
  static volatile float clientTemperature;
  static volatile boolean clientIndoors;
  static volatile long clientReceivedMs;
  static volatile BlockPos clientReceivedAt = BlockPos.ORIGIN;

  /** How long a reading is trusted before the HUD hides. */
  static final long CLIENT_STALE_MS = 8000L;

  /**
   * The client's best answer for a temperature near the player: the last reading if it is fresh
   * and the position is close to where it was taken, else null.
   */
  static Float clientTemperatureNear(World world, BlockPos pos) {
    if (!clientVisible || System.currentTimeMillis() - clientReceivedMs > CLIENT_STALE_MS) {
      return null;
    }
    return clientReceivedAt.distanceSq(pos) <= 64.0 ? clientTemperature : null;
  }

  // endregion

  /** Client-side handler. */
  public static class Handler implements IMessageHandler<HvacHudPacket, IMessage> {

    @Override
    public IMessage onMessage(HvacHudPacket message, MessageContext ctx) {
      Minecraft.getMinecraft().addScheduledTask(() -> receive(message));
      return null;
    }

    @SideOnly(Side.CLIENT)
    private static void receive(HvacHudPacket message) {
      Minecraft mc = Minecraft.getMinecraft();
      clientVisible = message.visible;
      clientTemperature = message.temperature;
      clientIndoors = message.indoors;
      clientReceivedMs = System.currentTimeMillis();
      clientReceivedAt = mc.player != null ? new BlockPos(mc.player) : BlockPos.ORIGIN;
    }
  }
}

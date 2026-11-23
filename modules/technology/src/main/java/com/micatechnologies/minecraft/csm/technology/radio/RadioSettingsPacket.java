package com.micatechnologies.minecraft.csm.technology.radio;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client to server: the settings a player left a radio's screen with, for the radio in one of
 * their hands or the one standing at a position.
 *
 * <p>Sent once, when the screen closes. The values are only what the client says; the handler
 * brings each into the range of the radio it finds ({@link RadioSettingsHandler}).</p>
 *
 * @since 2026.11
 */
public class RadioSettingsPacket implements IMessage {

  /** The most bytes a unit ID may take on the wire; the text itself is cut far shorter. */
  private static final int MAX_UNIT_ID_BYTES = 64;

  private boolean held;
  private boolean offHand;
  private BlockPos pos = BlockPos.ORIGIN;
  private RadioSettings settings = RadioSettings.DEFAULT;

  public RadioSettingsPacket() {
    // Required by Forge
  }

  /**
   * Settings for the radio in a hand.
   *
   * @param hand     the hand holding it
   * @param settings the settings
   */
  public RadioSettingsPacket(EnumHand hand, RadioSettings settings) {
    this.held = true;
    this.offHand = hand == EnumHand.OFF_HAND;
    this.settings = settings;
  }

  /**
   * Settings for a radio standing in the world.
   *
   * @param pos      its position
   * @param settings the settings
   */
  public RadioSettingsPacket(BlockPos pos, RadioSettings settings) {
    this.held = false;
    this.pos = pos;
    this.settings = settings;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    held = buf.readBoolean();
    offHand = buf.readBoolean();
    pos = BlockPos.fromLong(buf.readLong());
    boolean on = buf.readBoolean();
    int channel = buf.readUnsignedByte();
    int volume = buf.readUnsignedByte();
    int squelch = buf.readUnsignedByte();
    int privacyCode = buf.readUnsignedByte();
    boolean scan = buf.readBoolean();
    String unitId = CsmPacketUtils.readBoundedString(buf, MAX_UNIT_ID_BYTES);
    settings = new RadioSettings(on, channel, volume, squelch, privacyCode, scan, unitId);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeBoolean(held);
    buf.writeBoolean(offHand);
    buf.writeLong(pos.toLong());
    buf.writeBoolean(settings.isOn());
    buf.writeByte(settings.getChannel());
    buf.writeByte(settings.getVolume());
    buf.writeByte(settings.getSquelch());
    buf.writeByte(settings.getPrivacyCode());
    buf.writeBoolean(settings.isScan());
    byte[] unitId = settings.getUnitId().getBytes(StandardCharsets.UTF_8);
    buf.writeInt(unitId.length);
    buf.writeBytes(unitId);
  }

  /** Whether the radio is in a hand rather than standing in the world. */
  public boolean isHeld() {
    return held;
  }

  /** The hand holding the radio, when it is held. */
  public EnumHand getHand() {
    return offHand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
  }

  /** The standing radio's position, when it is not held. */
  public BlockPos getPos() {
    return pos;
  }

  /** The settings, in the widest range any make has. */
  public RadioSettings getSettings() {
    return settings;
  }
}

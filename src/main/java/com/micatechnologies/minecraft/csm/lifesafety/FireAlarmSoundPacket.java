package com.micatechnologies.minecraft.csm.lifesafety;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet sent from server to client to start or stop a fire alarm voice evac sound that
 * follows the player. When starting, includes the sound resource name, hearing range, and all
 * linked speaker positions so the client can calculate distance-based volume locally.
 */
public class FireAlarmSoundPacket implements IMessage {

  private boolean start;
  private String soundResource;
  private float hearingRange;
  private List<BlockPos> speakerPositions;

  public FireAlarmSoundPacket() {
    // Required by Forge
  }

  /**
   * Creates a stop packet.
   */
  public static FireAlarmSoundPacket stop() {
    FireAlarmSoundPacket pkt = new FireAlarmSoundPacket();
    pkt.start = false;
    pkt.soundResource = "";
    pkt.hearingRange = 0;
    pkt.speakerPositions = new ArrayList<>();
    return pkt;
  }

  /**
   * Creates a start packet with the given sound, range, and speaker positions.
   */
  public static FireAlarmSoundPacket start(String soundResource, float hearingRange,
      List<BlockPos> speakerPositions) {
    FireAlarmSoundPacket pkt = new FireAlarmSoundPacket();
    pkt.start = true;
    pkt.soundResource = soundResource;
    pkt.hearingRange = hearingRange;
    pkt.speakerPositions = speakerPositions;
    return pkt;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    start = buf.readBoolean();
    int strLen = buf.readInt();
    byte[] bytes = new byte[strLen];
    buf.readBytes(bytes);
    soundResource = new String(bytes, StandardCharsets.UTF_8);
    hearingRange = buf.readFloat();
    int count = buf.readInt();
    speakerPositions = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      speakerPositions.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeBoolean(start);
    byte[] bytes = soundResource.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
    buf.writeFloat(hearingRange);
    buf.writeInt(speakerPositions.size());
    for (BlockPos pos : speakerPositions) {
      buf.writeInt(pos.getX());
      buf.writeInt(pos.getY());
      buf.writeInt(pos.getZ());
    }
  }

  public boolean isStart() {
    return start;
  }

  public String getSoundResource() {
    return soundResource;
  }

  public float getHearingRange() {
    return hearingRange;
  }

  public List<BlockPos> getSpeakerPositions() {
    return speakerPositions;
  }
}

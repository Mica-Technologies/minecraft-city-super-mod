package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet sent from server to client to start or stop an APS (accessible pedestrian signal)
 * or crosswalk tweeter sound. When starting, includes the sound resource name, hearing range, and
 * source position(s) so the client can calculate distance-based volume locally via
 * {@link APSMovingSound}.
 */
public class APSSoundPacket implements IMessage {

  private boolean start;
  private String channel;
  private String soundResource;
  private float hearingRange;
  private List<BlockPos> sourcePositions;

  public APSSoundPacket() {
    // Required by Forge
  }

  /**
   * Creates a stop packet for a specific channel.
   */
  public static APSSoundPacket stop(String channel) {
    APSSoundPacket pkt = new APSSoundPacket();
    pkt.start = false;
    pkt.channel = channel;
    pkt.soundResource = "";
    pkt.hearingRange = 0;
    pkt.sourcePositions = new ArrayList<>();
    return pkt;
  }

  /**
   * Creates a stop packet that stops all APS sound channels (empty channel = stop all).
   */
  public static APSSoundPacket stopAll() {
    return stop("");
  }

  /**
   * Creates a start packet for a single source position.
   */
  public static APSSoundPacket start(String channel, String soundResource,
      float hearingRange, BlockPos sourcePosition) {
    return start(channel, soundResource, hearingRange, Collections.singletonList(sourcePosition));
  }

  /**
   * Creates a start packet with the given channel, sound, range, and source positions.
   */
  public static APSSoundPacket start(String channel, String soundResource,
      float hearingRange, List<BlockPos> sourcePositions) {
    APSSoundPacket pkt = new APSSoundPacket();
    pkt.start = true;
    pkt.channel = channel;
    pkt.soundResource = soundResource;
    pkt.hearingRange = hearingRange;
    pkt.sourcePositions = sourcePositions;
    return pkt;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    start = buf.readBoolean();
    int channelLen = buf.readInt();
    byte[] channelBytes = new byte[channelLen];
    buf.readBytes(channelBytes);
    channel = new String(channelBytes, StandardCharsets.UTF_8);
    int strLen = buf.readInt();
    byte[] bytes = new byte[strLen];
    buf.readBytes(bytes);
    soundResource = new String(bytes, StandardCharsets.UTF_8);
    hearingRange = buf.readFloat();
    int count = buf.readInt();
    sourcePositions = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      sourcePositions.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeBoolean(start);
    byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(channelBytes.length);
    buf.writeBytes(channelBytes);
    byte[] bytes = soundResource.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
    buf.writeFloat(hearingRange);
    buf.writeInt(sourcePositions.size());
    for (BlockPos pos : sourcePositions) {
      buf.writeInt(pos.getX());
      buf.writeInt(pos.getY());
      buf.writeInt(pos.getZ());
    }
  }

  public boolean isStart() {
    return start;
  }

  public String getChannel() {
    return channel;
  }

  public String getSoundResource() {
    return soundResource;
  }

  public float getHearingRange() {
    return hearingRange;
  }

  public List<BlockPos> getSourcePositions() {
    return sourcePositions;
  }
}

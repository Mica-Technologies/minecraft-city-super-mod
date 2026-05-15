package com.micatechnologies.minecraft.csm.technology;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Server → client packet that starts or stops a {@link SpeakerAmbientSound} for a single
 * speaker. The server fires this when a speaker enters or leaves a player's hearing range
 * with the ambient sound active. Carries the speaker position, the sound resource (for
 * start packets), and the hearing range used by the client to compute distance falloff.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class SpeakerAmbientPacket implements IMessage {

  private BlockPos speakerPos;
  private boolean start;
  private String soundResource;
  private float hearingRange;

  public SpeakerAmbientPacket() {
    // Required by Forge
  }

  /** Convenience: build a "start playing this sound" packet. */
  public static SpeakerAmbientPacket start(BlockPos pos, String soundResource,
      float hearingRange) {
    SpeakerAmbientPacket p = new SpeakerAmbientPacket();
    p.speakerPos = pos;
    p.start = true;
    p.soundResource = soundResource == null ? "" : soundResource;
    p.hearingRange = hearingRange;
    return p;
  }

  /** Convenience: build a "stop the sound at this speaker" packet. */
  public static SpeakerAmbientPacket stop(BlockPos pos) {
    SpeakerAmbientPacket p = new SpeakerAmbientPacket();
    p.speakerPos = pos;
    p.start = false;
    p.soundResource = "";
    p.hearingRange = 0f;
    return p;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.speakerPos = BlockPos.fromLong(buf.readLong());
    this.start = buf.readBoolean();
    this.soundResource = ByteBufUtils.readUTF8String(buf);
    this.hearingRange = buf.readFloat();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(speakerPos.toLong());
    buf.writeBoolean(start);
    ByteBufUtils.writeUTF8String(buf, soundResource);
    buf.writeFloat(hearingRange);
  }

  public BlockPos getSpeakerPos() {
    return speakerPos;
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
}

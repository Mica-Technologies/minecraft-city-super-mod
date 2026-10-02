package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client to server, from the bell controller's screen: save its schedule, or ring a tone now.
 * Every count and string is bounded on decode; the handler checks reach and cleans the rest.
 *
 * @since 2026.10
 */
public class BellScheduleUpdatePacket implements IMessage {

  /** Save the schedule carried. */
  public static final byte SAVE = 0;
  /** Ring the tone carried now; the schedule is not touched. */
  public static final byte RING = 1;

  /** The smallest an encoded bell can be: minute, tone, enabled, text length. */
  private static final int MIN_ENTRY_BYTES = 2 + 1 + 1 + 4;

  private BlockPos pos;
  private byte action;
  private byte ringTone;
  private List<BellSchedule.Entry> entries = new ArrayList<>();

  public BellScheduleUpdatePacket() {
    // Required by Forge
  }

  /**
   * A save.
   *
   * @param pos      the controller
   * @param schedule the schedule to save
   *
   * @return the packet
   */
  public static BellScheduleUpdatePacket save(BlockPos pos, BellSchedule schedule) {
    BellScheduleUpdatePacket p = new BellScheduleUpdatePacket();
    p.pos = pos;
    p.action = SAVE;
    p.entries = new ArrayList<>(schedule.getEntries());
    return p;
  }

  /**
   * A ring now.
   *
   * @param pos  the controller
   * @param tone the tone to ring
   *
   * @return the packet
   */
  public static BellScheduleUpdatePacket ring(BlockPos pos, BellTone tone) {
    BellScheduleUpdatePacket p = new BellScheduleUpdatePacket();
    p.pos = pos;
    p.action = RING;
    p.ringTone = (byte) tone.ordinal();
    return p;
  }

  public BlockPos getPos() {
    return pos;
  }

  public byte getAction() {
    return action;
  }

  public BellTone getRingTone() {
    return BellTone.byOrdinal(ringTone);
  }

  public List<BellSchedule.Entry> getEntries() {
    return entries;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    action = buf.readByte();
    ringTone = buf.readByte();
    int count = CsmPacketUtils.readBoundedCount(buf, BellSchedule.MAX_ENTRIES, MIN_ENTRY_BYTES);
    entries = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      int minute = buf.readShort();
      BellTone tone = BellTone.byOrdinal(buf.readByte());
      boolean enabled = buf.readBoolean();
      String text = CsmPacketUtils.readBoundedString(buf, BellAnnouncePacket.MAX_TEXT_BYTES);
      entries.add(new BellSchedule.Entry(minute, tone, text, enabled));
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(action);
    buf.writeByte(ringTone);
    buf.writeInt(entries.size());
    for (BellSchedule.Entry e : entries) {
      buf.writeShort(e.getMinute());
      buf.writeByte(e.getTone().ordinal());
      buf.writeBoolean(e.isEnabled());
      byte[] bytes = e.getText().getBytes(StandardCharsets.UTF_8);
      buf.writeInt(bytes.length);
      buf.writeBytes(bytes);
    }
  }
}

package com.micatechnologies.minecraft.csm.buildingmaterials;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client to server: a code entered on a garage door keypad, or a new code set on it.
 *
 * <p>Fixed size -- no length is read off the wire -- so decoding cannot be made to allocate. The
 * code travels as a number with its digit count beside it, so a code with leading zeros survives
 * the trip.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class GarageKeypadPacket implements IMessage {

  /** Enter a code, to work the door. */
  public static final int ENTER = 0;
  /** Set a new code (the owner only). */
  public static final int SET = 1;

  private BlockPos pos;
  private int action;
  private int digits;
  private int code;

  /**
   * For Forge's reflection.
   *
   * @since 1.0
   */
  public GarageKeypadPacket() {
  }

  /**
   * Constructs a {@link GarageKeypadPacket}.
   *
   * @param pos    the keypad
   * @param action {@link #ENTER} or {@link #SET}
   * @param entry  the digits, as typed
   *
   * @since 1.0
   */
  public GarageKeypadPacket(BlockPos pos, int action, String entry) {
    this.pos = pos;
    this.action = action;
    this.digits = entry.length();
    this.code = entry.isEmpty() ? 0 : Integer.parseInt(entry);
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    action = buf.readByte();
    digits = buf.readByte();
    code = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(action);
    buf.writeByte(digits);
    buf.writeInt(code);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getAction() {
    return action;
  }

  /**
   * The digits as typed, or null if the packet does not describe a code a keypad could hold.
   *
   * @return the digits, or null
   *
   * @since 1.0
   */
  public String getEntry() {
    if (digits < 1 || digits > TileEntityGarageDoorControl.MAX_DIGITS || code < 0) {
      return null;
    }
    String s = String.format("%0" + digits + "d", code);
    return s.length() == digits ? s : null;
  }
}

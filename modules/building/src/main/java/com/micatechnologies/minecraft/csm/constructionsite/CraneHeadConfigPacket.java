package com.micatechnologies.minecraft.csm.constructionsite;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client to server: a crane's new configuration, from its GUI.
 *
 * <p>Fixed size -- no length is read off the wire -- so decoding cannot be made to allocate.
 * {@code pos} is the block the player clicked to open the GUI, which may be the foot of the mast;
 * the server checks reach against it and finds the head from it, exactly as the GUI did.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class CraneHeadConfigPacket implements IMessage {

  private BlockPos pos;
  private int model;
  private int livery;
  private int jibLength;
  private float slew;
  private float trolley;
  private int hookDrop;
  private float luff;

  /**
   * For Forge's reflection.
   *
   * @since 1.0
   */
  public CraneHeadConfigPacket() {
  }

  /**
   * Constructs a {@link CraneHeadConfigPacket}.
   *
   * @since 1.0
   */
  public CraneHeadConfigPacket(BlockPos pos, CraneModel model, CraneLivery livery, int jibLength,
      float slew, float trolley, int hookDrop, float luff) {
    this.pos = pos;
    this.model = model.ordinal();
    this.livery = livery.ordinal();
    this.jibLength = jibLength;
    this.slew = slew;
    this.trolley = trolley;
    this.hookDrop = hookDrop;
    this.luff = luff;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    model = buf.readByte();
    livery = buf.readByte();
    jibLength = buf.readShort();
    slew = buf.readFloat();
    trolley = buf.readFloat();
    hookDrop = buf.readShort();
    luff = buf.readFloat();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(model);
    buf.writeByte(livery);
    buf.writeShort(jibLength);
    buf.writeFloat(slew);
    buf.writeFloat(trolley);
    buf.writeShort(hookDrop);
    buf.writeFloat(luff);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getModel() {
    return model;
  }

  public int getLivery() {
    return livery;
  }

  public int getJibLength() {
    return jibLength;
  }

  public float getSlew() {
    return slew;
  }

  public float getTrolley() {
    return trolley;
  }

  public int getHookDrop() {
    return hookDrop;
  }

  public float getLuff() {
    return luff;
  }
}

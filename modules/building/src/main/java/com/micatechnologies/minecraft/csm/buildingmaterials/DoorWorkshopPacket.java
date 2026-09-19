package com.micatechnologies.minecraft.csm.buildingmaterials;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client to server: the behaviour on a Door Workshop's screen, and what to do with it -- just
 * remember it, make a door or a stack of them, re-program or copy the door in the edit slot, or
 * save, load or delete a design. Small and bounded: the name is cut to
 * {@link TileEntityDoorWorkshop#MAX_NAME}. Every value is clamped or checked again on the server
 * ({@link CustomDoorSettings} does it on construction).
 *
 * @version 1.0
 * @since 2026.9
 */
public class DoorWorkshopPacket implements IMessage {

  /** Remember the settings. */
  public static final int SET = 0;
  /** Remember them and make a door. */
  public static final int MAKE = 1;
  /** Remember them and re-program the doors in the edit slot. */
  public static final int APPLY = 2;
  /** Make doors until the materials run out or the output is a full stack. */
  public static final int MAKE_ALL = 3;
  /** Take the whole design from the door in the edit slot (the behaviour sent is ignored). */
  public static final int COPY = 4;
  /** Remember them and save the design under {@link #getName()}. */
  public static final int SAVE = 5;
  /** Load saved design {@link #getIndex()} (the behaviour sent is ignored). */
  public static final int LOAD = 6;
  /** Delete saved design {@link #getIndex()}. */
  public static final int DELETE = 7;

  private BlockPos pos;
  private int action;
  private int movement;
  private int sound;
  private int openTicks;
  private int autoCloseTicks;
  private int redstone;
  private boolean proximity;
  private int index;
  private String name = "";

  /**
   * For Forge's reflection.
   *
   * @since 1.0
   */
  public DoorWorkshopPacket() {
  }

  /**
   * Constructs a {@link DoorWorkshopPacket}.
   *
   * @since 1.0
   */
  public DoorWorkshopPacket(BlockPos pos, int action, CustomDoorSettings s) {
    this.pos = pos;
    this.action = action;
    this.movement = s.movement().ordinal();
    this.sound = s.sound().ordinal();
    this.openTicks = s.openTicks();
    this.autoCloseTicks = s.autoCloseTicks();
    this.redstone = s.redstone().ordinal();
    this.proximity = s.proximity();
  }

  /**
   * Constructs a {@link DoorWorkshopPacket} that names a saved design.
   *
   * @since 1.0
   */
  public DoorWorkshopPacket(BlockPos pos, int action, CustomDoorSettings s, int index,
      String name) {
    this(pos, action, s);
    this.index = index;
    this.name = TileEntityDoorWorkshop.clean(name);
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    action = buf.readByte();
    movement = buf.readByte();
    sound = buf.readByte();
    openTicks = buf.readShort();
    autoCloseTicks = buf.readShort();
    redstone = buf.readByte();
    proximity = buf.readBoolean();
    index = buf.readByte();
    int length = Math.min(buf.readUnsignedByte(), TileEntityDoorWorkshop.MAX_NAME);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      b.append(buf.readChar());
    }
    name = b.toString();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(action);
    buf.writeByte(movement);
    buf.writeByte(sound);
    buf.writeShort(openTicks);
    buf.writeShort(autoCloseTicks);
    buf.writeByte(redstone);
    buf.writeBoolean(proximity);
    buf.writeByte(index);
    buf.writeByte(name.length());
    for (char c : name.toCharArray()) {
      buf.writeChar(c);
    }
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getAction() {
    return action;
  }

  public int getIndex() {
    return index;
  }

  public String getName() {
    return name;
  }

  /**
   * The settings sent, with the default materials (the workshop's slots supply the real ones).
   *
   * @return the settings
   *
   * @since 1.0
   */
  public CustomDoorSettings getSettings() {
    return CustomDoorSettings.DEFAULT.withBehaviour(CustomDoorSettings.Movement.of(movement),
        CustomDoorSettings.Sound.of(sound), openTicks, autoCloseTicks,
        CustomDoorSettings.Redstone.of(redstone), proximity);
  }
}

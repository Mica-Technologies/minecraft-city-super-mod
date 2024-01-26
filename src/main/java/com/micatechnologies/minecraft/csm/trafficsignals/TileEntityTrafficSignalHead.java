package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTESRProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTESR;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link TileEntityTrafficSignalHead} is a {@link TileEntity} which is paired with TBD.
 *
 * @version 1.0
 * @see TileEntity
 * @since 2024.1
 */
public class TileEntityTrafficSignalHead extends AbstractTileEntity {

  private static final String NBT_KEY_VISOR_TYPE = "vt";
  private static final String NBT_KEY_BODY_COLOR = "bc";
  private static final String NBT_KEY_FACING = "f";

  private VISOR_TYPE visorType = VISOR_TYPE.CIRCLE;
  private BODY_COLOR bodyColor = BODY_COLOR.BLACK;
  private DirectionSixteen facing = DirectionSixteen.N;


  public enum VISOR_TYPE implements IStringSerializable {
    CAP("cap", 0),
    TUNNEL("tunnel", 1),
    CIRCLE("circle", 2),
    CIRCLE_VERTICAL_LOUVER("circle_vertical_louver", 3),
    CIRCLE_HORIZONTAL_LOUVER("circle_horizontal_louver", 4),
    NONE("none", 5);

    private final String name;
    private final int value;

    private VISOR_TYPE(String name, int value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public @NotNull String getName() {
      return name;
    }

    public int getValue() {
      return value;
    }

    public static VISOR_TYPE fromName(String name) {
      for (VISOR_TYPE visorType : VISOR_TYPE.values()) {
        if (visorType.getName().equals(name)) {
          return visorType;
        }
      }
      return null;
    }

    public static VISOR_TYPE fromValue(int value) {
      for (VISOR_TYPE visorType : VISOR_TYPE.values()) {
        if (visorType.getValue() == value) {
          return visorType;
        }
      }
      return null;
    }
  }

  public enum BODY_COLOR implements IStringSerializable {
    BATTLESHIP_GRAY("battleship_gray", 0),
    BLACK("flat_black", 1),
    YELLOW("yellow", 2),
    DARK_OLIVE_GREEN("dark_olive_green", 3),
    BLACK_AND_YELLOW("black_and_yellow", 4);

    private final String name;
    private final int value;

    private BODY_COLOR(String name, int value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public @NotNull String getName() {
      return name;
    }

    public int getValue() {
      return value;
    }

    public static BODY_COLOR fromName(String name) {
      for (BODY_COLOR bodyColor : BODY_COLOR.values()) {
        if (bodyColor.getName().equals(name)) {
          return bodyColor;
        }
      }
      return null;
    }

    public static BODY_COLOR fromValue(int value) {
      for (BODY_COLOR bodyColor : BODY_COLOR.values()) {
        if (bodyColor.getValue() == value) {
          return bodyColor;
        }
      }
      return null;
    }
  }

  /**
   * Returns the specified NBT tag compound with the {@link TileEntityTrafficSignalHead}'s NBT
   * data.
   *
   * @param compound the NBT tag compound to write the {@link TileEntityTrafficSignalHead}'s NBT
   *                 data to
   *
   * @return the NBT tag compound with the {@link TileEntityTrafficSignalHead}'s NBT data
   *
   * @since 2.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {

    // Write Visor Type to NBT
    compound.setInteger(NBT_KEY_VISOR_TYPE, visorType.getValue());

    // Write Body Color to NBT
    compound.setInteger(NBT_KEY_BODY_COLOR, bodyColor.getValue());

    // Write Facing to NBT
    compound.setInteger(NBT_KEY_FACING, facing.getIndex());

    // Return the NBT tag compound
    return compound;
  }

  /**
   * Processes the reading of the {@link TileEntityTrafficSignalHead}'s NBT data from the supplied
   * NBT tag compound.
   *
   * @param compound the NBT tag compound to read the {@link TileEntityTrafficSignalHead}'s NBT data
   *                 from
   *
   * @since 2.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {

    // Read Visor Type from NBT
    if (compound.hasKey(NBT_KEY_VISOR_TYPE)) {
      visorType = VISOR_TYPE.fromValue(compound.getInteger(NBT_KEY_VISOR_TYPE));
      if (visorType == null) {
        visorType = VISOR_TYPE.NONE;
      }
    }

    // Read Body Color from NBT
    if (compound.hasKey(NBT_KEY_BODY_COLOR)) {
      bodyColor = BODY_COLOR.fromValue(compound.getInteger(NBT_KEY_BODY_COLOR));
      if (bodyColor == null) {
        bodyColor = BODY_COLOR.BLACK;
      }
    }

    // Read Facing from NBT
    if (compound.hasKey(NBT_KEY_FACING)) {
      facing = DirectionSixteen.fromIndex(compound.getInteger(NBT_KEY_FACING));
      if (facing == null) {
        facing = DirectionSixteen.N;
      }
    }
  }

  public VISOR_TYPE getVisorType() {
    return visorType;
  }

  public void setVisorType(VISOR_TYPE visorType) {
    this.visorType = visorType;
  }

  public BODY_COLOR getBodyColor() {
    return bodyColor;
  }

  public void setBodyColor(BODY_COLOR bodyColor) {
    this.bodyColor = bodyColor;
  }

  public DirectionSixteen getFacing() {
    return facing;
  }

  public void setFacing(DirectionSixteen facing) {
    this.facing = facing;
  }
}

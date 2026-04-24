package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityVariableSpeedLimit extends AbstractTileEntity {

  public static final int COLOR_COUNT = 5;
  public static final int ANGLE_COUNT = 5;
  public static final int FLASHER_MODE_COUNT = 3;

  public static final String[] COLOR_NAMES = {"Orange", "Yellow", "Black", "Silver", "White"};
  public static final String[] ANGLE_NAMES =
      {"Normal", "Left Tilt", "Right Tilt", "Left Angle", "Right Angle"};
  public static final String[] FLASHER_MODE_NAMES = {"None", "Off", "On"};

  public static final int FLASHER_NONE = 0;
  public static final int FLASHER_OFF = 1;
  public static final int FLASHER_ON = 2;

  private int speedValue = 35;
  private int flasherMode = FLASHER_ON;
  private int trailerColor = 0;
  private int signAngle = 0;

  @Override
  public void readNBT(NBTTagCompound compound) {
    speedValue = compound.getInteger("speedVal");
    if (speedValue < 20) speedValue = 20;
    if (speedValue > 95) speedValue = 95;

    if (compound.hasKey("flashMode")) {
      flasherMode = compound.getInteger("flashMode");
    }
    flasherMode = Math.max(0, Math.min(FLASHER_MODE_COUNT - 1, flasherMode));

    trailerColor = Math.max(0, Math.min(COLOR_COUNT - 1, compound.getInteger("color")));
    signAngle = Math.max(0, Math.min(ANGLE_COUNT - 1, compound.getInteger("angle")));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger("speedVal", speedValue);
    compound.setInteger("flashMode", flasherMode);
    compound.setInteger("color", trailerColor);
    compound.setInteger("angle", signAngle);
    return compound;
  }

  public int getSpeedValue() {
    return speedValue;
  }

  public int getFlasherMode() {
    return flasherMode;
  }

  public int getTrailerColor() {
    return trailerColor;
  }

  public int getSignAngle() {
    return signAngle;
  }

  public void setData(int speed, int flashMode, int color, int angle) {
    this.speedValue = Math.max(20, Math.min(95, speed));
    this.flasherMode = Math.max(0, Math.min(FLASHER_MODE_COUNT - 1, flashMode));
    this.trailerColor = Math.max(0, Math.min(COLOR_COUNT - 1, color));
    this.signAngle = Math.max(0, Math.min(ANGLE_COUNT - 1, angle));
    markDirtySync(getWorld(), getPos(), true);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 4, pos.getY(), pos.getZ() - 4,
        pos.getX() + 5, pos.getY() + 7, pos.getZ() + 5);
  }
}

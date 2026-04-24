package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityPortableMessageSign extends AbstractTileEntity {

  private static final int MAX_PAGES = 8;
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

  private final List<String[]> pages = new ArrayList<>();
  private int flasherMode = FLASHER_ON;
  private int cycleSpeed = 3;
  private int trailerColor = 0;
  private int signAngle = 0;

  public TileEntityPortableMessageSign() {
    pages.add(new String[]{"", "", ""});
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    pages.clear();

    if (compound.hasKey("flashMode")) {
      flasherMode = compound.getInteger("flashMode");
    } else if (compound.hasKey("flashOn")) {
      flasherMode = compound.getBoolean("flashOn") ? FLASHER_ON : FLASHER_OFF;
    }
    flasherMode = Math.max(0, Math.min(FLASHER_MODE_COUNT - 1, flasherMode));

    cycleSpeed = compound.hasKey("speed") ? compound.getInteger("speed") : 3;
    if (cycleSpeed < 1) cycleSpeed = 1;
    if (cycleSpeed > 10) cycleSpeed = 10;
    trailerColor = Math.max(0, Math.min(COLOR_COUNT - 1, compound.getInteger("color")));
    signAngle = Math.max(0, Math.min(ANGLE_COUNT - 1, compound.getInteger("angle")));

    if (compound.hasKey("pages")) {
      NBTTagList list = compound.getTagList("pages", Constants.NBT.TAG_COMPOUND);
      for (int i = 0; i < list.tagCount(); i++) {
        NBTTagCompound page = list.getCompoundTagAt(i);
        pages.add(new String[]{
            page.getString("l0"),
            page.getString("l1"),
            page.getString("l2")
        });
      }
    }

    if (pages.isEmpty()) {
      if (compound.hasKey("l1")) {
        pages.add(new String[]{
            compound.getString("l1"),
            compound.getString("l2"),
            compound.getString("l3")
        });
      } else {
        pages.add(new String[]{"", "", ""});
      }
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger("flashMode", flasherMode);
    compound.setInteger("speed", cycleSpeed);
    compound.setInteger("color", trailerColor);
    compound.setInteger("angle", signAngle);

    NBTTagList list = new NBTTagList();
    for (String[] page : pages) {
      NBTTagCompound p = new NBTTagCompound();
      p.setString("l0", page[0]);
      p.setString("l1", page[1]);
      p.setString("l2", page[2]);
      list.appendTag(p);
    }
    compound.setTag("pages", list);
    return compound;
  }

  public int getPageCount() {
    return pages.size();
  }

  public String[] getPage(int index) {
    if (index < 0 || index >= pages.size()) {
      return new String[]{"", "", ""};
    }
    return pages.get(index);
  }

  public String getLine1() {
    return pages.isEmpty() ? "" : pages.get(0)[0];
  }

  public String getLine2() {
    return pages.isEmpty() ? "" : pages.get(0)[1];
  }

  public String getLine3() {
    return pages.isEmpty() ? "" : pages.get(0)[2];
  }

  public int getFlasherMode() {
    return flasherMode;
  }

  public int getCycleSpeed() {
    return cycleSpeed;
  }

  public int getTrailerColor() {
    return trailerColor;
  }

  public int getSignAngle() {
    return signAngle;
  }

  public void setData(List<String[]> newPages, int flashMode, int speed, int color, int angle) {
    pages.clear();
    for (String[] p : newPages) {
      if (pages.size() >= MAX_PAGES) break;
      pages.add(new String[]{
          p.length > 0 && p[0] != null ? p[0] : "",
          p.length > 1 && p[1] != null ? p[1] : "",
          p.length > 2 && p[2] != null ? p[2] : ""
      });
    }
    if (pages.isEmpty()) {
      pages.add(new String[]{"", "", ""});
    }
    this.flasherMode = Math.max(0, Math.min(FLASHER_MODE_COUNT - 1, flashMode));
    this.cycleSpeed = Math.max(1, Math.min(10, speed));
    this.trailerColor = Math.max(0, Math.min(COLOR_COUNT - 1, color));
    this.signAngle = Math.max(0, Math.min(ANGLE_COUNT - 1, angle));
    markDirtySync(getWorld(), getPos(), true);
  }

  public int getCurrentPageIndex() {
    if (pages.size() <= 1) return 0;
    long interval = cycleSpeed * 1000L;
    return (int) ((System.currentTimeMillis() / interval) % pages.size());
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 4, pos.getY(), pos.getZ() - 4,
        pos.getX() + 5, pos.getY() + 7, pos.getZ() + 5);
  }
}

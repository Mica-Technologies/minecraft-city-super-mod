package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The ID number on a utility box: one or two short lines, as the stick-on digits on a pad-mount
 * transformer are ("2290" over "50").
 *
 * <p>A box nobody has numbered still shows a number, made up from its position, so a street of
 * them does not read as a row of blank decals and two neighbours do not match. That number is
 * computed wherever it is drawn and never stored or sent; only a number a player typed is.</p>
 *
 * <p>A pad-mount transformer also carries the utility's trouble phone number on its "IN CASE OF
 * TROUBLE / CALL" sticker ({@link UtilityBoxSpec.Phone}). The same rule holds: empty means
 * {@link #DEFAULT_PHONE}, and only a number a player typed is stored.</p>
 *
 * @version 1.0
 */
public class TileEntityUtilityBoxLabel extends AbstractTileEntity {

  /** Characters a line may hold. A real decal line is four or five digits. */
  public static final int MAX_LINE_LENGTH = 6;

  /** Characters the trouble phone number may hold. */
  public static final int MAX_PHONE_LENGTH = 14;

  /** The trouble number a transformer shows until a player sets one: obviously fictional. */
  public static final String DEFAULT_PHONE = "555-DWP-0100";

  private static final String KEY_LINE_1 = "l1";
  private static final String KEY_LINE_2 = "l2";
  private static final String KEY_PHONE = "ph";

  private String line1 = "";
  private String line2 = "";
  private String phone = "";

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  /** Whether a player has numbered this box. */
  public boolean isSet() {
    return !line1.isEmpty() || !line2.isEmpty();
  }

  public void setLines(String line1, String line2) {
    this.line1 = clamp(line1);
    this.line2 = clamp(line2);
  }

  /** The trouble phone number a player set, or empty for {@link #DEFAULT_PHONE}. */
  public String getPhone() {
    return phone;
  }

  /** The trouble phone number to print: the player's, or the default. */
  public String getShownPhone() {
    return phone.isEmpty() ? DEFAULT_PHONE : phone;
  }

  public void setPhone(String phone) {
    this.phone = clampPhone(phone);
  }

  /**
   * What a phone number may hold: capitals, digits, a space, a hyphen, a full stop and round
   * brackets, at most {@link #MAX_PHONE_LENGTH} of them. Lower case is raised rather than
   * refused.
   */
  public static String clampPhone(String text) {
    if (text == null) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (char c : text.toUpperCase().toCharArray()) {
      if (out.length() >= MAX_PHONE_LENGTH) {
        break;
      }
      if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '-'
          || c == '.' || c == '(' || c == ')') {
        out.append(c);
      }
    }
    return out.toString().trim();
  }

  /**
   * What a line may hold: capitals, digits, a space, a hyphen or a slash, at most
   * {@link #MAX_LINE_LENGTH} of them. Lower case is raised rather than refused.
   */
  public static String clamp(String text) {
    if (text == null) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (char c : text.toUpperCase().toCharArray()) {
      if (out.length() >= MAX_LINE_LENGTH) {
        break;
      }
      if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '-'
          || c == '/') {
        out.append(c);
      }
    }
    return out.toString().trim();
  }

  /** The made-up number an unnumbered box shows: four digits over two. */
  public static String[] defaultLines(BlockPos pos) {
    long h = pos.toLong() * 0x9E3779B97F4A7C15L;
    h ^= h >>> 29;
    int first = (int) Math.floorMod(h, 9000L) + 1000;
    int second = (int) Math.floorMod(h >>> 20, 90L) + 10;
    return new String[]{String.valueOf(first), String.valueOf(second)};
  }

  /** Nothing a baked model reads comes from here; only the special renderer does. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  /**
   * The whole unit, not just the root cell: on a two-wide box the decal sits over the seam
   * between cells, and a renderer is culled by this box.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(getPos()).grow(UtilityBoxSpec.MAX_CELLS - 1);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    line1 = clamp(compound.getString(KEY_LINE_1));
    line2 = clamp(compound.getString(KEY_LINE_2));
    phone = clampPhone(compound.getString(KEY_PHONE));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(KEY_LINE_1, line1);
    compound.setString(KEY_LINE_2, line2);
    if (!phone.isEmpty()) {
      compound.setString(KEY_PHONE, phone);
    }
    return compound;
  }
}

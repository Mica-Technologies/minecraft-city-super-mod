package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficTimeOfDaySchedule;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * State for a school zone speed limit assembly: the speed it posts, how big the panel is, how
 * many beacon bars it carries, and the hours those beacons flash.
 *
 * <p>Nothing here ticks. Whether the beacons are flashing right now is a pure function of the
 * world's time of day and the stored schedule, so {@link #isFlashingNow()} answers it on demand
 * and the renderer asks per frame. That also means the answer needs no syncing beyond the
 * schedule itself, and a chunk that reloads mid-window comes back flashing.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntitySchoolZoneBeacon extends AbstractTileEntity {

  /** Beacons never flash, whatever the clock says. */
  public static final int MODE_OFF = 0;
  /** Beacons flash inside either posted window. */
  public static final int MODE_SCHEDULED = 1;
  /** Beacons flash continuously — for a zone worked by hand, or a test. */
  public static final int MODE_ON = 2;
  public static final int MODE_COUNT = 3;
  public static final String[] MODE_NAMES = {"Off", "Scheduled", "Always On"};

  /** One beacon above the sign and a second below it, as a taller assembly carries. */
  public static final int BEACONS_ABOVE_AND_BELOW = 0;
  /** A single beacon above the sign. */
  public static final int BEACONS_ABOVE = 1;
  /** A wig-wag pair side by side above the sign. */
  public static final int BEACONS_TWO_ABOVE = 2;
  public static final int BEACON_ARRANGEMENT_COUNT = 3;
  public static final String[] BEACON_ARRANGEMENT_NAMES =
      {"One Above, One Below", "One Above", "Two Above"};

  /** Beacon head diameters, matching the signal sections the renderer borrows geometry from. */
  public static final int BEACON_SIZE_8_INCH = 0;
  public static final int BEACON_SIZE_12_INCH = 1;
  public static final int BEACON_SIZE_COUNT = 2;
  public static final String[] BEACON_SIZE_NAMES = {"8 inch", "12 inch"};

  /**
   * Lens styles a beacon offers — the full signal set, since a beacon lens is an ordinary ball
   * and the shared atlas carries a yellow tile for every one of them.
   *
   * <p>The unlit tile differs by style: GTX, DR6 and LED Dotted each have a single untinted off
   * texture while LED and Incandescent keep a tinted one, so the choice shows most of the day,
   * when the beacon is dark.
   */
  public static final TrafficSignalBulbStyle[] BULB_STYLES = {
      TrafficSignalBulbStyle.LED,
      TrafficSignalBulbStyle.LED_DOTTED,
      TrafficSignalBulbStyle.INCANDESCENT,
      TrafficSignalBulbStyle.GTX,
      TrafficSignalBulbStyle.DR6};

  /**
   * Visor shells a beacon offers. Deliberately not the signal's full list: louvers exist to
   * narrow a signal's visibility cone, which defeats the point of a flasher meant to be seen
   * from the whole approach, and the two Barlo types are a strobe assembly this renderer does
   * not draw.
   */
  public static final TrafficSignalVisorType[] VISOR_TYPES = {
      TrafficSignalVisorType.CIRCLE,
      TrafficSignalVisorType.TUNNEL,
      TrafficSignalVisorType.CUTAWAY,
      TrafficSignalVisorType.NONE};

  /**
   * Housing colours a beacon offers: the standard blacks, grays, yellows and greens that school
   * assemblies actually ship in. The specialty colours in the signal palette are left out.
   */
  public static final TrafficSignalBodyColor[] HOUSING_COLORS = {
      TrafficSignalBodyColor.FLAT_BLACK,
      TrafficSignalBodyColor.GLOSSY_BLACK,
      TrafficSignalBodyColor.BATTLESHIP_GRAY,
      TrafficSignalBodyColor.CHARCOAL_GRAY,
      TrafficSignalBodyColor.YELLOW,
      TrafficSignalBodyColor.SCHOOL_BUS_YELLOW,
      TrafficSignalBodyColor.DARK_OLIVE_GREEN,
      TrafficSignalBodyColor.FOREST_GREEN};

  /** Selectable panel sizes, as a multiplier on the drawn assembly. */
  public static final float[] SCALES = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
  public static final String[] SCALE_NAMES = {"75%", "100%", "125%", "150%", "200%"};

  /**
   * The plaque's background. The MUTCD permits either colour on a school warning sign, and this
   * is the same choice the radar speed sign offers, so the two agree when they share a pole.
   */
  private static final MutcdSignFaceColor DEFAULT_BANNER_COLOR =
      MutcdSignFaceColor.FLUORESCENT_YELLOW_GREEN;

  public static final int MIN_SPEED = 5;
  public static final int MAX_SPEED = 45;

  private static final String KEY_SPEED = "spd";
  private static final String KEY_SCALE = "scl";
  private static final String KEY_ARRANGEMENT = "arr";
  private static final String KEY_BEACON_SIZE = "bsz";
  private static final String KEY_BULB_STYLE = "bst";
  private static final String KEY_VISOR_TYPE = "vsr";
  private static final String KEY_HOUSING_COLOR = "hcl";
  private static final String KEY_BANNER_COLOR = "bnc";
  private static final String KEY_MODE = "mode";
  private static final String KEY_AM_START = "amS";
  private static final String KEY_AM_END = "amE";
  private static final String KEY_PM_START = "pmS";
  private static final String KEY_PM_END = "pmE";

  private int speedLimit = 20;
  private int scaleIndex = 1;
  private int arrangement = BEACONS_ABOVE;
  private int beaconSize = BEACON_SIZE_8_INCH;
  private int bulbStyleIndex = 0;
  private int visorTypeIndex = 0;
  private int housingColorIndex = 0;
  private MutcdSignFaceColor bannerColor = DEFAULT_BANNER_COLOR;
  private int mode = MODE_SCHEDULED;

  // Two windows, because that is what a real school zone posts: one around arrival and one
  // around dismissal. A zone that only wants one can collapse the second onto itself.
  private int amStartHour = 7;
  private int amEndHour = 9;
  private int pmStartHour = 14;
  private int pmEndHour = 16;

  public int getSpeedLimit() {
    return speedLimit;
  }

  public int getScaleIndex() {
    return scaleIndex;
  }

  public float getScale() {
    return SCALES[clamp(scaleIndex, 0, SCALES.length - 1)];
  }

  public int getArrangement() {
    return arrangement;
  }

  public int getBeaconSize() {
    return beaconSize;
  }

  public int getBulbStyleIndex() {
    return bulbStyleIndex;
  }

  /** The lens style the beacons are drawn with. */
  public TrafficSignalBulbStyle getBulbStyle() {
    return BULB_STYLES[clamp(bulbStyleIndex, 0, BULB_STYLES.length - 1)];
  }

  public int getVisorTypeIndex() {
    return visorTypeIndex;
  }

  /** The visor shell the beacons are drawn with. */
  public TrafficSignalVisorType getVisorType() {
    return VISOR_TYPES[clamp(visorTypeIndex, 0, VISOR_TYPES.length - 1)];
  }

  public int getHousingColorIndex() {
    return housingColorIndex;
  }

  /** The colour of the beacon housings, doors, visors and their bracketry. */
  public TrafficSignalBodyColor getHousingColor() {
    return HOUSING_COLORS[clamp(housingColorIndex, 0, HOUSING_COLORS.length - 1)];
  }

  /** The SCHOOL plaque's background. The sign body under it is always white. */
  public MutcdSignFaceColor getBannerColor() {
    return bannerColor;
  }

  public int getMode() {
    return mode;
  }

  public int getAmStartHour() {
    return amStartHour;
  }

  public int getAmEndHour() {
    return amEndHour;
  }

  public int getPmStartHour() {
    return pmStartHour;
  }

  public int getPmEndHour() {
    return pmEndHour;
  }

  /**
   * The world's hour of day, 0-23.
   *
   * @return the hour of day, or -1 with no world
   */
  public int getWorldHour() {
    return TrafficTimeOfDaySchedule.hourOfDay(world);
  }

  /**
   * Whether the beacons should be flashing at this moment.
   *
   * <p>Windows are read inclusive of the start hour and exclusive of the end, and a window whose
   * end is at or before its start is taken as wrapping past midnight rather than as empty — so a
   * night-shift zone posted 22 to 02 behaves the way it reads.</p>
   *
   * @return {@code true} if the beacons are lit this instant
   */
  public boolean isFlashingNow() {
    if (mode == MODE_OFF) {
      return false;
    }
    if (mode == MODE_ON) {
      return true;
    }
    int hour = getWorldHour();
    if (hour < 0) {
      return false;
    }
    return inWindow(hour, amStartHour, amEndHour) || inWindow(hour, pmStartHour, pmEndHour);
  }

  /**
   * Whether an hour falls inside a posted window.
   *
   * <p>The arithmetic itself is {@link TrafficTimeOfDaySchedule#inWindow}, shared with the
   * controllers that read the same clock. This alias stays because the beacon's own tests
   * document the beacon's contract, and because the short name reads better where it is used.
   * </p>
   *
   * @param hour  the hour of day, 0-23
   * @param start the window's first hour, inclusive
   * @param end   the window's last hour, exclusive
   *
   * @return {@code true} if the hour is inside the window
   */
  static boolean inWindow(int hour, int start, int end) {
    return TrafficTimeOfDaySchedule.inWindow(hour, start, end);
  }

  public void setSpeedLimit(int speed) {
    this.speedLimit = clamp(speed, MIN_SPEED, MAX_SPEED);
    sync();
  }

  public void setScaleIndex(int index) {
    this.scaleIndex = clamp(index, 0, SCALES.length - 1);
    sync();
  }

  public void setArrangement(int arrangement) {
    this.arrangement = clamp(arrangement, 0, BEACON_ARRANGEMENT_COUNT - 1);
    sync();
  }

  public void setBeaconSize(int beaconSize) {
    this.beaconSize = clamp(beaconSize, 0, BEACON_SIZE_COUNT - 1);
    sync();
  }

  public void setBulbStyleIndex(int bulbStyleIndex) {
    this.bulbStyleIndex = clamp(bulbStyleIndex, 0, BULB_STYLES.length - 1);
    sync();
  }

  public void setVisorTypeIndex(int visorTypeIndex) {
    this.visorTypeIndex = clamp(visorTypeIndex, 0, VISOR_TYPES.length - 1);
    sync();
  }

  public void setHousingColorIndex(int housingColorIndex) {
    this.housingColorIndex = clamp(housingColorIndex, 0, HOUSING_COLORS.length - 1);
    sync();
  }

  public void setBannerColor(MutcdSignFaceColor bannerColor) {
    this.bannerColor = bannerColor == null ? DEFAULT_BANNER_COLOR : bannerColor;
    sync();
  }

  public void setMode(int mode) {
    this.mode = clamp(mode, 0, MODE_COUNT - 1);
    sync();
  }

  /**
   * Sets one of the four schedule hours.
   *
   * @param which 0 = AM start, 1 = AM end, 2 = PM start, 3 = PM end
   * @param hour  the hour of day, wrapped into 0-23
   */
  public void setScheduleHour(int which, int hour) {
    int wrapped = ((hour % 24) + 24) % 24;
    switch (which) {
      case 0:
        amStartHour = wrapped;
        break;
      case 1:
        amEndHour = wrapped;
        break;
      case 2:
        pmStartHour = wrapped;
        break;
      case 3:
        pmEndHour = wrapped;
        break;
      default:
        return;
    }
    sync();
  }

  /**
   * Reads one of the four schedule hours.
   *
   * @param which 0 = AM start, 1 = AM end, 2 = PM start, 3 = PM end
   *
   * @return the hour, or 0 for an unknown index
   */
  public int getScheduleHour(int which) {
    switch (which) {
      case 0:
        return amStartHour;
      case 1:
        return amEndHour;
      case 2:
        return pmStartHour;
      case 3:
        return pmEndHour;
      default:
        return 0;
    }
  }

  /**
   * Pushes a change to the client, guarded on there being a world to push into. The sibling
   * crosswalk tile entity guards its setters the same way: a tile entity can be configured
   * before it is placed, and syncing then dereferences a world that does not exist yet.
   */
  private void sync() {
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  private static int clamp(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    speedLimit = clamp(compound.getInteger(KEY_SPEED), MIN_SPEED, MAX_SPEED);
    scaleIndex = clamp(compound.getInteger(KEY_SCALE), 0, SCALES.length - 1);
    // Absent means the tag was never written, and index 0 is now the two-beacon arrangement
    // rather than the single one, so an unconfigured beacon has to fall back explicitly.
    arrangement = compound.hasKey(KEY_ARRANGEMENT)
        ? clamp(compound.getInteger(KEY_ARRANGEMENT), 0, BEACON_ARRANGEMENT_COUNT - 1)
        : BEACONS_ABOVE;
    beaconSize = clamp(compound.getInteger(KEY_BEACON_SIZE), 0, BEACON_SIZE_COUNT - 1);
    // Absent on beacons saved before these were configurable, which getInteger reads as 0 —
    // the first entry of each table, i.e. the look those beacons already had.
    bulbStyleIndex = clamp(compound.getInteger(KEY_BULB_STYLE), 0, BULB_STYLES.length - 1);
    visorTypeIndex = clamp(compound.getInteger(KEY_VISOR_TYPE), 0, VISOR_TYPES.length - 1);
    housingColorIndex =
        clamp(compound.getInteger(KEY_HOUSING_COLOR), 0, HOUSING_COLORS.length - 1);
    // Absent means a beacon saved before the colour was a choice, and every one of those was
    // fluorescent yellow-green -- which is ordinal 1, so this cannot lean on the zero default.
    bannerColor = compound.hasKey(KEY_BANNER_COLOR)
        ? MutcdSignFaceColor.fromNBT(compound.getInteger(KEY_BANNER_COLOR))
        : DEFAULT_BANNER_COLOR;
    // Absent means a beacon saved before the mode existed; those all ran on their schedule.
    mode = compound.hasKey(KEY_MODE)
        ? clamp(compound.getInteger(KEY_MODE), 0, MODE_COUNT - 1)
        : MODE_SCHEDULED;
    if (compound.hasKey(KEY_AM_START)) {
      amStartHour = clamp(compound.getInteger(KEY_AM_START), 0, 23);
      amEndHour = clamp(compound.getInteger(KEY_AM_END), 0, 23);
      pmStartHour = clamp(compound.getInteger(KEY_PM_START), 0, 23);
      pmEndHour = clamp(compound.getInteger(KEY_PM_END), 0, 23);
    }
    // A speed of zero means the tag was absent entirely, not that someone posted 0 mph.
    if (compound.getInteger(KEY_SPEED) == 0) {
      speedLimit = 20;
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(KEY_SPEED, speedLimit);
    compound.setInteger(KEY_SCALE, scaleIndex);
    compound.setInteger(KEY_ARRANGEMENT, arrangement);
    compound.setInteger(KEY_BEACON_SIZE, beaconSize);
    compound.setInteger(KEY_BULB_STYLE, bulbStyleIndex);
    compound.setInteger(KEY_VISOR_TYPE, visorTypeIndex);
    compound.setInteger(KEY_HOUSING_COLOR, housingColorIndex);
    compound.setInteger(KEY_BANNER_COLOR, bannerColor.ordinal());
    compound.setInteger(KEY_MODE, mode);
    compound.setInteger(KEY_AM_START, amStartHour);
    compound.setInteger(KEY_AM_END, amEndHour);
    compound.setInteger(KEY_PM_START, pmStartHour);
    compound.setInteger(KEY_PM_END, pmEndHour);
    return compound;
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return LONG_RANGE_RENDER_DISTANCE_SQUARED;
  }

  /**
   * The assembly is drawn taller than its own cell — a beacon bar above the sign, and at the
   * larger scales a good deal more — so the render box has to cover what is actually drawn or
   * the whole thing vanishes when the block's own cell leaves the frustum.
   */
  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 2, pos.getY() - 4, pos.getZ() - 2,
        pos.getX() + 3, pos.getY() + 8, pos.getZ() + 3);
  }
}

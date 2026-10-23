package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

/**
 * A whole sign at a click of the editor's preset button: its lines, pictogram and colours, and
 * its arrow where it has one (otherwise the sign keeps the arrow it had, since which way a sign
 * points depends on where it hangs, not on what it says). Everything else the sign has (its
 * mount, scale, line icons, custom colours) is kept too, so a preset only rewrites what it says.
 *
 * @since 2026.10
 */
public final class WayfindingPreset {

  /** The airport panel's presets. */
  public static final WayfindingPreset[] AIRPORT = {
      new WayfindingPreset("GATES", "", WayfindingIcon.DEPART, WayfindingArrow.RIGHT,
          WayfindingScheme.AIRPORT),
      new WayfindingPreset("ARRIVALS", "", WayfindingIcon.ARRIVE, null, WayfindingScheme.AIRPORT),
      new WayfindingPreset("CHECK-IN", "", WayfindingIcon.CHECKIN, null,
          WayfindingScheme.AIRPORT),
      new WayfindingPreset("BAGGAGE CLAIM", "", WayfindingIcon.BAGGAGE, null,
          WayfindingScheme.AIRPORT),
      new WayfindingPreset("GROUND", "TRANSPORTATION", WayfindingIcon.GROUND, null,
          WayfindingScheme.AIRPORT),
      new WayfindingPreset("TO TRAINS", "", WayfindingIcon.TRAIN, null, WayfindingScheme.METRO),
      new WayfindingPreset("EXIT", "", WayfindingIcon.EXIT, null, WayfindingScheme.EXIT),
      new WayfindingPreset("RESTROOMS", "", WayfindingIcon.RESTROOM, null,
          WayfindingScheme.INFO),
  };

  /** The hospital panel's presets. */
  public static final WayfindingPreset[] HOSPITAL = {
      new WayfindingPreset("INFORMATION", "", WayfindingIcon.INFO, null,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("EMERGENCY", "", WayfindingIcon.EMERGENCY, null,
          WayfindingScheme.EMERGENCY),
      new WayfindingPreset("IMAGING", "X-RAY  CT  MRI", WayfindingIcon.XRAY, null,
          WayfindingScheme.IMAGING),
      new WayfindingPreset("SURGERY", "", WayfindingIcon.NONE, null, WayfindingScheme.SURGERY),
      new WayfindingPreset("MATERNITY", "", WayfindingIcon.MATERNITY, null,
          WayfindingScheme.MATERNITY),
      new WayfindingPreset("OUTPATIENT", "CLINICS", WayfindingIcon.NONE, null,
          WayfindingScheme.OUTPATIENT),
      new WayfindingPreset("WARDS", "", WayfindingIcon.NONE, null, WayfindingScheme.INPATIENT),
      new WayfindingPreset("LABORATORY", "", WayfindingIcon.LAB, null,
          WayfindingScheme.LABORATORY),
      new WayfindingPreset("PHARMACY", "", WayfindingIcon.PHARMACY, null,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("ADMINISTRATION", "", WayfindingIcon.NONE, null,
          WayfindingScheme.ADMIN),
      new WayfindingPreset("ELEVATORS", "", WayfindingIcon.ELEVATOR, null,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("STAIRS", "", WayfindingIcon.STAIRS, null, WayfindingScheme.NEUTRAL),
      new WayfindingPreset("RESTROOMS", "", WayfindingIcon.RESTROOM, null,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("CAFE", "", WayfindingIcon.CAFE, null, WayfindingScheme.NEUTRAL),
      new WayfindingPreset("CHAPEL", "", WayfindingIcon.CHAPEL, null, WayfindingScheme.NEUTRAL),
      new WayfindingPreset("PARKING", "", WayfindingIcon.PARKING, null,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("EXIT", "", WayfindingIcon.EXIT, null, WayfindingScheme.EXIT),
  };

  /** The generic panel's presets: buildings, offices, museums and stairwells. */
  public static final WayfindingPreset[] BUILDING = {
      new WayfindingPreset("INFORMATION", "", WayfindingIcon.INFO, null,
          WayfindingScheme.CORPORATE),
      new WayfindingPreset("LEVEL 1", "STAIR A", WayfindingIcon.STAIRS, null,
          WayfindingScheme.CORPORATE),
      new WayfindingPreset("ELEVATORS", "", WayfindingIcon.ELEVATOR, null,
          WayfindingScheme.CORPORATE),
      new WayfindingPreset("RESTROOMS", "", WayfindingIcon.RESTROOM, null,
          WayfindingScheme.CORPORATE),
      new WayfindingPreset("RECEPTION", "", WayfindingIcon.INFO, null,
          WayfindingScheme.CORPORATE),
      new WayfindingPreset("MEETING ROOMS", "", WayfindingIcon.NONE, null,
          WayfindingScheme.CORPORATE),
      new WayfindingPreset("ROOM 101", "", WayfindingIcon.NONE, WayfindingArrow.NONE,
          WayfindingScheme.CORPORATE),
      new WayfindingPreset("GALLERIES", "", WayfindingIcon.GALLERY, null,
          WayfindingScheme.NEUTRAL),
      new WayfindingPreset("CAFE", "", WayfindingIcon.CAFE, null, WayfindingScheme.NEUTRAL),
      new WayfindingPreset("GIFT SHOP", "", WayfindingIcon.SHOP, null, WayfindingScheme.NEUTRAL),
      new WayfindingPreset("TICKETS", "", WayfindingIcon.TICKET, null, WayfindingScheme.TRANSIT),
      new WayfindingPreset("FIRST AID", "", WayfindingIcon.FIRST_AID, null,
          WayfindingScheme.EXIT),
      new WayfindingPreset("EXIT", "", WayfindingIcon.EXIT, null, WayfindingScheme.EXIT),
  };

  private final String line1;
  private final String line2;
  private final WayfindingIcon pictogram;
  private final WayfindingArrow arrow;
  private final WayfindingScheme scheme;

  /**
   * A preset.
   *
   * @param line1     the top line
   * @param line2     the second line, empty for none
   * @param pictogram the pictogram
   * @param arrow     the arrow it sets, or null to keep the sign's
   * @param scheme    the colours
   */
  public WayfindingPreset(String line1, String line2, WayfindingIcon pictogram,
      WayfindingArrow arrow, WayfindingScheme scheme) {
    this.line1 = line1;
    this.line2 = line2;
    this.pictogram = pictogram;
    this.arrow = arrow;
    this.scheme = scheme;
  }

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  /** The legend as one line of text, for the editor's button. */
  public String getLabel() {
    return line2.isEmpty() ? line1 : line1 + " / " + line2;
  }

  /**
   * A sign showing this preset: the lines, pictogram, scheme and arrow (if any) put on the sign,
   * lines 3 and 4, the line icons and the line count cleared, and its custom colours dropped so the
   * scheme shows.
   */
  public WayfindingSign applyTo(WayfindingSign sign) {
    WayfindingSign.Builder b = sign.toBuilder().line(0, line1).line(1, line2).line(2, "")
        .line(3, "").lineCount(0).pictogram(pictogram).scheme(scheme)
        .background(WayfindingSign.NO_COLOUR).legend(WayfindingSign.NO_COLOUR);
    for (int i = 0; i < WayfindingSign.MAX_LINES; i++) {
      b.iconLeft(i, WayfindingIcon.NONE).iconRight(i, WayfindingIcon.NONE);
    }
    if (arrow != null) {
      b.arrow(arrow);
    }
    return b.build();
  }

  /** A preset as a fresh sign, for a block's default. */
  public WayfindingSign toSign(WayfindingArrow arrow, boolean lit) {
    return applyTo(WayfindingSign.builder().arrow(arrow).lit(lit).build());
  }
}

package com.micatechnologies.minecraft.csm.trafficaccessories.streetsign;

import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignArrowType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignColor;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.SignLightMode;

/**
 * Pre-built {@link StreetSignData} configurations the player can cycle through from the Style
 * tab. Each call returns a fresh instance, so applying a template never shares state with the
 * data it replaced.
 */
public final class StreetSignTemplates {

  private static final String[] NAMES = {
      "Standard Green Blade",
      "Blue Blade",
      "Illuminated Blade",
      "Numbered Cross Street",
      "Historic District",
      "Flat Wall Blade"
  };

  private StreetSignTemplates() {
  }

  public static int count() {
    return NAMES.length;
  }

  public static String getName(int index) {
    return NAMES[Math.floorMod(index, NAMES.length)];
  }

  public static StreetSignData get(int index) {
    switch (Math.floorMod(index, NAMES.length)) {
      case 0:
        return standardGreen();
      case 1:
        return blueBlade();
      case 2:
        return illuminated();
      case 3:
        return numberedCross();
      case 4:
        return historicDistrict();
      case 5:
        return flatWall();
      default:
        return new StreetSignData();
    }
  }

  private static StreetSignData standardGreen() {
    StreetSignData data = new StreetSignData();
    data.setStreetName("MAIN");
    data.setSuffix("ST");
    return data;
  }

  private static StreetSignData blueBlade() {
    StreetSignData data = new StreetSignData();
    data.setSignColor(GuideSignColor.BLUE);
    data.setStreetName("HARBOR");
    data.setSuffix("BLVD");
    return data;
  }

  /** The internally-lit extruded blade the reference photos show hanging off a mast arm. */
  private static StreetSignData illuminated() {
    StreetSignData data = new StreetSignData();
    data.setStreetName("GREENVILLE");
    data.setSuffix("BLVD");
    data.setExtrudedFrame(true);
    data.setInternalLight(true);
    data.setLightMode(SignLightMode.NIGHT);
    return data;
  }

  private static StreetSignData numberedCross() {
    StreetSignData data = new StreetSignData();
    data.setPrefix("W");
    data.setStreetName("BAKER");
    data.setSuffix("RD");
    data.setBlockNumber("1200");
    data.setBlockPosition(StreetSignSlotPosition.LEFT);
    data.setBlockVertical(StreetSignVerticalPos.BOTTOM);
    return data;
  }

  private static StreetSignData historicDistrict() {
    StreetSignData data = new StreetSignData();
    data.setSignColor(GuideSignColor.BROWN);
    data.setStreetName("OLD MILL");
    data.setSuffix("RD");
    data.setCityText("HISTORIC DISTRICT");
    data.setEmblemKind(StreetSignEmblemKind.LOGO);
    data.setEmblemPosition(StreetSignSlotPosition.LEFT);
    data.setLogoType(StreetSignLogoType.SEAL_LAUREL);
    return data;
  }

  private static StreetSignData flatWall() {
    StreetSignData data = new StreetSignData();
    data.setMountType(StreetSignMount.FLAT);
    data.setStreetName("CENTER");
    data.setSuffix("AVE");
    data.setArrowPosition(StreetSignSlotPosition.RIGHT);
    data.setArrowType(GuideSignArrowType.RIGHT);
    return data;
  }
}

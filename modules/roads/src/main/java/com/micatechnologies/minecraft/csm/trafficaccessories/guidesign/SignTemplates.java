package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

// Pre-built GuideSignData configurations the player can cycle through from the
// Properties tab. Each call returns a fresh instance so applying a template does
// not share state with the previous data.
public final class SignTemplates {

  private static final String[] NAMES = {
      "Blank Green",
      "Blank Blue",
      "Brown Recreation",
      "Standard Exit",
      "Exit Only",
      "Junction Split"
  };

  public static int count() {
    return NAMES.length;
  }

  public static String getName(int index) {
    return NAMES[Math.floorMod(index, NAMES.length)];
  }

  public static GuideSignData get(int index) {
    switch (Math.floorMod(index, NAMES.length)) {
      case 0:
        return blankGreen();
      case 1:
        return blankBlue();
      case 2:
        return brownRecreation();
      case 3:
        return standardExit();
      case 4:
        return exitOnly();
      case 5:
        return junctionSplit();
      default:
        return new GuideSignData();
    }
  }

  private static GuideSignData blankGreen() {
    GuideSignData d = baseData(GuideSignColor.GREEN, PostType.OVERHEAD);
    d.getPanels().clear();
    GuideSignPanel p = new GuideSignPanel();
    p.addRow(textRow("DESTINATION", 1.0f));
    d.getPanels().add(p);
    return d;
  }

  private static GuideSignData blankBlue() {
    GuideSignData d = baseData(GuideSignColor.BLUE, PostType.RIGHT);
    d.getPanels().clear();
    GuideSignPanel p = new GuideSignPanel();
    p.addRow(textRow("SERVICES", 1.0f));
    d.getPanels().add(p);
    return d;
  }

  private static GuideSignData brownRecreation() {
    GuideSignData d = baseData(GuideSignColor.BROWN, PostType.LEFT);
    d.getPanels().clear();
    GuideSignPanel p = new GuideSignPanel();
    p.addRow(textRow("PARK", 1.0f));
    p.addRow(arrowRow(GuideSignArrowType.UP_RIGHT));
    d.getPanels().add(p);
    return d;
  }

  private static GuideSignData standardExit() {
    GuideSignData d = baseData(GuideSignColor.GREEN, PostType.OVERHEAD);
    d.getPanels().clear();
    GuideSignPanel p = new GuideSignPanel();
    ExitTabData exitTab = new ExitTabData();
    exitTab.setText("EXIT 42");
    p.setExitTab(exitTab);
    p.addRow(shieldRow(GuideSignShieldType.INTERSTATE, "95", GuideSignBannerType.NORTH));
    p.addRow(textRow("BALTIMORE", 1.0f));
    p.addRow(arrowRow(GuideSignArrowType.RIGHT));
    d.getPanels().add(p);
    return d;
  }

  private static GuideSignData exitOnly() {
    GuideSignData d = baseData(GuideSignColor.GREEN, PostType.OVERHEAD);
    d.getPanels().clear();
    GuideSignPanel p = new GuideSignPanel();
    ExitTabData exitTab = new ExitTabData();
    exitTab.setText("EXIT 12");
    p.setExitTab(exitTab);
    p.addRow(shieldRow(GuideSignShieldType.INTERSTATE, "70", GuideSignBannerType.EAST));
    p.addRow(textRow("SPRINGFIELD", 1.0f));
    GuideSignRow patch = new GuideSignRow();
    patch.addElement(GuideSignElement.createText("EXIT", 0.9f));
    patch.addElement(GuideSignElement.createArrow(GuideSignArrowType.DOWN));
    patch.addElement(GuideSignElement.createText("ONLY", 0.9f));
    patch.setYellowPatch(true);
    p.addRow(patch);
    d.getPanels().add(p);
    return d;
  }

  private static GuideSignData junctionSplit() {
    GuideSignData d = baseData(GuideSignColor.GREEN, PostType.OVERHEAD);
    d.setMinWidth(48);
    d.getPanels().clear();
    GuideSignPanel top = new GuideSignPanel();
    top.addRow(shieldRow(GuideSignShieldType.INTERSTATE, "95", GuideSignBannerType.NORTH));
    top.addRow(textRow("CITY CENTER", 1.0f));
    top.addRow(arrowRow(GuideSignArrowType.UP_LEFT));
    d.getPanels().add(top);
    GuideSignPanel bottom = new GuideSignPanel();
    bottom.addRow(shieldRow(GuideSignShieldType.US_ROUTE, "1", GuideSignBannerType.SOUTH));
    bottom.addRow(textRow("HARBOR", 1.0f));
    bottom.addRow(arrowRow(GuideSignArrowType.UP_RIGHT));
    d.getPanels().add(bottom);
    return d;
  }

  private static GuideSignData baseData(GuideSignColor color, PostType post) {
    GuideSignData d = new GuideSignData();
    d.setSignColor(color.ordinal());
    d.setPostType(post.ordinal());
    d.setBorderWidth(1);
    d.setCornerStyle(CornerStyle.ROUND.ordinal());
    return d;
  }

  private static GuideSignRow textRow(String text, float scale) {
    GuideSignRow r = new GuideSignRow();
    r.addElement(GuideSignElement.createText(text, scale));
    return r;
  }

  private static GuideSignRow shieldRow(GuideSignShieldType shield, String route,
      GuideSignBannerType banner) {
    GuideSignRow r = new GuideSignRow();
    r.addElement(GuideSignElement.createShield(shield, route, banner));
    return r;
  }

  private static GuideSignRow arrowRow(GuideSignArrowType arrow) {
    GuideSignRow r = new GuideSignRow();
    r.addElement(GuideSignElement.createArrow(arrow));
    return r;
  }

  private SignTemplates() {
  }
}

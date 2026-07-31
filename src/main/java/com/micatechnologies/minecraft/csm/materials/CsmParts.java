package com.micatechnologies.minecraft.csm.materials;

/**
 * Registry names of the mod's crafting parts — the intermediate tier of the survival crafting
 * chain (vanilla ores and ingots to parts, parts to City Super Mod blocks).
 *
 * <p>These are declared as constants rather than inline string literals because they are
 * referenced from several places that must agree exactly: the {@code CSM: Materials} creative tab,
 * the generated block recipes, and the Sign Press, which consumes {@link #SIGN_BLANK}.</p>
 *
 * <p>Each constant must have a matching item model at
 * {@code assets/csm/models/item/&lt;name&gt;.json}, a texture at
 * {@code assets/csm/textures/items/&lt;name&gt;.png}, and a lang entry
 * {@code item.&lt;name&gt;.name}.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public final class CsmParts {

  /**
   * Flat galvanized steel stock. The single most widely used part — the body, backplate or
   * housing of most CSM equipment starts here.
   */
  public static final String SHEET_METAL = "part_sheet_metal";

  /**
   * Assorted bolts, nuts and washers used to fasten equipment to its mounting.
   */
  public static final String FASTENER_KIT = "part_fastener_kit";

  /**
   * A length of hollow steel tube, used for poles, mounts and signposts.
   */
  public static final String POLE_SECTION = "part_pole_section";

  /**
   * A weatherproof equipment cabinet shell, used for controllers, panels and cabinets.
   */
  public static final String ENCLOSURE_SHELL = "part_enclosure_shell";

  /**
   * A loom of insulated conductors. Required by anything that carries power or signalling.
   */
  public static final String WIRING_HARNESS = "part_wiring_harness";

  /**
   * A populated printed circuit board providing the logic for controllers and smart equipment.
   */
  public static final String CONTROL_BOARD = "part_control_board";

  /**
   * A cluster of light-emitting diodes on a carrier, used by every signal and light fixture.
   */
  public static final String LED_MODULE = "part_led_module";

  /**
   * A coloured optical lens and its gasket, used for signal heads and beacons.
   */
  public static final String LENS_ASSEMBLY = "part_lens_assembly";

  /**
   * The driver and cone of an audible appliance — horns, speakers and tweeters.
   */
  public static final String SOUNDER_DRIVER = "part_sounder_driver";

  /**
   * Retroreflective film that makes sign faces legible in headlights.
   */
  public static final String REFLECTIVE_SHEETING = "part_reflective_sheeting";

  /**
   * An undecorated sign panel. Fed into the Sign Press to produce any of the mod's road signs.
   */
  public static final String SIGN_BLANK = "part_sign_blank";

  /**
   * Dry cement, sand and aggregate, used for the mod's concrete building materials.
   */
  public static final String CONCRETE_MIX = "part_concrete_mix";

  /**
   * A section of sheet metal ductwork, used by the HVAC equipment.
   */
  public static final String DUCTING = "part_ducting";

  /**
   * A camera or radar sensing element, used by the traffic detection equipment.
   */
  public static final String OPTICAL_SENSOR = "part_optical_sensor";

  /**
   * Utility class; not instantiable.
   */
  private CsmParts() {
    throw new UnsupportedOperationException("CsmParts is a constant holder and is not "
        + "instantiable.");
  }
}

package com.micatechnologies.minecraft.csm;

/**
 * Class containing constant values and information for the mod.
 *
 * @author ah@micatechnologies.com
 * @version N/T
 * @since 2020.6
 */
public class CsmConstants {

  /**
   * The namespace of the mod, which is used to identify the mod and its elements. The namespace
   * prefixes each block name; for example, in the block ID `csm:testblock1`, the mod namespace is
   * `csm` and the block name is `testblock1`.
   * <p>
   * Note: Changing this value has little effect on new maps, but existing maps will lost any
   * previously placed blocks in the previous namespace.
   */
  public static final String MOD_NAMESPACE = Tags.MODID;

  /**
   * The name of the mod. This name is displayed in the mod list and in the mod configuration
   * screen.
   */
  public static final String MOD_NAME = Tags.MODNAME;

  /**
   * The version of the mod. This number should be incremented by 0.0.1 for bug fix releases,
   * incremented by 0.1 for feature and bug fix releases, or incremented by 1.0 for large or
   * breaking changes.
   */
  public static final String MOD_VERSION = Tags.VERSION;
}

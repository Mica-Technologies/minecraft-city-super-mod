package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/**
 * RealGrid, an OPTIONAL dependency: a floodlight placed against the front of one of its crossarms
 * goes on a bracket back to the arm ({@link BlockFloodlight.Surface#CROSSARM}).
 *
 * <p>Nothing here links against RealGrid: a crossarm is recognised by its registry name and its
 * {@code facing} property, both read off the block state, and only when Forge reports the mod
 * loaded. Without RealGrid every answer is {@code false} and a flood places as it would anywhere.
 *
 * <p>What it relies on (RealGrid's {@code BlockCrossarm}, read from its source): the segments are
 * {@code realgrid:crossarm_<material>}; {@code facing} (horizontal) points away from the pole; the
 * arm is a 6 px square at the top back of its block, so its front face is 6 px back from the cell
 * a flood in front of it is placed in, at y 10..16 (the bracket in the flood's {@code _x} model
 * reaches back to it). Its braces, spacers and guy mounts share the prefix and are not arms.
 *
 * @since 2026.11
 */
public final class RealGridCompat {

  /** RealGrid's mod id, from its {@code mcmod.info}. */
  public static final String MOD_ID = "realgrid";

  private static Boolean loaded;

  private RealGridCompat() {
  }

  /** Whether RealGrid is installed (asked once, then remembered). */
  public static boolean isLoaded() {
    if (loaded == null) {
      loaded = Loader.isModLoaded(MOD_ID);
    }
    return loaded;
  }

  /**
   * Whether a registry name is one of RealGrid's crossarm segments (not a brace, a spacer or a
   * guy mount).
   *
   * @param id a block's registry name
   *
   * @return {@code true} for a crossarm segment
   */
  public static boolean isCrossarmName(ResourceLocation id) {
    if (id == null || !MOD_ID.equals(id.getNamespace())) {
      return false;
    }
    String path = id.getPath();
    return path.startsWith("crossarm_") && !path.contains("brace") && !path.contains("spacer")
        && !path.contains("guy");
  }

  /**
   * Whether {@code state} is a RealGrid crossarm whose front faces {@code side}: a flood placed
   * against that face, in the cell in front of the arm, can bolt to it.
   *
   * @param state the block clicked
   * @param side  the face clicked
   *
   * @return {@code true} if a flood placed there goes on the crossarm bracket
   */
  public static boolean isCrossarmFront(IBlockState state, EnumFacing side) {
    if (!isLoaded() || !isCrossarmName(state.getBlock().getRegistryName())) {
      return false;
    }
    for (IProperty<?> property : state.getPropertyKeys()) {
      if ("facing".equals(property.getName())) {
        Object facing = state.getValue(property);
        return facing == side;
      }
    }
    return false;
  }
}

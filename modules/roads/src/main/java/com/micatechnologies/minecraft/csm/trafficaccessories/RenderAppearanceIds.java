package com.micatechnologies.minecraft.csm.trafficaccessories;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Hands out a small id for each distinct appearance, so a list shared by appearance can be keyed
 * on one that is too wide to pack into a {@code long}: a bracket or cover shaped to the heads
 * beside it is four envelope edges and a few more values, every one a float.
 *
 * <p>An appearance is given as its exact bits (floats through {@link Float#floatToIntBits}), so
 * two appearances share an id only when every value is identical; there is no hashing into the
 * id and no chance of two shapes drawing each other's list.</p>
 *
 * <p>Ids are never reused and the map is never cleared, even when the lists are released on
 * disconnect: a stale id would otherwise name another shape's list. It holds one entry per
 * distinct look seen in the session, which is a handful.</p>
 *
 * <p>Render thread only.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
final class RenderAppearanceIds {

  private final Map<Key, Integer> ids = new HashMap<>();

  /**
   * Returns the id of an appearance, giving it the next free one if it has not been seen.
   *
   * @param bits every value the compiled geometry depends on, as exact bits
   *
   * @return the appearance's id, 0 or more
   */
  int idOf(int... bits) {
    Key key = new Key(bits);
    Integer id = ids.get(key);
    if (id == null) {
      id = ids.size();
      ids.put(key, id);
    }
    return id;
  }

  /** An appearance's bits, compared by value. */
  private static final class Key {

    private final int[] bits;
    private final int hash;

    private Key(int[] bits) {
      this.bits = bits;
      this.hash = Arrays.hashCode(bits);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Key && Arrays.equals(bits, ((Key) o).bits);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }
}

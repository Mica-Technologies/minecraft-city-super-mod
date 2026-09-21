package com.micatechnologies.minecraft.csm.codeutils;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Display lists keyed by what a block looks like rather than where it is, so every copy of a block
 * that looks the same replays one list.
 *
 * <p><b>Why not {@link CsmDisplayListCache}.</b> That cache holds a list per position, which is
 * right when the geometry depends on the block's own state or light and wrong when it does not: a
 * hundred emergency lights would compile a hundred identical lists and fill a hundred cache slots.
 * Geometry that depends only on a handful of appearance fields (a block class, a colour, a mode)
 * belongs here instead, keyed on those fields packed into a {@code long}. A display list does not
 * capture the model-view matrix, so one list is replayed under each block's own transform.</p>
 *
 * <p><b>Light.</b> A list shared between positions must not bake a position's light into its
 * vertices. Either the geometry is fullbright, or the renderer draws it in a vertex format without
 * a lightmap and sets the lightmap coordinates as GL state before {@code glCallList}.</p>
 *
 * <p>The rules in {@code TRAFFIC_SIGNAL_SYSTEM.md} ("Display lists: one texture, no cached state")
 * apply unchanged: bind the texture and set blend, depth mask, cull and colour outside the list,
 * every frame.</p>
 *
 * <p>Every key a renderer can produce is an appearance, not a position, so the number of lists is
 * bounded by the number of distinct looks, not by the size of the world. {@link #clearAll()}
 * releases them on disconnect, with the position caches.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CsmSharedDisplayLists {

  /** Every instance, so {@link #clearAll()} and {@link #describeAll()} reach them all. */
  private static final List<CsmSharedDisplayLists> ALL = new ArrayList<>();

  /** Human-readable name, used only in diagnostics. */
  private final String name;

  /** Appearance key to display list name; {@link CsmDisplayListCache#NO_LIST} when absent. */
  private final Long2IntOpenHashMap lists = new Long2IntOpenHashMap();

  /**
   * Creates a store.
   *
   * @param name human-readable name for diagnostics
   */
  public CsmSharedDisplayLists(String name) {
    this.name = name;
    lists.defaultReturnValue(CsmDisplayListCache.NO_LIST);
    ALL.add(this);
  }

  /**
   * Returns the list compiled for an appearance, or {@link CsmDisplayListCache#NO_LIST} if the
   * caller must compile it with {@link #allocate(long)}.
   *
   * @param key the appearance key
   *
   * @return the display list name, or {@code NO_LIST}
   */
  public int get(long key) {
    return lists.get(key);
  }

  /**
   * Reserves a list name for an appearance. The caller follows it with
   * {@code glNewList(id, GL_COMPILE)} ... {@code glEndList()}, and draws directly instead if this
   * returns {@link CsmDisplayListCache#NO_LIST}.
   *
   * @param key the appearance key
   *
   * @return a display list name to compile into, or {@code NO_LIST} if the driver refused one
   */
  public int allocate(long key) {
    int existing = lists.get(key);
    if (existing != CsmDisplayListCache.NO_LIST) {
      return existing;
    }
    int listId = GL11.glGenLists(1);
    if (listId != CsmDisplayListCache.NO_LIST) {
      lists.put(key, listId);
    }
    return listId;
  }

  /** Releases every list this store holds. */
  public void clear() {
    for (int listId : lists.values()) {
      GL11.glDeleteLists(listId, 1);
    }
    lists.clear();
  }

  /** Releases every list in every store. Called on client disconnect. */
  public static void clearAll() {
    for (CsmSharedDisplayLists store : ALL) {
      store.clear();
    }
  }

  /**
   * Returns one line per store, for {@code /csm displaylists}. Read from the server thread, so it
   * reads only the size, never the map's contents.
   *
   * @return a human-readable line per store
   */
  public static List<String> describeAll() {
    List<String> out = new ArrayList<>();
    for (CsmSharedDisplayLists store : new ArrayList<>(ALL)) {
      out.add(String.format("%-24s %5d shared lists (one per appearance)", store.name,
          store.lists.size()));
    }
    return out;
  }
}

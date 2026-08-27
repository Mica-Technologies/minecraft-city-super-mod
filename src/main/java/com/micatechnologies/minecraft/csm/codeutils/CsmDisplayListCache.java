package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * A bounded, per-position cache of OpenGL display list handles for tile entity special renderers.
 *
 * <p>Several CSM renderers draw geometry that is expensive to tessellate but changes only when the
 * block's state does — signal heads, crosswalk signals, blankout boxes, lane control signals, and
 * the dynamic guide and street signs. Compiling that geometry into a display list once and
 * replaying it collapses the per-frame cost to a single {@code glCallList}.</p>
 *
 * <p><b>Why this class exists rather than a plain map per renderer.</b> Each renderer previously
 * kept its own unbounded {@link java.util.HashMap} keyed by {@link BlockPos}, pruned only when the
 * block was broken. Entries — and the GL list handles they name, which hold driver and GPU memory
 * rather than just heap — survived chunk unload, dimension change, and disconnect. Touring a large
 * city accumulated a handle per block ever rendered, for the whole session. This class bounds that
 * three ways:</p>
 *
 * <ol>
 *   <li><b>Precise release.</b> Renderers expose a cleanup entry point that the owning tile entity
 *       calls from {@code invalidate()} and {@code onChunkUnload()}, so walking away from a block
 *       releases its handle immediately.</li>
 *   <li><b>Capacity bound.</b> The map evicts in least-recently-rendered order once it exceeds
 *       {@link #getMaxEntries()}, deleting the evicted handle. This is the backstop for any
 *       release path that is missed; it is sized well above the number of blocks that can
 *       plausibly be visible at once, so it should never evict something still on screen.</li>
 *   <li><b>Session boundary.</b> {@link #clearAll()} releases every cache on client disconnect.</li>
 * </ol>
 *
 * <p><b>Threading.</b> Every method here issues GL calls and must run on the client render thread.
 * That holds naturally for {@link #get} and {@link #allocate} (called from
 * {@code TileEntitySpecialRenderer.render}) and for the cleanup paths (called from client-side
 * tile entity and block callbacks). {@link #clearAll()} is called from the disconnect handler,
 * which already schedules its work onto the client thread.</p>
 *
 * @author Mica Technologies
 * @see <code>assets/docs/agent_progress/PERFORMANCE_IMPROVEMENT_PLAN.md</code> §14.8.4
 * @since 2026.8
 */
@SideOnly(Side.CLIENT)
public final class CsmDisplayListCache {

  /**
   * Sentinel returned by {@link #get} when the position has no valid cached list. Zero is not a
   * legal display list name in OpenGL ({@code glGenLists} never returns it), so it is unambiguous.
   */
  public static final int NO_LIST = 0;

  /**
   * Default capacity. Chosen to sit comfortably above the number of custom-rendered blocks that
   * can be on screen at once — even a dense downtown intersection cluster at the 128-block render
   * distance these tile entities request is a few hundred — so the bound acts as a leak backstop
   * and not as a working-set limit. Evicting something still visible would cause it to recompile
   * every frame, which is worse than the leak it guards against.
   */
  private static final int DEFAULT_MAX_ENTRIES = 1024;

  /**
   * Every cache created, so {@link #clearAll()} can release all of them on disconnect without
   * each renderer having to register itself with the lifecycle handler.
   */
  private static final List<CsmDisplayListCache> ALL_CACHES = new ArrayList<>();

  /** Human-readable name, used only in diagnostics. */
  private final String name;

  /** Maximum live entries before least-recently-rendered eviction kicks in. */
  private final int maxEntries;

  /**
   * How many display lists this cache currently holds compiled. Maintained on allocation and
   * deletion rather than counted on demand: the /csm displaylists diagnostic reads it from the
   * server thread, and walking the entry map from there is a guaranteed
   * ConcurrentModificationException -- the map is access-ordered, so the render thread's every
   * cache hit structurally modifies it.
   */
  private final AtomicInteger liveLists = new AtomicInteger();

  /**
   * Position to cache entry, in access order so the eldest entry is the least recently rendered.
   */
  private final LinkedHashMap<BlockPos, CachedList> entries;

  /**
   * How many distinct states are kept compiled per position.
   *
   * <p>One is not enough, and the reason is visible to players. A flashing signal alternates
   * between two lit states roughly twice a second, so a single-state cache recompiles its list on
   * every flash -- and because the flash phase comes from a shared wall clock, every flashing
   * signal on screen recompiles on the <em>same</em> frame. That produces a periodic stutter
   * locked to the flash rate, which is precisely what it looks like. Keeping a handful of states
   * resident turns the alternation into two cache hits.</p>
   */
  private static final int STATES_PER_POSITION = 4;

  /**
   * The display lists compiled for one position, keyed by the renderer's state key. Named
   * CachedList rather than Entry so it cannot shadow Map.Entry inside the anonymous LinkedHashMap
   * subclass below, where the two would have the same erasure.
   */
  private static final class CachedList {

    /** The owning cache's live-list counter, decremented as this entry's lists are deleted. */
    private final AtomicInteger liveLists;

    private CachedList(AtomicInteger liveLists) {
      this.liveLists = liveLists;
    }

    /** State key to GL display list name, most recently used last. */
    private final LinkedHashMap<Long, Integer> byState =
        new LinkedHashMap<Long, Integer>(4, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest) {
            if (size() <= STATES_PER_POSITION) {
              return false;
            }
            deleteList(liveLists, eldest.getValue());
            return true;
          }
        };

    private void releaseAll() {
      for (Integer listId : byState.values()) {
        deleteList(liveLists, listId);
      }
      byState.clear();
    }

    private int size() {
      return byState.size();
    }
  }

  /**
   * Creates a cache with the default capacity.
   *
   * @param name human-readable name for diagnostics
   */
  public CsmDisplayListCache(String name) {
    this(name, DEFAULT_MAX_ENTRIES);
  }

  /**
   * Creates a cache with an explicit capacity.
   *
   * @param name       human-readable name for diagnostics
   * @param maxEntries maximum live entries before eviction; must be positive
   */
  public CsmDisplayListCache(String name, int maxEntries) {
    this.name = name;
    this.maxEntries = Math.max(1, maxEntries);
    this.entries = new LinkedHashMap<BlockPos, CachedList>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<BlockPos, CachedList> eldest) {
        if (size() <= CsmDisplayListCache.this.maxEntries) {
          return false;
        }
        // Release the GL handle before dropping the entry -- otherwise the eviction that is
        // supposed to bound this cache would itself leak the thing being bounded.
        eldest.getValue().releaseAll();
        return true;
      }
    };
    ALL_CACHES.add(this);
  }

  /**
   * Returns the cached display list for a position if one exists and was compiled for the given
   * state key, or {@link #NO_LIST} if the caller must (re)compile.
   *
   * <p>A hit also marks the entry as most recently used, which is what keeps the visible set
   * resident and pushes blocks the player has walked away from toward eviction.</p>
   *
   * @param pos      the block position
   * @param stateKey a value that changes whenever the compiled geometry would differ
   *
   * @return the display list name, or {@link #NO_LIST} if a rebuild is needed
   */
  public int get(BlockPos pos, long stateKey) {
    CachedList entry = entries.get(pos);
    if (entry == null) {
      return NO_LIST;
    }
    Integer listId = entry.byState.get(stateKey);
    return listId == null ? NO_LIST : listId;
  }

  /**
   * Reserves a display list name for a position and records the state key it is about to be
   * compiled for. The caller must follow this with {@code glNewList(id, GL_COMPILE)} ...
   * {@code glEndList()}.
   *
   * <p>An existing handle for the position is reused rather than deleted and regenerated:
   * {@code glNewList} on an existing list name replaces its contents, so recycling avoids
   * needless handle churn in the driver.</p>
   *
   * @param pos      the block position
   * @param stateKey the state key the list is being compiled for
   *
   * @return a display list name to compile into, or {@link #NO_LIST} if allocation failed
   */
  public int allocate(BlockPos pos, long stateKey) {
    CachedList entry = entries.get(pos);
    if (entry == null) {
      // Key on an immutable copy: BlockPos.MutableBlockPos is a BlockPos subclass whose hash and
      // equality follow its coordinates, so storing one a caller later mutates would silently
      // corrupt the map.
      entry = new CachedList(liveLists);
      entries.put(pos.toImmutable(), entry);
    }
    Integer existing = entry.byState.get(stateKey);
    if (existing != null) {
      return existing;
    }
    int listId = GL11.glGenLists(1);
    if (listId == NO_LIST) {
      // Driver refused to allocate; the caller falls back to immediate-mode drawing this frame.
      return NO_LIST;
    }
    entry.byState.put(stateKey, listId);
    liveLists.incrementAndGet();
    return listId;
  }

  /**
   * Releases the cached list for a position, if any. Safe to call for a position that was never
   * cached. Called from tile entity {@code invalidate()} / {@code onChunkUnload()} and from block
   * {@code breakBlock}.
   *
   * @param pos the block position
   */
  public void invalidate(BlockPos pos) {
    CachedList entry = entries.remove(pos);
    if (entry != null) {
      entry.releaseAll();
    }
  }

  /**
   * Releases every list held by this cache.
   */
  public void clear() {
    for (Iterator<CachedList> iterator = entries.values().iterator(); iterator.hasNext(); ) {
      iterator.next().releaseAll();
    }
    entries.clear();
  }

  /**
   * Returns the number of live entries. Diagnostics only.
   *
   * @return the live entry count
   */
  public int size() {
    return entries.size();
  }

  /**
   * Returns this cache's capacity.
   *
   * @return the maximum number of live entries
   */
  public int getMaxEntries() {
    return maxEntries;
  }

  /**
   * Returns this cache's diagnostic name.
   *
   * @return the name given at construction
   */
  public String getName() {
    return name;
  }

  /**
   * Returns one line per cache describing how full it is, for diagnostics.
   *
   * <p>Reports compiled lists as well as positions, because they are not the same number and the
   * lists are what cost driver and GPU memory: each position keeps up to
   * {@link #STATES_PER_POSITION} of them so a flashing signal does not recompile on every flash.
   * Nothing here reports GPU bytes, because OpenGL offers no way to ask.</p>
   *
   * @return a human-readable line per cache
   */
  public static List<String> describeAll() {
    List<String> out = new ArrayList<>();
    int totalLists = 0;
    for (CsmDisplayListCache cache : new ArrayList<>(ALL_CACHES)) {
      int lists = cache.compiledListCount();
      totalLists += lists;
      out.add(String.format("%-24s %4d/%d positions, %5d compiled lists",
          cache.name, cache.entries.size(), cache.maxEntries, lists));
    }
    out.add(String.format("%-24s %5d compiled lists across %d caches",
        "TOTAL", totalLists, ALL_CACHES.size()));
    return out;
  }

  /**
   * Returns how many display lists this cache currently holds compiled, across every position and
   * every cached state.
   *
   * @return the compiled list count
   */
  public int compiledListCount() {
    return liveLists.get();
  }

  /**
   * Releases every list held by every cache. Called on client disconnect so display list handles
   * do not survive into the next world the player joins.
   */
  public static void clearAll() {
    for (CsmDisplayListCache cache : ALL_CACHES) {
      cache.clear();
    }
  }

  /**
   * Deletes a display list name, tolerating the sentinel.
   *
   * @param listId the display list name to delete
   */
  private static void deleteList(AtomicInteger liveLists, int listId) {
    if (listId != NO_LIST) {
      GL11.glDeleteLists(listId, 1);
      liveLists.decrementAndGet();
    }
  }
}

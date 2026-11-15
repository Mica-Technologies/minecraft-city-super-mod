package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;

/**
 * Entities a module wants registered. Registration is Core's alone, so every entity keeps the
 * {@code csm:} namespace whichever jar its class ships in: a module calls {@link #register} from
 * its {@code preInit}, and Core's entity registry listener (which runs after every mod's
 * pre-initialization) registers each one beside Core's own {@link EntityCsmSeat}.
 *
 * <p>Each entry names its own network id rather than taking the next free one, so the ids a client
 * and a server agree on do not depend on which modules are installed or in what order they load.
 * Core's seat is id 0; a module picks an id no other entry uses, and a clash fails at start-up.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public final class CsmEntities {

  /** The network id Core's own seat uses. */
  public static final int SEAT_ID = 0;

  /**
   * One entity to register.
   *
   * @since 1.0
   */
  public static final class Entry {

    public final Class<? extends Entity> type;
    public final String name;
    public final int id;
    public final int trackingRange;
    public final int updateFrequency;
    public final boolean sendsVelocityUpdates;

    Entry(Class<? extends Entity> type, String name, int id, int trackingRange,
        int updateFrequency, boolean sendsVelocityUpdates) {
      this.type = type;
      this.name = name;
      this.id = id;
      this.trackingRange = trackingRange;
      this.updateFrequency = updateFrequency;
      this.sendsVelocityUpdates = sendsVelocityUpdates;
    }
  }

  private static final List<Entry> ENTRIES = new ArrayList<>();

  private CsmEntities() {
  }

  /**
   * Asks Core to register an entity as {@code csm:<name>}. Call from a module's {@code preInit}.
   *
   * @param type                 the entity's class
   * @param name                 its registry path, {@code snake_case}
   * @param id                   its network id, unique among CSM's entities and not
   *                             {@link #SEAT_ID}
   * @param trackingRange        how far, in blocks, clients are sent it
   * @param updateFrequency      how often, in ticks, its position is sent
   * @param sendsVelocityUpdates whether its velocity is sent too
   *
   * @throws IllegalStateException if the id or the name is taken
   * @since 1.0
   */
  public static synchronized void register(Class<? extends Entity> type, String name, int id,
      int trackingRange, int updateFrequency, boolean sendsVelocityUpdates) {
    if (id == SEAT_ID) {
      throw new IllegalStateException("CSM entity id " + id + " is Core's seat");
    }
    for (Entry e : ENTRIES) {
      if (e.id == id || e.name.equals(name)) {
        throw new IllegalStateException("CSM entity " + name + " (id " + id + ") clashes with "
            + e.name + " (id " + e.id + ")");
      }
    }
    ENTRIES.add(new Entry(type, name, id, trackingRange, updateFrequency,
        sendsVelocityUpdates));
  }

  /** What the modules registered, in the order they did. */
  public static synchronized List<Entry> entries() {
    return Collections.unmodifiableList(new ArrayList<>(ENTRIES));
  }
}

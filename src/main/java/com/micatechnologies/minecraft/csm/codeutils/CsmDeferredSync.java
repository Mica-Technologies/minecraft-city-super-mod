package com.micatechnologies.minecraft.csm.codeutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Sends a tile entity's client sync at the end of the server tick instead of at once, so it reaches
 * clients after the block changes made in the same tick.
 *
 * <p><b>Why.</b> A block whose renderer caches something read from its neighbours has to tell the
 * client when a neighbour changes, because the client never receives {@code neighborChanged}. The
 * natural place is the server's {@code neighborChanged}, but a sync sent from there goes out
 * immediately while the neighbour's own block change is batched and sent later in the same tick,
 * when the world flushes its changed blocks. The client would then drop its cache, rebuild it
 * against the world as it was before the change, and keep that stale answer. Queuing the sync
 * until {@link TickEvent.ServerTickEvent} ends -- after every world has ticked and flushed -- puts
 * it behind the block change on the same connection.</p>
 *
 * <p>Server thread only. A tile entity queued twice in a tick is synced once, and one that was
 * removed before the flush is skipped.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public final class CsmDeferredSync {

  /** Tile entities to sync at the end of this server tick, each once. */
  private static final Set<AbstractTileEntity> PENDING =
      Collections.newSetFromMap(new IdentityHashMap<>());

  private CsmDeferredSync() {
  }

  /**
   * Queues a tile entity's client sync for the end of the current server tick. Call on the server
   * only; does nothing on the client.
   *
   * @param tileEntity the tile entity to sync
   */
  public static void syncAfterBlockChanges(AbstractTileEntity tileEntity) {
    if (tileEntity.getWorld() != null && !tileEntity.getWorld().isRemote) {
      PENDING.add(tileEntity);
    }
  }

  /** Forge event handler that flushes the queue. Registered on the Forge bus in Csm.preInit. */
  public static final class Flusher {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
        return;
      }
      List<AbstractTileEntity> due = new ArrayList<>(PENDING);
      PENDING.clear();
      for (AbstractTileEntity tileEntity : due) {
        if (!tileEntity.isInvalid() && tileEntity.getWorld() != null) {
          tileEntity.syncServerToClient(tileEntity.getWorld());
        }
      }
    }
  }
}

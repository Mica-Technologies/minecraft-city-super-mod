package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import com.micatechnologies.minecraft.csm.transit.stop.BusDepartures;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Reads bus departures out when a departure board that announces them is near the player: "Route
 * 12 to Downtown is now departing from bay 3", once for each bus as it becomes due.
 *
 * <p><b>Only with Text to Speech.</b> Speech goes through Core's {@link CsmTts}, which speaks
 * through the engine the Text to Speech module registers; the module is never named. Nothing is
 * said unless an engine is registered and loaded ({@link CsmTts#isReady()}), so without the
 * module a board is silent: Core's narrator fallback, right for a block a player set up to talk,
 * would be wrong for a board that talks by itself. Until then this costs one check a second.</p>
 *
 * <p><b>Which board speaks.</b> Once a second, the first board of each announcing bank (its
 * page 0) within {@link #RANGE} blocks of the player looks at its list; every due bus not yet
 * announced is queued. A bus is known by its stop, route and minute, so two boards over the same
 * stops do not both call it. The queue is read out one message every {@link #SPACING_TICKS}
 * ticks, because the engine drops a message that arrives while it is still speaking, and a
 * message that waits longer than {@link #STALE_TICKS} is dropped rather than read late.</p>
 *
 * <p>Client side, and entirely local: every player hears what their own client works out, and
 * the schedule is the same for everyone, so they hear the same calls.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class BusBoardAnnouncer {

  /** How near a board must be to be heard. */
  private static final double RANGE = 24.0;
  private static final long CHECK_TICKS = 20;
  private static final long SPACING_TICKS = 120;
  private static final long STALE_TICKS = 600;
  /** How many announced buses are remembered, to call each once. */
  private static final int REMEMBERED = 256;
  private static final int QUEUE = 8;

  private final LinkedHashSet<Long> announced = new LinkedHashSet<>();
  private final ArrayDeque<String> queue = new ArrayDeque<>();
  private final ArrayDeque<Long> queuedAt = new ArrayDeque<>();
  private long lastSpoken = Long.MIN_VALUE;
  private long ticks;

  /** Forgets what was announced and queued: on disconnect. */
  public void clear() {
    announced.clear();
    queue.clear();
    queuedAt.clear();
    lastSpoken = Long.MIN_VALUE;
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
      return;
    }
    ticks++;
    if (ticks % CHECK_TICKS != 0 || !CsmTts.isReady()) {
      return;
    }
    Minecraft mc = Minecraft.getMinecraft();
    World world = mc.world;
    EntityPlayerSP player = mc.player;
    if (world == null || player == null || mc.isGamePaused()) {
      return;
    }
    long now = world.getTotalWorldTime();
    for (TileEntity te : world.loadedTileEntityList) {
      if (te instanceof TileEntityBusDepartureBoard) {
        listen((TileEntityBusDepartureBoard) te, player, now);
      }
    }
    speak(now);
  }

  private void listen(TileEntityBusDepartureBoard board, EntityPlayerSP player, long now) {
    if (!board.isAnnouncing() || board.isInvalid()
        || player.getDistanceSqToCenter(board.getPos()) > RANGE * RANGE) {
      return;
    }
    board.refreshView();
    if (board.getPage() != 0) {
      return;
    }
    long minute = BusDepartures.minuteOf(now);
    BusStation station = BusStation.at(board.getWorld(), board.getPos());
    BusStation.Departures list = station.departures(board.getFilter(), minute);
    for (int i = 0; i < list.count() && list.minutes(i) == 0; i++) {
      int bay = list.bay(i);
      int route = list.route(i);
      long key = (station.bayPos(bay - 1).toLong() * 31 + route) * 31 + minute;
      if (!announced.add(key)) {
        continue;
      }
      if (announced.size() > REMEMBERED) {
        Iterator<Long> oldest = announced.iterator();
        oldest.next();
        oldest.remove();
      }
      if (queue.size() < QUEUE) {
        queue.add("Route " + route + " to "
            + BusDepartures.DESTINATIONS_SPOKEN[BusDepartures.destinationOf(route)]
            + " is now departing from bay " + bay + ".");
        queuedAt.add(now);
      }
    }
  }

  private void speak(long now) {
    while (!queuedAt.isEmpty() && now - queuedAt.peek() > STALE_TICKS) {
      queue.poll();
      queuedAt.poll();
    }
    boolean spokeLately = lastSpoken != Long.MIN_VALUE && now >= lastSpoken
        && now - lastSpoken < SPACING_TICKS;
    if (queue.isEmpty() || spokeLately) {
      return;
    }
    String message = queue.poll();
    queuedAt.poll();
    lastSpoken = now;
    CsmTts.say(message, "");
  }
}

package com.micatechnologies.minecraft.csm.transit.stop;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * The route numbers on a bus stop flag's three route plates: 1 to {@link #MAX_ROUTE}, or 0 for a
 * plate that is not there. The flag's {@code getActualState} reads which plates are there (the
 * plates are baked), and {@link TileEntityBusStopFlagRenderer} draws the numbers, so a new number
 * rebuilds the chunk section only when a plate appears or goes ({@link #getBakedModelKey}).
 *
 * <p>It also keeps which side of the post the flag hangs off: to the reader's right (the default,
 * and what a flag saved before the choice existed loads as, since it has no key) or left.</p>
 *
 * @since 2026.9
 */
public class TileEntityBusStopFlag extends AbstractTileEntityBusStopSign implements IBusStopSource {

  /** How many route plates a flag has. */
  public static final int PLATES = 3;

  /** The highest route number. */
  public static final int MAX_ROUTE = 99;

  private static final String KEY_ROUTES = "r";

  /** Set, and true, while the flag hangs to the reader's left of the post. */
  private static final String KEY_LEFT = "l";

  private final int[] routes = {1, 0, 0};

  private boolean left;

  /**
   * Whether the flag hangs to the reader's left of the post: the post at the sign's right edge.
   *
   * @return true for left, false for right (the default)
   */
  public boolean isLeft() {
    return left;
  }

  /**
   * Moves the flag to the other side of the post. Server side; tells the players in range.
   *
   * @return true if the flag now hangs to the left
   */
  public boolean flipSide() {
    left = !left;
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
    return left;
  }

  /**
   * The route number on a plate.
   *
   * @param plate 0 (the top plate) to {@link #PLATES} - 1
   *
   * @return 1 to {@link #MAX_ROUTE}, or 0 for no plate
   */
  public int getRoute(int plate) {
    return routes[plate];
  }

  /**
   * Steps a plate's number one up, or one down: 0 (no plate), 1 ... {@link #MAX_ROUTE} and round
   * again. Server side; tells the players in range.
   *
   * @param plate 0 to {@link #PLATES} - 1
   * @param back  true to step down
   *
   * @return the new number
   */
  public int step(int plate, boolean back) {
    int span = MAX_ROUTE + 1;
    routes[plate] = (routes[plate] + (back ? span - 1 : 1)) % span;
    if (world != null && !world.isRemote) {
      markDirty();
      syncServerToClient(world);
    }
    return routes[plate];
  }

  private static int clamp(int n) {
    return Math.max(0, Math.min(MAX_ROUTE, n));
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(KEY_ROUTES)) {
      byte[] stored = compound.getByteArray(KEY_ROUTES);
      for (int i = 0; i < PLATES; i++) {
        routes[i] = i < stored.length ? clamp(stored[i]) : 0;
      }
    }
    left = compound.getBoolean(KEY_LEFT);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    byte[] stored = new byte[PLATES];
    for (int i = 0; i < PLATES; i++) {
      stored[i] = (byte) routes[i];
    }
    compound.setByteArray(KEY_ROUTES, stored);
    if (left) {
      compound.setBoolean(KEY_LEFT, true);
    } else {
      compound.removeTag(KEY_LEFT);
    }
    return compound;
  }

  // --- a stop source: its plates' numbers are their route keys ---------------------------

  @Override
  public BlockPos getStopPos() {
    return getPos();
  }

  @Override
  public int getStopRouteCount() {
    int n = 0;
    for (int i = 0; i < PLATES; i++) {
      if (routes[i] != 0) {
        n++;
      }
    }
    return n;
  }

  @Override
  public int getStopRoute(int i) {
    int n = 0;
    for (int p = 0; p < PLATES; p++) {
      if (routes[p] != 0 && n++ == i) {
        return routes[p];
      }
    }
    return 0;
  }

  @Override
  @Nullable
  public BusAgency getStopAgency() {
    Block block = world == null ? null : world.getBlockState(pos).getBlock();
    return block instanceof BlockBusStopFlag ? ((BlockBusStopFlag) block).getAgency() : null;
  }

  @Override
  public int getStopColour() {
    BusAgency agency = getStopAgency();
    return agency == null ? 0xFFFFFF : agency.getTextColour();
  }

  /** Only which plates are there, and the side, reach the baked model. */
  @Override
  protected long getBakedModelKey() {
    long mask = left ? 1L << PLATES : 0;
    for (int i = 0; i < PLATES; i++) {
      if (routes[i] != 0) {
        mask |= 1L << i;
      }
    }
    return mask;
  }
}

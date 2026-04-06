package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import net.minecraft.util.ITickable;

/**
 * Abstract tile entity implementation with tick handler and customizable tick rate. This class is
 * based on the {@link AbstractTileEntity} class and implements the {@link ITickable} interface.
 *
 * @author Mica Technologies
 * @since 2023.2.0
 */
public abstract class AbstractTickableTileEntity extends AbstractTileEntity implements ITickable {

  /**
   * Cached tick rate value, lazily initialized from {@link #getTickRate()} on first use.
   * A value of {@code -1} indicates the cache is not yet populated.
   * Subclasses that dynamically change their tick rate should call
   * {@link #invalidateTickRateCache()} to force a refresh.
   */
  private transient long cachedTickRate = -1;

  /**
   * Returns the tick rate, using a cached value when available. The cache is populated lazily
   * on first access and can be invalidated via {@link #invalidateTickRateCache()}.
   *
   * @return the cached tick rate
   */
  private long getCachedTickRate() {
    if (cachedTickRate == -1) {
      cachedTickRate = getTickRate();
    }
    return cachedTickRate;
  }

  /**
   * Invalidates the cached tick rate so that the next tick will re-read the value from
   * {@link #getTickRate()}. Subclasses should call this method whenever they modify the
   * value that {@link #getTickRate()} returns.
   */
  protected void invalidateTickRateCache() {
    cachedTickRate = -1;
  }

  /**
   * Handler for when the tile entity ticks. This method is called every tick, and a comparison is
   * made to see if the tile entity's tick rate has been reached. If so, the onTick() method is
   * called. The tick rate is determined by the value returned by the {@link #getTickRate()}
   * method.
   */
  @Override
  public void update() {
    if (getWorld() == null) {
      return;
    }
    if (doClientTick() || !getWorld().isRemote) {
      if (!pauseTicking() && getWorld().getTotalWorldTime() % getCachedTickRate() == 0L) {
        try {
          onTick();
        } catch (Exception e) {
          Csm.getLogger().error("Error ticking tile entity [remote: {}, class: {}, pos: {}]",
              world.isRemote, this.getClass().getCanonicalName(), getPos(), e);
        }
      }
    }
  }

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * should also tick on the client side. By default, the tile entity will always tick on the server
   * side, and in the event of singleplayer/local mode, the host client is considered the server.
   *
   * @return a boolean indicating if the tile entity should also tick on the client side
   */
  public abstract boolean doClientTick();

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * ticking should be paused. If the tile entity is paused, the tick event will not be called.
   *
   * @return a boolean indicating if the tile entity ticking should be paused
   */
  public abstract boolean pauseTicking();

  /**
   * Abstract method which must be implemented to return the tick rate of the tile entity.
   *
   * @return the tick rate of the tile entity
   */
  public abstract long getTickRate();

  /**
   * Abstract method which must be implemented to handle the tick event of the tile entity.
   */
  public abstract void onTick();
}

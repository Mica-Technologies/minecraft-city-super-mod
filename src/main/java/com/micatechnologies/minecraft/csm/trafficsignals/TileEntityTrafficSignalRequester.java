package com.micatechnologies.minecraft.csm.trafficsignals;

/**
 * Tile entity utility class for traffic signal requester blocks such as a crosswalk button or sensor. This class
 * assists in tracking and managing the count and state of requests.
 *
 * @author Mica Technologies
 * @version 2.0
 * @since 2022.1
 */
public class TileEntityTrafficSignalRequester extends TileEntityTrafficSignalTickableRequester
{
    /**
     * Returns the tick rate of the tile entity. This implementation returns {@link Long#MAX_VALUE} as the tile entity
     * does not tick.
     *
     * @return the tick rate of the tile entity. This implementation returns {@link Long#MAX_VALUE} as the tile entity
     *         does not tick.
     *
     * @since 2.0
     */
    @Override
    public long getTickRate() {
        return Long.MAX_VALUE;
    }

    /**
     * Handles the tick event of the tile entity. This implementation does nothing.
     *
     * @since 2.0
     */
    @Override
    public void onTick() {
        // Do nothing
    }

    /**
     * Returns a boolean indicating if the tile entity should also tick on the client side. By default, the tile entity
     * will always tick on the server side, and in the event of singleplayer/local mode, the host client is considered
     * the server. This implementation always returns false, as the tile entity does not tick.
     *
     * @return a boolean indicating if the tile entity should also tick on the client side. This implementation always
     *         returns false, as the tile entity does not tick.
     *
     * @since 2.0
     */
    @Override
    public boolean doClientTick() {
        return false;
    }

    /**
     * Returns a boolean indicating if the tile entity ticking should be paused. If the tile entity is paused, the tick
     * event will not be called. This implementation always returns true as the tile entity does not tick.
     *
     * @return a boolean indicating if the tile entity ticking should be paused. This implementation always returns true
     *         as the tile entity does not tick.
     *
     * @since 2.0
     */
    @Override
    public boolean pauseTicking() {
        return true;
    }
}

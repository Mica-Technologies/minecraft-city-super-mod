package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.util.ITickable;

/**
 * Abstract tile entity implementation with tick handler and customizable tick rate. This class is based on the
 * {@link AbstractTileEntity} class and implements the {@link ITickable} interface.
 *
 * @author Mica Technologies
 * @since 2022.1.0
 */
public abstract class AbstractTickableTileEntity extends AbstractTileEntity implements ITickable
{

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

    /**
     * Abstract method which must be implemented to return a boolean indicating if the tile entity should also tick on
     * the client side. By default, the tile entity will always tick on the server side, and in the event of
     * singleplayer/local mode, the host client is considered the server.
     *
     * @return a boolean indicating if the tile entity should also tick on the client side
     */
    public abstract boolean doClientTick();

    /**
     * Abstract method which must be implemented to return a boolean indicating if the tile entity ticking should be
     * paused. If the tile entity is paused, the tick event will not be called.
     *
     * @return a boolean indicating if the tile entity ticking should be paused
     */
    public abstract boolean pauseTicking();

    /**
     * Handler for when the tile entity ticks. This method is called every tick, and a comparison is made to see if the
     * tile entity's tick rate has been reached. If so, the onTick() method is called. The tick rate is determined by
     * the value returned by the {@link #getTickRate()} method.
     */
    @Override
    public void update() {
        if ( !pauseTicking() && getWorld().getTotalWorldTime() % getTickRate() == 0L  ) {
            if ( doClientTick() || !getWorld().isRemote ) {
                try {
                    onTick();
                }
                catch ( Exception e ) {
                    System.err.println( "An error occurred while ticking [remote: " +
                                                world.isRemote +
                                                "] a tile entity [" +
                                                this.getClass().getCanonicalName() +
                                                "]: " );
                    e.printStackTrace( System.err );
                }
            }
        }
    }
}

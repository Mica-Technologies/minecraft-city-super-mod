package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Tile entity utility class for traffic signal requester blocks such as a crosswalk button or sensor. This class
 * assists in tracking and managing the count and state of requests.
 *
 * @author Mica Technologies
 * @version 2.0
 * @since 2022.1
 */
@ElementsCitySuperMod.ModElement.Tag
public abstract class TileEntityTrafficSignalTickableRequester extends AbstractTickableTileEntity
{
    /**
     * The key used to store the request count in NBT data.
     *
     * @since 1.0
     */
    private static final String REQUEST_COUNT_KEY = "requestCount";

    /**
     * The current count of requests.
     *
     * @since 1.0
     */
    private int requestCount = 0;

    /**
     * Returns the NBT tag compound with the tile entity's NBT data.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     *
     * @since 2.0
     */
    @Override
    public NBTTagCompound writeNBT( NBTTagCompound compound ) {
        // Set the request count
        compound.setInteger( REQUEST_COUNT_KEY, requestCount );

        // Return the compound
        return compound;
    }

    /**
     * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     *
     * @since 2.0
     */
    @Override
    public void readNBT( NBTTagCompound compound ) {
        // Get the request count
        if ( compound.hasKey( REQUEST_COUNT_KEY ) ) {
            requestCount = compound.getInteger( REQUEST_COUNT_KEY );
        }
    }

    /**
     * Resets the request count to zero.
     *
     * @since 1.0
     */
    public void resetRequestCount() {
        requestCount = 0;
    }

    /**
     * Gets the current request count.
     *
     * @return the current request count
     *
     * @since 1.0
     */
    public int getRequestCount() {
        return requestCount;
    }

    /**
     * Increments the request count by one.
     *
     * @since 1.0
     */
    public void incrementRequestCount() {
        requestCount++;
    }
}
